package com.wtb.comiccollector.view_models

import android.util.Log
import androidx.lifecycle.*
import androidx.paging.PagingData
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.Issue
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "IssueListViewModel"

@ExperimentalCoroutinesApi
class IssueListViewModel : ViewModel() {
    private val repository: Repository = Repository.get()
    private val seriesIdLiveData = MutableLiveData<Int>()
    private val filterLiveData = MutableLiveData(SearchFilter())

    var seriesLiveData: LiveData<Series?> =
        Transformations.switchMap(filterLiveData) {
            it?.let { filter ->
                filter.mSeries?.seriesId?.let { id -> repository.getSeries(id).asLiveData() }
            }
        }

    fun issueList(): Flow<PagingData<FullIssue>>? {
        val filterValue = this.filterLiveData.value
        Log.d(TAG, "issueList() filterValue: $filterValue")
        return filterValue?.let { filter ->
            repository.getIssuesByFilterPaged(filter)
        }
    }

    fun setFilter(filter: SearchFilter) {
        filterLiveData.value = filter
    }

    fun addIssue(issue: Issue) {
        Log.d(TAG, "addIssue")
        repository.saveIssue(issue)
    }

    fun updateIssue(issue: FullIssue) = repository.updateIssue(issue)

    fun updateIssueCover(issue: FullIssue) = repository.updateIssueCover(issue)
}