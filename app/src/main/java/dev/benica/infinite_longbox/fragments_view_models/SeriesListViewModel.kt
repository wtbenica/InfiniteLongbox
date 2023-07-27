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
import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.FullSeries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class SeriesListViewModel : ListViewModel<FullSeries>() {

    override val itemList: Flow<PagingData<FullSeries>> = filter.switchMap {
        repository.getSeriesByFilterPaged(it).asLiveData()
    }.asFlow().cachedIn(viewModelScope)

    internal fun getIssue(issueId: Int, markedDelete: Boolean = true): Flow<FullIssue?> = repository.getIssue(issueId, markedDelete)

    private fun getIssueBySeries(series: FullSeries) =
        repository.getIssuesByFilter(SearchFilter(series = series))

    internal fun updateIssuesBySeries(series: FullSeries) {
        CoroutineScope(Dispatchers.Default).launch {
            getIssueBySeries(series).collectLatest { issues ->
                issues.forEach { getIssue(it.issue.issueId) }
            }
        }
    }

    companion object {
        private const val TAG = APP + "SeriesListViewModel"
    }
}
