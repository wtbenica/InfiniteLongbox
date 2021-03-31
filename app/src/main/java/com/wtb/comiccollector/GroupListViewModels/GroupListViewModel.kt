package com.wtb.comiccollector.GroupListViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wtb.comiccollector.*

private const val TAG = "GroupListViewModel"

abstract class GroupListViewModel<T : DataModel, U : DataModel> : ViewModel() {
    val issueRepository: IssueRepository = IssueRepository.get()
    val filterLiveData = MutableLiveData<Filter?>(null)

    abstract val objectListLiveData: LiveData<List<T>>

    val filterListLiveData: CombinedLiveData<List<Series>, List<Creator>, List<Filterable>?> =
        issueRepository.everything

    abstract fun filterUpdate(id: Int)

    class Filter(
        val filterId: Int? = null,
        val text: String? = null
    ) {
        fun isEmpty(): Boolean {
            return filterId == null && text == null
        }
    }

    private fun updateFilter(filter: Filter?) {
        filterLiveData.value = filter
    }

    fun filter(
        filterId: Int? = null,
        text: String? = null
    ) {
        filterId?.let { filterUpdate(it) }
        updateFilter(
            Filter(
                filterId = filterId ?: filterLiveData.value?.filterId,
                text = text ?: filterLiveData.value?.text
            )
        )
    }

    fun removeFilter(
        filterId: Int? = null,
        text: String? = null
    ) {
        updateFilter(
            Filter(
                filterId = if (filterId == null) {
                    filterLiveData.value?.filterId
                } else {
                    null
                },
                text = if (text == null) {
                    filterLiveData.value?.text
                } else {
                    null
                }
            )
        )
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