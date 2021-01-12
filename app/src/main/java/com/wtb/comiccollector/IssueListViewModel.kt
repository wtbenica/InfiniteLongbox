package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

class IssueListViewModel: ViewModel() {
    fun addIssue(issue: Issue) {
        issueRepository.addIssue(issue)
    }
    
    private val issueRepository: IssueRepository = IssueRepository.get()
    val issueListLiveData: LiveData<List<Issue>> = issueRepository.getIssues()
}