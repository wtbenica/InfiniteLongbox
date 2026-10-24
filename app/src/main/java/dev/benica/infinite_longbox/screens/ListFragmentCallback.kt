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
import dev.benica.infinite_longbox.ui.toolbar.SelectionBar

/** What a screen asks of the activity that holds it. */
interface ListFragmentCallback {
    /** [subtitle] is the smaller line under the title; every call sets both, so it clears. */
    fun setTitle(title: String? = null, subtitle: String? = null)

    /** Turns the top bar into the picked issues' bar; null puts the title back. */
    fun setSelection(selection: SelectionBar?)

    fun updateFilter(filter: SearchFilter)
}
