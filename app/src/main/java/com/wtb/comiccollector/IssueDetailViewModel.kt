package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.util.*

private const val TAG = "IssueDetailViewModel"

class IssueDetailViewModel : ViewModel() {

    private val issueRepository: IssueRepository = IssueRepository.get()
    private val issueIdLiveData = MutableLiveData<UUID>()
    private val seriesIdLiveData = MutableLiveData<UUID>()
    private val writerIdLiveData = MutableLiveData<UUID>()

    var issueLiveData: LiveData<Issue?> =
        Transformations.switchMap(issueIdLiveData) { issueId ->
            issueRepository.getIssue(issueId)
        }

    var seriesLiveData: LiveData<Series?> =
        Transformations.switchMap(issueLiveData) { issue ->
            issue?.seriesId?.let { issueRepository.getSeries(it) }
        }

    var writerLiveData: LiveData<Creator> =
        Transformations.switchMap(issueLiveData) { issue ->
            issue?.writerId?.let { issueRepository.getCreator(it) }
        }

    val pencillerLiveData: LiveData<Creator> =
        Transformations.switchMap(issueLiveData) { issue ->
            issue?.pencillerId?.let { issueRepository.getCreator(it) }
        }

    val inkerLiveData: LiveData<Creator> =
        Transformations.switchMap(issueLiveData) { issue ->
            issue?.inkerId?.let { issueRepository.getCreator(it) }
        }

    var allSeriesLiveData: LiveData<List<Series>> = issueRepository.allSeries

    var allPublishersLiveData: LiveData<List<Publisher>> = issueRepository.allPublishers

    var allCreatorsLiveData: LiveData<List<Creator>> = issueRepository.allCreators

    var allWritersLiveData: LiveData<List<Creator>> = issueRepository.allWriters

    fun loadIssue(issueId: UUID) {
        issueIdLiveData.value = issueId
        seriesIdLiveData.value = issueLiveData.value?.seriesId
    }

    fun updateIssue(issue: Issue) {
        issueRepository.updateIssue(issue)
    }

    fun deleteIssue(issue: Issue) {
        issueRepository.deleteIssue(issue)
    }

    fun updateSeries(series: Series) {
        issueRepository.updateSeries(series)
    }

    fun addSeries(series: Series) {
        // TODO: Check if series exists
        issueRepository.addSeries(series)
    }

    fun addCreator(creator: Creator) {
        issueRepository.addCreator(creator)
    }

    fun loadSeries(seriesId: UUID) {
        seriesIdLiveData.value = seriesId
    }

    fun deleteSeries(series: Series) {
        issueRepository.deleteSeries(series)
    }

    fun getNewSeries(): LiveData<Series?> = issueRepository.newSeries
}