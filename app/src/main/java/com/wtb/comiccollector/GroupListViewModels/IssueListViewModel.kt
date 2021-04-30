package com.wtb.comiccollector.GroupListViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.Issue
import com.wtb.comiccollector.IssueRepository
import com.wtb.comiccollector.database.models.Series

private const val TAG = "NewIssueListViewModel"

class IssueListViewModel : ViewModel() {
    private val issueRepository: IssueRepository = IssueRepository.get()
    private val seriesIdLiveData = MutableLiveData<Int>()
    private val filterLiveData = MutableLiveData<Filter?>(null)

    var seriesLiveData: LiveData<Series?> =
        Transformations.switchMap(seriesIdLiveData) { seriesId ->
            issueRepository.getSeries(seriesId)
        }

    var issueListLiveData: LiveData<List<FullIssue>> =
        Transformations.switchMap(filterLiveData) { filter ->
            filter?.let { issueRepository.getIssuesByFilter(it) }
        }

    fun setFilter(filter: Filter) {
        filterLiveData.value = filter
    }

    fun addIssue(issue: Issue) {
        Log.d(TAG, "addIssue")
        issueRepository.saveIssue(issue)
    }
}