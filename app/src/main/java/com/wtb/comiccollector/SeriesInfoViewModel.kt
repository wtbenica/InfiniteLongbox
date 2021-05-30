package com.wtb.comiccollector

import androidx.lifecycle.*
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.repository.Repository

private const val TAG = "IssueDetailViewModel"

class SeriesInfoViewModel : ViewModel() {

    private val repository: Repository = Repository.get()
    private val seriesIdLiveData = MutableLiveData<Int>()
    private val publisherIdLiveData = MutableLiveData<Int>()

    var seriesLiveData: LiveData<Series?> =
        Transformations.switchMap(seriesIdLiveData) { seriesId ->
            repository.getSeries(seriesId).asLiveData()
        }

    var publisherLiveData: LiveData<Publisher?> =
        Transformations.switchMap(publisherIdLiveData) { publisherId ->
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