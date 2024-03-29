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

import android.os.Parcelable
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.database.models.AUTO_ID
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.FullSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest

private const val TAG = APP + "IssueListViewModel"

@ExperimentalCoroutinesApi
class IssueListViewModel : ListViewModel<FullIssue>() {

    private val _seriesId = MutableStateFlow(AUTO_ID)
    val seriesId: StateFlow<Int> = _seriesId

    var seriesLiveData: LiveData<FullSeries?> =
        seriesId.flatMapLatest { seriesId -> repository.getSeries(seriesId) }.asLiveData()

    override fun setFilter(filter: SearchFilter) {
        super.setFilter(filter)
        val series = filter.mSeries
        if (series != null) {
            _seriesId.value = series.series.seriesId
        }
    }

    override val itemList: Flow<PagingData<FullIssue>> = filter.switchMap { filter ->
        repository.getIssuesByFilterPaged(filter).asLiveData()
    }.asFlow().cachedIn(viewModelScope)

    fun updateIssueCover(issueId: Int) = repository.updateIssueCover(issueId)

    fun saveIssueListState(instanceState: Parcelable?) {
        repository.saveIssueListState = instanceState
    }
}