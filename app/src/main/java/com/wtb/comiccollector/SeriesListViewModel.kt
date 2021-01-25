package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import java.util.*

class SeriesListViewModel : ViewModel() {
    private val issueRepository: IssueRepository = IssueRepository.get()

    val seriesListLiveData: LiveData<List<Series>> = issueRepository.getSeriesList()

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

    fun getNewSeries(): LiveData<Series?> = issueRepository.newSeries
}