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

import android.net.Uri
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.database.models.FullSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalCoroutinesApi
class SeriesListViewModel : ListViewModel<FullSeries>() {

    override val itemList: Flow<PagingData<FullSeries>> = filter.switchMap {
        repository.getSeriesByFilterPaged(it).asLiveData()
    }.asFlow().cachedIn(viewModelScope)

    internal fun updateIssue(issueId: Int, markedDelete: Boolean = true) {
        repository.updateIssue(issueId, markedDelete)
    }

    /** Looks up the series' earliest existing cover off the main thread; [onResult] gets null if none. */
    internal fun loadEarliestCover(seriesId: Int, onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            onResult(withContext(Dispatchers.IO) { repository.getEarliestCoverUri(seriesId) })
        }
    }

    private fun getIssueBySeries(series: FullSeries) =
        repository.getIssuesByFilter(SearchFilter(series = series))

    internal fun updateIssuesBySeries(series: FullSeries) {
        viewModelScope.launch(Dispatchers.Default) {
            getIssueBySeries(series).collectLatest { issues ->
                issues.forEach { updateIssue(it.issue.issueId) }
            }
        }
    }

    companion object {
        private const val TAG = APP + "SeriesListViewModel"
    }
}
