package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

private const val TAG = "IssueDetailViewModel"

class SeriesInfoViewModel : ViewModel() {

    private val issueRepository: NewIssueRepository = NewIssueRepository.get()
    private val seriesIdLiveData = MutableLiveData<Int>()
    private val publisherIdLiveData = MutableLiveData<Int>()

    var seriesLiveData: LiveData<Series?> =
        Transformations.switchMap(seriesIdLiveData) { seriesId ->
            issueRepository.getSeries(seriesId)
        }

    var publisherLiveData: LiveData<Publisher?> =
        Transformations.switchMap(publisherIdLiveData) { publisherId ->
            issueRepository.getPublisher(publisherId)
        }

    var allPublishersLiveData: LiveData<List<Publisher>> = issueRepository.allPublishers

    fun updateSeries(series: Series) {
        issueRepository.saveSeries(series)
    }

    fun addSeries(series: Series) {
        // TODO: Check if series exists
        issueRepository.saveSeries(series)
    }

    fun deleteSeries(series: Series) {
        issueRepository.deleteSeries(series)
    }

    fun loadSeries(seriesId: Int) {
        seriesIdLiveData.value = seriesId
    }

    fun loadPublisher(publisherId: Int) {
        publisherIdLiveData.value = publisherId
    }
}