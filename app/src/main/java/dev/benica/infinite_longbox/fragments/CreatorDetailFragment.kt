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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.benica.infinite_longbox.database.models.Creator
import dev.benica.infinite_longbox.fragments_view_models.CreatorDetailState
import dev.benica.infinite_longbox.fragments_view_models.CreatorDetailViewModel
import dev.benica.infinite_longbox.ui.CreatorDetailActions
import dev.benica.infinite_longbox.ui.CreatorDetailScreen
import dev.benica.infinite_longbox.ui.InfiniteLongboxTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

private const val ARG_CREATOR_ID = "creator_id"

/** A creator's page: who they are, other names, credits and the series they worked on most. */
@ExperimentalCoroutinesApi
class CreatorDetailFragment : Fragment() {
    private val viewModel: CreatorDetailViewModel by viewModels()

    private val creatorId: Int
        get() = requireArguments().getInt(ARG_CREATOR_ID)

    private val callback: CreatorDetailCallback?
        get() = activity as? CreatorDetailCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewModel.load(creatorId)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                InfiniteLongboxTheme {
                    val state by viewModel.state.collectAsState()
                    CreatorDetailScreen(
                        state = state,
                        actions = CreatorDetailActions(
                            onFilter = { viewModel.creator?.let { callback?.filterByCreator(it) } },
                            onRetry = { viewModel.load(creatorId, force = true) },
                            onIssue = { callback?.openIssue(it) },
                        ),
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // The title is the creator's name, as soon as it is known
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { state ->
                    val name = when (state) {
                        is CreatorDetailState.Loaded -> state.info.name
                        is CreatorDetailState.Loading -> state.creator?.name
                        is CreatorDetailState.Failed -> state.creator?.name
                    }
                    (activity as? ListFragment.ListFragmentCallback)?.setTitle(name)
                }
            }
        }
    }

    interface CreatorDetailCallback {
        /** Adds [creator] to the filter and goes back to the list it filters. */
        fun filterByCreator(creator: Creator)
        fun openIssue(issueId: Int)
    }

    companion object {
        @JvmStatic
        fun newInstance(creatorId: Int) = CreatorDetailFragment().apply {
            arguments = Bundle().apply { putInt(ARG_CREATOR_ID, creatorId) }
        }
    }
}
