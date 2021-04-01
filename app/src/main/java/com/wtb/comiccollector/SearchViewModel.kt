package com.wtb.comiccollector

import androidx.lifecycle.ViewModel

class SearchViewModel : ViewModel() {

    val issueRepository: NewIssueRepository = NewIssueRepository.get()

    val filterListLiveData = issueRepository.everything
}