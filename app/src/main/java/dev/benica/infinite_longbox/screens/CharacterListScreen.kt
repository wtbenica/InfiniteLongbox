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
import androidx.compose.runtime.remember
import dev.benica.infinite_longbox.nav.ScreenHost
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.database.models.FullCharacter
import dev.benica.infinite_longbox.viewmodels.CharacterListViewModel
import dev.benica.infinite_longbox.ui.lists.NameCardState
import dev.benica.infinite_longbox.ui.lists.NameListItem
import dev.benica.infinite_longbox.ui.lists.NameListItemSkeleton
import kotlinx.coroutines.ExperimentalCoroutinesApi

/** The characters, with the name they go by and the publisher. */
@ExperimentalCoroutinesApi
class CharacterListScreen(host: ScreenHost) : ComposeListScreen<FullCharacter>(host) {

    override val viewModel: CharacterListViewModel by lazy { host.entry.viewModel<CharacterListViewModel>() }
    override val minColSizeDp: Int
        get() = 600

    override fun onShown() {
        callback.setTitle()
    }

    override fun key(item: FullCharacter): Any = item.character.characterId

    @Composable
    override fun Skeleton(index: Int) = NameListItemSkeleton(placeholderKey = "character|$index")

    @Composable
    override fun Row(item: FullCharacter, index: Int) {
        val state = remember(item) {
            NameCardState(
                name = item.character.name,
                details = listOf(item.character.alterEgo.orEmpty(), item.publisher.publisher),
            )
        }
        NameListItem(
            state,
            onClick = { (callback as CharacterListCallback).onCharacterSelected(item.character) },
            onInfo = { (callback as CharacterListCallback).onCharacterInfo(item.character) },
        )
    }

    override fun scrollLabel(item: FullCharacter): String? =
        if (sortedByName()) initialOf(item.character.compareValue) else null

    interface CharacterListCallback : ScreenCallback {
        fun onCharacterSelected(character: Character)

        /** The info button: open the character's page rather than filter by it. */
        fun onCharacterInfo(character: Character)
    }

}
