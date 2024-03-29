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

import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.database.models.FullCreator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "SeriesListViewModel"

@ExperimentalCoroutinesApi
class CreatorListViewModel : ListViewModel<FullCreator>() {

    override val itemList: Flow<PagingData<FullCreator>> = filter.switchMap {
        repository.getCreatorsByFilterPaged(it).asLiveData()
    }.asFlow().cachedIn(viewModelScope)
}
