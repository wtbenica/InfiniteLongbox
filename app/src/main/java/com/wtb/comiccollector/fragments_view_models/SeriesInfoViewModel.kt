package com.wtb.comiccollector.fragments_view_models

import androidx.lifecycle.*
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = "IssueDetailViewModel"

@ExperimentalCoroutinesApi
class SeriesInfoViewModel : ViewModel() {

    private val repository: Repository = Repository.get()
    private val seriesIdLiveData = MutableLiveData<Int>()
    private val publisherIdLiveData = MutableLiveData<Int>()

    var seriesLiveData: LiveData<Series?> = seriesIdLiveData.switchMap { seriesId ->
        repository.getSeries(seriesId).asLiveData()
    }

    var publisherLiveData: LiveData<Publisher?> = publisherIdLiveData.switchMap { publisherId ->
        repository.getPublisher(publisherId).asLiveData()
    }

    var allPublishersLiveData: LiveData<List<Publisher>> = repository.allPublishers.asLiveData()

    fun updateSeries(series: Series) {
        repository.saveSeries(series)
    }

    fun addSeries(series: Series) {
        // TODO: Check if series exists
        repository.saveSeries(series)
    }

    fun deleteSeries(series: Series) {
        repository.deleteSeries(series)
    }

    fun loadSeries(seriesId: Int) {
        seriesIdLiveData.value = seriesId
    }

    fun loadPublisher(publisherId: Int) {
        publisherIdLiveData.value = publisherId
    }
}