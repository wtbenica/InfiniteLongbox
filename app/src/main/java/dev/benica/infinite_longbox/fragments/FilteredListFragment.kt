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

package dev.benica.infinite_longbox.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import dev.benica.infinite_longbox.FilterSnapshot
import dev.benica.infinite_longbox.MainActivity
import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.database.models.ListItem
import dev.benica.infinite_longbox.fragments_view_models.FilterViewModel
import dev.benica.infinite_longbox.fragments_view_models.ListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * What every list screen shares, whatever it draws its rows with: the filter it was made for (kept
 * for when it is come back to, and rebuilt if the app was killed), the activity it reports to, and
 * how many columns fit.
 */
@ExperimentalCoroutinesApi
abstract class FilteredListFragment<T : ListItem> : Fragment() {
    protected abstract val viewModel: ListViewModel<T>
    protected val filterViewModel: FilterViewModel by viewModels({ requireActivity() })

    protected var callback: ListFragmentCallback? = null

    /** True from when a killed app's list asks for its filter to be rebuilt until it arrives. */
    protected var restoringFilter = false

    protected var numCols: Int = 1
    abstract val minColSizeDp: Int

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as ListFragmentCallback
    }

    /** The filter this list is showing: it is kept for when the list is returned to. */
    val currentFilter: SearchFilter?
        get() = viewModel.filter.value?.takeIf { viewModel.hasFilter }

    /** Edits made while this list is on screen (the filter sheet); MainActivity passes them on. */
    fun applyFilter(filter: SearchFilter) {
        if (filter != currentFilter) viewModel.setFilter(filter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A list is created for one filter and keeps it, so coming back to it from further down
        // the back stack finds it as it was
        if (!viewModel.hasFilter) {
            val registered = arguments?.getInt(ARG_SCREEN)
                ?.let { filterViewModel.takeScreenFilter(it) }
            val saved = savedInstanceState?.getString(KEY_FILTER)

            when {
                registered != null -> viewModel.setFilter(registered)
                // The app was killed and this screen came back: rebuild its filter from the ids
                // that were saved with it
                saved != null -> {
                    restoringFilter = true
                    viewModel.restoreFilter(saved) { callback?.onRestoreFailed() }
                }
            }
        }

        numCols = (context as MainActivity).screenSizeInDp.x / minColSizeDp + 1
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentFilter?.let { outState.putString(KEY_FILTER, FilterSnapshot.save(it)) }
    }

    override fun onResume() {
        super.onResume()
        callback?.setToolbarScrollFlags(SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS)
    }

    /** Tells the activity once a rebuilt filter arrives, so the filter sheet can show it. */
    protected fun announceRestoredFilter() {
        if (!restoringFilter) return
        viewModel.filter.observe(viewLifecycleOwner) { filter ->
            if (restoringFilter && filter != null) {
                restoringFilter = false
                callback?.onFilterRestored(this, filter)
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    companion object {
        private const val ARG_SCREEN = "screen"
        private const val KEY_FILTER = "filter"

        /** Makes [this] list start with the filter registered for [screenId]. */
        fun <F : Fragment> F.forScreen(screenId: Int): F = apply {
            arguments = Bundle().apply { putInt(ARG_SCREEN, screenId) }
        }
    }
}
