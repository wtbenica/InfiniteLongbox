package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.util.*

private const val TAG = "CreatorListViewModel"

class CreatorListViewModel : ViewModel(),
    GroupListViewModel<Creator> {

    override val issueRepository: IssueRepository = IssueRepository.get()
    override val filterIdLiveData = MutableLiveData<UUID?>(null)

    override val creatorListLiveData: LiveData<List<Creator>> = issueRepository.allCreators

    val seriesListLiveData: LiveData<List<Series>> =
        Transformations.switchMap(filterIdLiveData) { creatorId ->
            if (creatorId == null) {
                issueRepository.getSeriesList()
            } else {
                issueRepository.getSeriesByCreator(creatorId)
            }
        }


    fun filter(
        seriesId: UUID? = null,
        creatorId: UUID? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ) {

    }

    fun filterByCreator(creatorId: UUID) {
        filterIdLiveData.value = creatorId
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
