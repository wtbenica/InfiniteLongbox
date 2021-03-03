package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.util.*

abstract class GroupListViewModel<T>: ViewModel() {
    val issueRepository: IssueRepository = IssueRepository.get()
    val filterIdLiveData = MutableLiveData<UUID?>(null)

    abstract val objectListLiveData: LiveData<List<T>>

    fun filter(
        filterId: UUID? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ) {
        filterIdLiveData.value = filterId
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