package com.wtb.comiccollector.GroupListViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wtb.comiccollector.*
import java.time.LocalDate

private const val TAG = "GroupListViewModel"

abstract class GroupListViewModel<T>: ViewModel() {
    val issueRepository: IssueRepository = IssueRepository.get()
    val filterIdLiveData = MutableLiveData<Int?>(null)

    abstract val objectListLiveData: LiveData<List<T>>

    fun filter(
        filterId: Int? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ) {
        filterIdLiveData.value = filterId
    }

    fun addIssue(issue: Issue) {
        Log.d(TAG, "addIssue")
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

    fun getSeries(seriesId: Int): LiveData<Series?> = issueRepository.getSeries(seriesId)
}