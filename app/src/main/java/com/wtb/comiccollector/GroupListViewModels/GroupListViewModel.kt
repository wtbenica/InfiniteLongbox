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
        issueRepository.saveIssue(issue)
    }

    fun updateIssue(issue: Issue) {
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