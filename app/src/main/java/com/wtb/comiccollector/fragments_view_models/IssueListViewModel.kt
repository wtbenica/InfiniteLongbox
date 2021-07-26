package com.wtb.comiccollector.fragments_view_models

import android.util.Log
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.database.models.Issue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "IssueListViewModel"

@ExperimentalCoroutinesApi
class IssueListViewModel : ListViewModel<FullIssue>() {

    private val seriesIdLiveData = MutableLiveData<Int>()
    val seriesId: LiveData<Int> = seriesIdLiveData

    var seriesLiveData: LiveData<FullSeries?> =
        Transformations.switchMap(filterLiveData) {
            it?.let { filter ->
                filter.mSeries?.seriesId?.let { id -> repository.getSeries(id).asLiveData() }
            }
        }

    val issueList: Flow<PagingData<FullIssue>> = filter.switchMap { filter ->
        Log.d(TAG, "issueList: filterChanged")
        repository.getIssuesByFilterPaged(filter).asLiveData()
    }.asFlow().cachedIn(viewModelScope)

    val series: Flow<FullSeries?> = seriesId.switchMap { id ->
        repository.getSeries(id).asLiveData()
    }.asFlow()

    fun addIssue(issue: Issue) {
        Log.d(TAG, "addIssue")
        repository.saveIssue(issue)
    }

    fun updateIssue(issue: FullIssue) = repository.updateIssue(issue)

    fun updateIssueCover(issue: FullIssue) = repository.updateIssueCover(issue)
}