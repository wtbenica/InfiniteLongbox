package com.wtb.comiccollector

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.util.*

private const val TAG = "IssueListViewModel"

class IssueListViewModel : ViewModel() {
    private val issueRepository: IssueRepository = IssueRepository.get()
    private val seriesIdLiveData = MutableLiveData<UUID>()

    var seriesLiveData: LiveData<Series?> =
        Transformations.switchMap(seriesIdLiveData) { seriesId ->
            issueRepository.getSeries(seriesId)
        }

    var issueListLiveData: LiveData<List<FullIssue>> =
        Transformations.switchMap(seriesIdLiveData) { seriesId ->
            issueRepository.getIssuesBySeries(seriesId)
        }

    fun loadSeries(seriesId: UUID) {
        Log.d(TAG, "Loading series: $seriesId")
        seriesIdLiveData.value = seriesId
    }

    fun addIssue(issue: Issue) {
        issueRepository.addIssue(issue)
    }

    fun addSeries(series: Series) {
        issueRepository.addSeries(series)
    }

    fun addCreator(creator: Creator) {
        issueRepository.addCreator(creator)
    }

    fun addRole(role: Role) {
        issueRepository.addRole(role)
    }

    fun addCredit(issue: Issue, creator: Creator, role: Role) {
        issueRepository.addCredit(
            Credit(
                issueId = issue.issueId,
                creatorId = creator.creatorId,
                roleId = role.roleId
            )
        )
    }

    fun getSeries(seriesId: UUID): LiveData<Series?> = issueRepository.getSeries(seriesId)
}