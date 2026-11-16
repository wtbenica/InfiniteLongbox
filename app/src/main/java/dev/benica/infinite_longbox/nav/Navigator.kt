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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import dev.benica.infinite_longbox.filter.FilterSnapshot
import dev.benica.infinite_longbox.filter.SearchFilter
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.database.models.FullCreator
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.viewmodels.CharacterListViewModel
import dev.benica.infinite_longbox.viewmodels.CreatorListViewModel
import dev.benica.infinite_longbox.viewmodels.IssueListViewModel
import dev.benica.infinite_longbox.viewmodels.ListViewModel
import dev.benica.infinite_longbox.viewmodels.SeriesListViewModel
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

/** The places the tab bar goes to; each keeps its own stack of screens. */
enum class Tab { BROWSE, RELEASES, COLLECTIONS }

/** A screen the app can be on. A list's filter lives in its [Entry]'s view model, not in the route. */
sealed interface Route {
    class List(val kind: ListKind) : Route

    /** [collectionId]: the collection the issue was opened from, so the jump bar stays in it. */
    class Issue(val issueId: Int, val variantOf: Int? = null, val collectionId: Int? = null) : Route
    class Character(val characterId: Int) : Route
    class Creator(val creatorId: Int) : Route

    /** The month-by-month timeline of releases: the root of its tab. */
    data object Releases : Route

    /** My Collection, the wish list and the lists to come: the root of its tab. */
    data object Collections : Route

    /** The barcode scanner, over the whole screen. */
    data object Scan : Route
}

/** One screen on the stack; its view models live until it is left for good. */
@OptIn(ExperimentalCoroutinesApi::class)
class Entry(val key: Int, val route: Route) : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    /** A page that covers the lists: an issue's, a character's or a creator's. */
    val isDetail: Boolean
        get() = route !is Route.List

    /** The first screen of a tab that isn't a list. */
    val isTabRoot: Boolean
        get() = route is Route.Releases || route is Route.Collections

    /** Draws over the whole screen, with no top bar or sheet. */
    val isFullScreen: Boolean
        get() = route is Route.Scan

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

/** The screens as a stack: the top is on show, Back goes to the one below; saved for a killed app. */
@OptIn(ExperimentalCoroutinesApi::class)
class Navigator(
    private val later: (delayMs: Long, work: () -> Unit) -> Unit = { delayMs, work ->
        Handler(Looper.getMainLooper()).postDelayed(work, delayMs)
    },
) : ViewModel() {
    val stack = mutableStateListOf<Entry>()

    /** The tab on show: [stack] is its screens. */
    var tab by mutableStateOf(Tab.BROWSE)
        private set

    /** The screens of the tabs not on show. */
    private val parked = mutableMapOf<Tab, List<Entry>>()

    val top: Entry?
        get() = stack.lastOrNull()

    private var nextKey = 1

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

    /** Shows [to], with the screens it was left on; its first screen if it has none (a Browse with none is left for the caller). */
    fun switchTab(to: Tab) {
        if (to == tab) return
        parked[tab] = stack.toList()
        stack.clear()
        tab = to
        val kept = parked.remove(to)
        if (kept != null) stack.addAll(kept)
        else when (to) {
            Tab.RELEASES -> push(Route.Releases)
            Tab.COLLECTIONS -> push(Route.Collections)
            Tab.BROWSE -> Unit
        }
    }

    /** Back to the first screen of the tab on show. */
    fun popToRoot() {
        while (pop()) continue
    }

    /** Goes back one screen; false if there is none to go back to. */
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        retire(stack.removeAt(stack.lastIndex))
        return true
    }

    fun clear() {
        while (stack.isNotEmpty()) retire(stack.removeAt(stack.lastIndex))
        parked.values.forEach { entries -> entries.forEach(::retire) }
        parked.clear()
        tab = Tab.BROWSE
    }

    /** A screen that has left may still be fading out: its view models go a moment later. */
    private fun retire(entry: Entry) {
        later(RETIRE_DELAY_MS) { entry.viewModelStore.clear() }
    }

    override fun onCleared() {
        stack.forEach { it.viewModelStore.clear() }
        parked.values.forEach { entries -> entries.forEach { it.viewModelStore.clear() } }
    }

    // THE STACK THROUGH A KILLED APP

    fun save(): Bundle = Bundle().apply {
        putString(TAB, tab.name)
        Tab.entries.forEach { t ->
            val entries = if (t == tab) stack.toList() else parked[t].orEmpty()
            val kinds = ArrayList<String>()
            val args = ArrayList<String>()
            entries.forEach { entry ->
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
                    Route.Releases -> {
                        kinds += "releases"
                        args += ""
                    }
                    Route.Collections -> {
                        kinds += "collections"
                        args += ""
                    }
                    // A scan session isn't kept through a killed app
                    Route.Scan -> Unit
                }
            }
            putStringArrayList(KINDS + t.name, kinds)
            putStringArrayList(ARGS + t.name, args)
        }
    }

    /** Rebuilds the stacks from [save]; [onListFailed] hears of a list whose filter can't be rebuilt. */
    fun restore(saved: Bundle, onListFailed: () -> Unit) {
        clear()
        tab = saved.getString(TAB)?.let { name -> Tab.entries.firstOrNull { it.name == name } } ?: Tab.BROWSE
        Tab.entries.forEach { t ->
            val kinds = saved.getStringArrayList(KINDS + t.name) ?: return@forEach
            val args = saved.getStringArrayList(ARGS + t.name) ?: return@forEach
            val entries = kinds.zip(args).mapNotNull { (kind, arg) -> restoreEntry(kind, arg, onListFailed) }
            if (t == tab) stack.addAll(entries) else parked[t] = entries
        }
    }

    private fun restoreEntry(kind: String, arg: String, onListFailed: () -> Unit): Entry? = when (kind) {
        "list" -> {
            val (name, snapshot) = arg.split("|", limit = 2)
            val entry = Entry(nextKey++, Route.List(ListKind.valueOf(name)))
            if (snapshot.isEmpty()) onListFailed()
            else entry.listViewModel()?.restoreFilter(snapshot, onListFailed)
            entry
        }
        "issue" -> {
            val (id, variantOf, collection) = arg.split("|")
            Entry(nextKey++, Route.Issue(id.toInt(), variantOf.toIntOrNull(), collection.toIntOrNull()))
        }
        "character" -> Entry(nextKey++, Route.Character(arg.toInt()))
        "creator" -> Entry(nextKey++, Route.Creator(arg.toInt()))
        "releases" -> Entry(nextKey++, Route.Releases)
        "collections" -> Entry(nextKey++, Route.Collections)
        else -> null
    }

    private companion object {
        const val TAB = "nav_tab"
        const val KINDS = "nav_kinds_"
        const val ARGS = "nav_args_"
        const val RETIRE_DELAY_MS = 700L
    }
}
