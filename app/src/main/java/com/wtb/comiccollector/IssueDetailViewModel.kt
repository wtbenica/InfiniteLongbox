package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.util.*

class IssueDetailViewModel : ViewModel() {

    private val issueRepository: IssueRepository = IssueRepository.get()
    private val issueIdLiveData = MutableLiveData<UUID>()
    private val seriesIdLiveData = MutableLiveData<UUID>()

    var issueLiveData: LiveData<Issue?> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getIssue(issueId)
        }

    var seriesLiveData: LiveData<Series?> =
        Transformations.switchMap(issueLiveData) { issue ->
            issue?.seriesId?.let { issueRepository.getSeries(it) }
        }

    var allSeriesLiveData: LiveData<List<Series>> = issueRepository.allSeries

    var allPublishersLiveData: LiveData<List<Publisher>> = issueRepository.allPublishers

    fun loadIssue(issueId: UUID) {
        issueIdLiveData.value = issueId
        seriesIdLiveData.value = issueLiveData.value?.seriesId
    }

    fun saveIssue(issue: Issue) {
        issueRepository.updateIssue(issue)
    }

    fun deleteIssue(issue: Issue) {
        issueRepository.deleteIssue(issue)
    }

    fun loadSeries(seriesId: UUID) {
        seriesIdLiveData.value = seriesId
    }

    fun saveSeries(series: Series) {
        issueRepository.updateSeries(series)
    }

    fun addSeries(series: Series) {
        // TODO: Check if series exists
        issueRepository.addSeries(series)
    }

    fun deleteSeries(series: Series) {
        issueRepository.deleteSeries(series)
    }

    fun getNewSeries(): LiveData<Series?> = issueRepository.newSeries
}