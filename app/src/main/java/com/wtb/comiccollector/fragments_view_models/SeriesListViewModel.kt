package com.wtb.comiccollector.fragments_view_models

import android.os.Parcelable
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.FullSeries
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

    fun getIssue(issueId: Int, markedDelete: Boolean = true): Flow<FullIssue?> = repository
        .getIssue(issueId, markedDelete)

    fun getIssueBySeries(series: FullSeries) =
        repository.getIssuesByFilter(SearchFilter(series = series))

    fun updateIssuesBySeries(series: FullSeries) {
        CoroutineScope(Dispatchers.Default).launch {
            getIssueBySeries(series).collectLatest { issues ->
                issues.forEach { getIssue(it.issue.issueId) }
            }
        }
    }

    fun saveSeriesListState(instanceState: Parcelable?) {
        repository.saveSeriesListState = instanceState
    }

    fun getSeriesListState() = repository.saveSeriesListState

    companion object {
        private const val TAG = APP + "SeriesListViewModel"
    }
}
