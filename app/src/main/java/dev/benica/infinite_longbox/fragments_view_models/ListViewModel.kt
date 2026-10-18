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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.FilterSnapshot
import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.database.models.ListItem
import dev.benica.infinite_longbox.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalCoroutinesApi
abstract class ListViewModel<T : ListItem> : ViewModel() {
    protected val repository: Repository = Repository.get()
    abstract val itemList: Flow<PagingData<T>>

    // Nothing is listed until the list is given its filter
    private val _filter = MutableLiveData<SearchFilter>()
    val filter: LiveData<SearchFilter>
        get() = _filter

    /** False until the list has been given its filter (when it's first created). */
    var hasFilter = false
        private set

    open fun setFilter(filter: SearchFilter) {
        hasFilter = true
        _filter.value = filter
    }

    /**
     * Gives the list the filter saved by [FilterSnapshot], once its records have been looked up
     * again; [onFailed] if that can't be done.
     */
    fun restoreFilter(snapshot: String, onFailed: () -> Unit) {
        viewModelScope.launch {
            val filter = withContext(Dispatchers.IO) {
                FilterSnapshot.restore(
                    snapshot,
                    loadSeries = { repository.getSeries(it).first() },
                    loadCreator = repository::getCreatorById,
                    loadPublisher = repository::getPublisherById,
                    loadCharacter = repository::getCharacterById,
                )
            }
            if (filter != null) setFilter(filter) else onFailed()
        }
    }

    companion object {
        private const val TAG = APP + "ListViewModel"
    }
}