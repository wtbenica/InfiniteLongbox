/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.benica.infinite_longbox.nav

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import dev.benica.infinite_longbox.FilterSnapshot
import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.database.models.FullCreator
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.fragments_view_models.CharacterListViewModel
import dev.benica.infinite_longbox.fragments_view_models.CreatorListViewModel
import dev.benica.infinite_longbox.fragments_view_models.IssueListViewModel
import dev.benica.infinite_longbox.fragments_view_models.ListViewModel
import dev.benica.infinite_longbox.fragments_view_models.SeriesListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.reflect.KClass

/** What a list lists. */
enum class ListKind {
    SERIES, ISSUES, CHARACTERS, CREATORS;

    companion object {
        fun of(view: KClass<*>): ListKind = when (view) {
            FullIssue::class -> ISSUES
            Character::class -> CHARACTERS
            FullSeries::class -> SERIES
            FullCreator::class -> CREATORS
            else -> throw IllegalStateException("illegal view option: ${view.simpleName}")
        }
    }
}

/** A screen the app can be on. A list's filter lives in its [Entry]'s view model, not in the route. */
sealed interface Route {
    class List(val kind: ListKind) : Route

    /** [collectionId]: the collection the issue was opened from, so the jump bar stays in it. */
    class Issue(val issueId: Int, val variantOf: Int? = null, val collectionId: Int? = null) : Route
    class Character(val characterId: Int) : Route
    class Creator(val creatorId: Int) : Route
}

/**
 * One screen on the stack. It owns the view models of that screen, which live until the screen is
 * left for good (not just while it is on top).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Entry(val key: Int, val route: Route) : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    /** A page that covers the lists: an issue's, a character's or a creator's. */
    val isDetail: Boolean
        get() = route !is Route.List

    inline fun <reified VM : ViewModel> viewModel(): VM =
        ViewModelProvider(viewModelStore, ViewModelProvider.NewInstanceFactory())[VM::class.java]

    /** The view model of a list screen (which holds its filter). */
    fun listViewModel(): ListViewModel<*>? = (route as? Route.List)?.let {
        when (it.kind) {
            ListKind.SERIES -> viewModel<SeriesListViewModel>()
            ListKind.ISSUES -> viewModel<IssueListViewModel>()
            ListKind.CHARACTERS -> viewModel<CharacterListViewModel>()
            ListKind.CREATORS -> viewModel<CreatorListViewModel>()
        }
    }

    /** The filter a list screen is showing, once it has one. */
    val filter: SearchFilter?
        get() = listViewModel()?.let { vm -> vm.filter.value?.takeIf { vm.hasFilter } }
}

/**
 * The screens, as a stack: the one on top is on show, Back goes to the one below. The activity
 * decides what a change of filter means (see [dev.benica.infinite_longbox.MainActivity]); this
 * only keeps the stack, and saves it for when the app is killed and comes back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Navigator : ViewModel() {
    val stack = mutableStateListOf<Entry>()

    val top: Entry?
        get() = stack.lastOrNull()

    private var nextKey = 1
    private val handler = Handler(Looper.getMainLooper())

    fun push(route: Route): Entry = Entry(nextKey++, route).also { stack.add(it) }

    /** A list for [filter] on top of the stack. */
    fun pushList(filter: SearchFilter): Entry {
        val entry = push(Route.List(ListKind.of(filter.mViewOption)))
        entry.listViewModel()?.setFilter(filter)
        return entry
    }

    /** Takes the place of the top screen (the list seen another way). */
    fun replaceTopWithList(filter: SearchFilter): Entry {
        top?.let { retire(stack.removeAt(stack.lastIndex)) }
        return pushList(filter)
    }

    /** Goes back one screen; false if there is none to go back to. */
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        retire(stack.removeAt(stack.lastIndex))
        return true
    }

    fun clear() {
        while (stack.isNotEmpty()) retire(stack.removeAt(stack.lastIndex))
    }

    /** A screen that has left may still be fading out: its view models go a moment later. */
    private fun retire(entry: Entry) {
        handler.postDelayed({ entry.viewModelStore.clear() }, RETIRE_DELAY_MS)
    }

    override fun onCleared() {
        stack.forEach { it.viewModelStore.clear() }
    }

    // THE STACK THROUGH A KILLED APP

    fun save(): Bundle = Bundle().apply {
        val kinds = ArrayList<String>()
        val args = ArrayList<String>()
        stack.forEach { entry ->
            when (val route = entry.route) {
                is Route.List -> {
                    kinds += "list"
                    args += "${route.kind.name}|${entry.filter?.let { FilterSnapshot.save(it) }.orEmpty()}"
                }
                is Route.Issue -> {
                    kinds += "issue"
                    args += "${route.issueId}|${route.variantOf ?: ""}|${route.collectionId ?: ""}"
                }
                is Route.Character -> {
                    kinds += "character"
                    args += route.characterId.toString()
                }
                is Route.Creator -> {
                    kinds += "creator"
                    args += route.creatorId.toString()
                }
            }
        }
        putStringArrayList(KINDS, kinds)
        putStringArrayList(ARGS, args)
    }

    /**
     * Rebuilds the stack from [save]. Lists get their filters back asynchronously (their records
     * are looked up again); [onListFailed] is told of one that can't be rebuilt.
     */
    fun restore(saved: Bundle, onListFailed: () -> Unit) {
        val kinds = saved.getStringArrayList(KINDS) ?: return
        val args = saved.getStringArrayList(ARGS) ?: return
        clear()
        kinds.zip(args).forEach { (kind, arg) ->
            when (kind) {
                "list" -> {
                    val (name, snapshot) = arg.split("|", limit = 2)
                    val entry = push(Route.List(ListKind.valueOf(name)))
                    if (snapshot.isEmpty()) onListFailed()
                    else entry.listViewModel()?.restoreFilter(snapshot, onListFailed)
                }
                "issue" -> {
                    val (id, variantOf, collection) = arg.split("|")
                    push(Route.Issue(id.toInt(), variantOf.toIntOrNull(), collection.toIntOrNull()))
                }
                "character" -> push(Route.Character(arg.toInt()))
                "creator" -> push(Route.Creator(arg.toInt()))
            }
        }
    }

    private companion object {
        const val KINDS = "nav_kinds"
        const val ARGS = "nav_args"
        const val RETIRE_DELAY_MS = 700L
    }
}
