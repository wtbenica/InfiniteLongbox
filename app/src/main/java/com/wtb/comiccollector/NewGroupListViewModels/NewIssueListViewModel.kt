package com.wtb.comiccollector.NewGroupListViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.wtb.comiccollector.*

private const val TAG = "NewIssueListViewModel"

class NewIssueListViewModel : ViewModel() {
    private val issueRepository: NewIssueRepository = NewIssueRepository.get()
    private val seriesIdLiveData = MutableLiveData<Int>()

    var seriesLiveData: LiveData<Series?> =
        Transformations.switchMap(seriesIdLiveData) { seriesId ->
            issueRepository.getSeries(seriesId)
        }

    var issueListLiveData: LiveData<List<FullIssue>> =
        Transformations.switchMap(seriesIdLiveData) { seriesId ->
            issueRepository.getIssuesBySeries(seriesId)
        }

    fun loadSeries(seriesId: Int) {
        Log.d(TAG, "Loading series: $seriesId")
        seriesIdLiveData.value = seriesId
    }

    fun addIssue(issue: Issue) {
        Log.d(TAG, "addIssue")
        issueRepository.saveIssue(issue)
    }

    fun addSeries(series: Series) {
        issueRepository.saveSeries(series)
    }

    fun addCreator(creator: Creator) {
        issueRepository.saveCreator(creator)
    }

    fun addRole(role: Role) {
        issueRepository.saveRole(role)
    }

    fun addCredit(issue: Issue, creator: Creator, role: Role) {
        issueRepository.saveCredit(
            Credit(
                storyId = issue.issueId,
                nameDetailId = creator.creatorId,
                roleId = role.roleId
            )
        )
    }

    fun getSeries(seriesId: Int): LiveData<Series?> = issueRepository.getSeries(seriesId)
}