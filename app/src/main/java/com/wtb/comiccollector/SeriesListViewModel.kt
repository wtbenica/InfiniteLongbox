package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.util.*

private const val TAG = "SeriesListViewModel"

class SeriesListViewModel : ViewModel() {
    private val issueRepository: IssueRepository = IssueRepository.get()
    private val creatorIdFilterLiveData = MutableLiveData<UUID?>(null)

    val seriesListLiveData: LiveData<List<Series>> =
        Transformations.switchMap(creatorIdFilterLiveData) { creatorId ->
            if (creatorId == null) {
                issueRepository.getSeriesList()
            } else {
                issueRepository.getSeriesByCreator(creatorId)
            }
        }

    val creatorListLiveData: LiveData<List<Creator>> = issueRepository.allCreators

    fun filterByCreator(creatorId: UUID) {
        creatorIdFilterLiveData.value = creatorId
    }

    fun addIssue(issue: Issue) {
        issueRepository.addIssue(issue)
    }

    fun updateIssue(issue: Issue) {
        issueRepository.updateIssue(issue)
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
