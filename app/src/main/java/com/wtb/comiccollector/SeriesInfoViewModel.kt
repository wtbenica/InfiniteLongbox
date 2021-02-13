package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.util.*

private const val TAG = "IssueDetailViewModel"

class SeriesInfoViewModel : ViewModel() {

    private val issueRepository: IssueRepository = IssueRepository.get()
    private val seriesIdLiveData = MutableLiveData<UUID>()
    private val publisherIdLiveData = MutableLiveData<UUID>()

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
        issueRepository.updateSeries(series)
    }

    fun addSeries(series: Series) {
        // TODO: Check if series exists
        issueRepository.addSeries(series)
    }

    fun deleteSeries(series: Series) {
        issueRepository.deleteSeries(series)
    }

    fun loadSeries(seriesId: UUID) {
        seriesIdLiveData.value = seriesId
    }

    fun loadPublisher(publisherId: UUID) {
        publisherIdLiveData.value = publisherId
    }
}