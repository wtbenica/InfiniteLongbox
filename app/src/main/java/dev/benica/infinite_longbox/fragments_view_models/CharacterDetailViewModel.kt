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

package dev.benica.infinite_longbox.fragments_view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.repository.CharacterInfo
import dev.benica.infinite_longbox.repository.LoadTracker
import dev.benica.infinite_longbox.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the character page shows: its row on the phone at once, the rest when the server answers. */
sealed interface CharacterDetailState {
    /** [character] is the phone's own row, so the name shows while the rest loads. */
    data class Loading(val character: Character?) : CharacterDetailState
    data class Failed(val character: Character?) : CharacterDetailState
    data class Loaded(val info: CharacterInfo) : CharacterDetailState
}

@ExperimentalCoroutinesApi
class CharacterDetailViewModel : ViewModel() {
    private val repository = Repository.get()

    private val _state = MutableStateFlow<CharacterDetailState>(CharacterDetailState.Loading(null))
    val state: StateFlow<CharacterDetailState> = _state

    /** The character on the phone, for adding it as a filter; null until [load] finds it. */
    var character: Character? = null
        private set

    private var loadedId: Int? = null

    fun load(characterId: Int, force: Boolean = false) {
        if (!force && loadedId == characterId) return
        loadedId = characterId
        viewModelScope.launch {
            character = repository.getCharacterById(characterId)
            _state.value = CharacterDetailState.Loading(character)
            val info = LoadTracker.track { repository.getCharacterInfo(characterId) }
            _state.value = info?.let { CharacterDetailState.Loaded(it) } ?: CharacterDetailState.Failed(character)
        }
    }
}
