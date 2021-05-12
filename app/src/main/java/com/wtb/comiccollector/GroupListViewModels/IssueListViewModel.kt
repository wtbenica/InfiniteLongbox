package com.wtb.comiccollector.GroupListViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.IssueRepository
import com.wtb.comiccollector.database.Daos.REQUEST_LIMIT
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.Issue
import com.wtb.comiccollector.database.models.Series
import kotlinx.coroutines.flow.Flow

private const val TAG = "NewIssueListViewModel"

class IssueListViewModel : ViewModel() {
    private val issueRepository: IssueRepository = IssueRepository.get()
    private val seriesIdLiveData = MutableLiveData<Int>()
    private val filterLiveData = MutableLiveData<Filter?>(null)

    var seriesLiveData: LiveData<Series?> =
        Transformations.switchMap(seriesIdLiveData) { seriesId ->
            issueRepository.getSeries(seriesId)
        }

    fun issueList(filter: Filter) : Flow<PagingData<FullIssue>> = Pager(
        config = PagingConfig(
            pageSize = REQUEST_LIMIT,
            enablePlaceholders = true,
            maxSize = 200
        )
    ) {
        issueRepository.getIssuesByFilter(filter)
    }.flow

    fun setFilter(filter: Filter) {
        filterLiveData.value = filter
    }

    fun addIssue(issue: Issue) {
        Log.d(TAG, "addIssue")
        issueRepository.saveIssue(issue)
    }

    fun updateIssue(issue: FullIssue?) {
        issueRepository.updateIssue(issue)
    }
}