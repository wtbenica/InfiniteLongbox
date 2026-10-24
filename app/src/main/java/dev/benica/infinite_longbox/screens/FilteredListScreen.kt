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

package dev.benica.infinite_longbox.screens

import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.database.models.ListItem
import dev.benica.infinite_longbox.fragments_view_models.ListViewModel
import dev.benica.infinite_longbox.nav.Screen
import dev.benica.infinite_longbox.nav.ScreenHost
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * What every list screen shares, whatever it draws its rows with: the filter it was made for (kept
 * with the screen, for when it is come back to) and how many columns fit.
 */
@ExperimentalCoroutinesApi
abstract class FilteredListScreen<T : ListItem>(host: ScreenHost) : Screen(host) {
    protected abstract val viewModel: ListViewModel<T>

    protected val callback: ListFragmentCallback get() = host.activity

    abstract val minColSizeDp: Int

    protected val numCols: Int by lazy { host.activity.screenSizeInDp.x / minColSizeDp + 1 }

    /** The filter this list is showing: it is kept for when the list is returned to. */
    val currentFilter: SearchFilter?
        get() = viewModel.filter.value?.takeIf { viewModel.hasFilter }

    /** Edits made while this list is on screen (the filter sheet); the activity passes them on. */
    fun applyFilter(filter: SearchFilter) {
        if (filter != currentFilter) viewModel.setFilter(filter)
    }
}
