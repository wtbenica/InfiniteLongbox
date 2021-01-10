package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.util.*

class IssueDetailViewModel : ViewModel() {

    private val issueRepository: IssueRepository = IssueRepository.get()
    private val issueIdLiveData = MutableLiveData<UUID>()

    var issueLiveData: LiveData<Issue?> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getIssue(issueId)
        }

    fun loadIssue(issueId: UUID) {
        issueIdLiveData.value = issueId
    }

    fun saveIssue(issue: Issue) {
        issueRepository.updateIssue(issue)
    }

    fun deleteIssue(issue: Issue) {
        issueRepository.deleteIssue(issue)
    }
}