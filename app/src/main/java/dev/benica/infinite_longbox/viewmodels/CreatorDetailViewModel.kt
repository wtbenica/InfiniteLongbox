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

package dev.benica.infinite_longbox.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.benica.infinite_longbox.database.models.Creator
import dev.benica.infinite_longbox.repository.CreatorInfo
import dev.benica.infinite_longbox.repository.LoadTracker
import dev.benica.infinite_longbox.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the creator page shows: their row on the phone at once, the rest when the server answers. */
sealed interface CreatorDetailState {
    /** [creator] is the phone's own row, so the name shows while the rest loads. */
    data class Loading(val creator: Creator?) : CreatorDetailState
    data class Failed(val creator: Creator?) : CreatorDetailState
    data class Loaded(val info: CreatorInfo) : CreatorDetailState
}

@ExperimentalCoroutinesApi
class CreatorDetailViewModel : ViewModel() {
    private val repository = Repository.get()

    private val _state = MutableStateFlow<CreatorDetailState>(CreatorDetailState.Loading(null))
    val state: StateFlow<CreatorDetailState> = _state

    /** The creator on the phone, for adding them as a filter; null until [load] finds them. */
    var creator: Creator? = null
        private set

    private var loadedId: Int? = null

    fun load(creatorId: Int, force: Boolean = false) {
        if (!force && loadedId == creatorId) return
        loadedId = creatorId
        viewModelScope.launch {
            creator = repository.getCreatorById(creatorId)
            _state.value = CreatorDetailState.Loading(creator)
            val info = LoadTracker.track { repository.getCreatorInfo(creatorId) }
            _state.value = info?.let { CreatorDetailState.Loaded(it) } ?: CreatorDetailState.Failed(creator)
        }
    }
}
