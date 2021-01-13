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

    fun getSeries(seriesId: UUID): LiveData<Series?> = issueRepository.getSeries(seriesId)

}