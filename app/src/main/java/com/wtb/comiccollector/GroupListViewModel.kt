package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.*

interface GroupListViewModel<T> {
    val issueRepository: IssueRepository
    val filterIdLiveData: MutableLiveData<UUID?>

    val creatorListLiveData: LiveData<List<Creator>>

    fun addIssue(issue: Issue) {
        issueRepository.addIssue(issue)
    }
}