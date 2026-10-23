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
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.fragments_view_models.CharacterDetailViewModel
import dev.benica.infinite_longbox.ui.character.CharacterDetailActions
import dev.benica.infinite_longbox.ui.character.CharacterDetailScreen
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val ARG_CHARACTER_ID = "character_id"

/** A character's page: who they are, other names, relations, teams and where they appear. */
@ExperimentalCoroutinesApi
class CharacterDetailFragment : Fragment() {
    private val viewModel: CharacterDetailViewModel by viewModels()

    private val characterId: Int
        get() = requireArguments().getInt(ARG_CHARACTER_ID)

    private val callback: CharacterDetailCallback?
        get() = activity as? CharacterDetailCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewModel.load(characterId)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                InfiniteLongboxTheme {
                    val state by viewModel.state.collectAsState()
                    CharacterDetailScreen(
                        state = state,
                        actions = CharacterDetailActions(
                            onFilter = { viewModel.character?.let { callback?.filterByCharacter(it) } },
                            onRetry = { viewModel.load(characterId, force = true) },
                            onCharacter = { callback?.openCharacter(it) },
                            onIssue = { callback?.openIssue(it) },
                        ),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The page's own heading is the name; the toolbar says what kind of page this is
        (activity as? ListFragmentCallback)?.setTitle(getString(R.string.filter_type_character))
    }

    interface CharacterDetailCallback {
        /** Adds [character] to the filter and goes back to the list it filters. */
        fun filterByCharacter(character: Character)
        fun openCharacter(characterId: Int)
        fun openIssue(issueId: Int)
    }

    companion object {
        @JvmStatic
        fun newInstance(characterId: Int) = CharacterDetailFragment().apply {
            arguments = Bundle().apply { putInt(ARG_CHARACTER_ID, characterId) }
        }
    }
}
