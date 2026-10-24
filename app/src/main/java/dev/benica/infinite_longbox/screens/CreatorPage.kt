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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.nav.Route
import dev.benica.infinite_longbox.nav.Screen
import dev.benica.infinite_longbox.nav.ScreenHost
import dev.benica.infinite_longbox.database.models.Creator
import dev.benica.infinite_longbox.viewmodels.CreatorDetailViewModel
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.ui.creator.CreatorDetailActions
import dev.benica.infinite_longbox.ui.creator.CreatorDetailScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi


/** A creator's page: who they are, other names, credits and the series they worked on most. */
@ExperimentalCoroutinesApi
class CreatorPage(host: ScreenHost) : Screen(host) {
    private val viewModel: CreatorDetailViewModel by lazy { host.entry.viewModel<CreatorDetailViewModel>() }

    private val creatorId: Int = (host.entry.route as Route.Creator).creatorId

    private val callback: CreatorDetailCallback get() = host.activity

    init {
        viewModel.load(creatorId)
    }

    @Composable
    override fun Content() {
        val state by viewModel.state.collectAsState()
        val signed by remember(creatorId) { Repository.get().signedBy(creatorId) }.collectAsState(emptyList())
        CreatorDetailScreen(
            state = state,
            signed = signed,
            actions = CreatorDetailActions(
                onFilter = { viewModel.creator?.let { callback.filterByCreator(it) } },
                onRetry = { viewModel.load(creatorId, force = true) },
                onIssue = { callback.openIssue(it) },
            ),
        )
    }

    override fun onShown() {
        // The page's own heading is the name; the toolbar says what kind of page this is
        activity.setTitle(getString(R.string.filter_type_creator))
    }

    interface CreatorDetailCallback {
        /** Adds [creator] to the filter and goes back to the list it filters. */
        fun filterByCreator(creator: Creator)
        fun openIssue(issueId: Int)
    }

}
