package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import java.util.*

class IssueListViewModel : ViewModel() {
    private val issueRepository: IssueRepository = IssueRepository.get()
    val issueListLiveData: LiveData<List<FullIssue>> = issueRepository.getIssues()

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
        issueRepository.addCredit(Credit(issue.issueId, creator.creatorId, role.roleId))
    }

    fun getSeries(seriesId: UUID): LiveData<Series?> = issueRepository.getSeries(seriesId)

}