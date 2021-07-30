package com.wtb.comiccollector.fragments_view_models

import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.FullSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "IssueListViewModel"

@ExperimentalCoroutinesApi
class IssueListViewModel : ListViewModel<FullIssue>() {

    private val seriesIdLiveData = MutableLiveData<Int>()

    var seriesLiveData: LiveData<FullSeries?> = seriesIdLiveData.switchMap { seriesId ->
        repository.getSeries(seriesId).asLiveData()
    }

    override fun setFilter(filter: SearchFilter) {
        super.setFilter(filter)
        val series = filter.mSeries
        if (series != null) {
            seriesIdLiveData.value = series.seriesId
        }
    }

    val issueList: Flow<PagingData<FullIssue>> = filter.switchMap { filter ->
        repository.getIssuesByFilterPaged(filter).asLiveData()
    }.asFlow().cachedIn(viewModelScope)

    fun updateIssueCover(issueId: Int) = repository.updateIssueCover(issueId)
}