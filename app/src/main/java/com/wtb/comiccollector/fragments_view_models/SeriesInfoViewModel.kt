package com.wtb.comiccollector.fragments_view_models

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = "SeriesInfoViewModel"

/**
 * Currently unused, but if the ability to edit/enter series info is put back, this is the
 * view model to do it, along with [com.wtb.comiccollector.SeriesInfoDialogFragment]
 */
@ExperimentalCoroutinesApi
class SeriesInfoViewModel : ViewModel() {

//    private val repository: Repository = Repository.get()
//
//    private val seriesIdLiveData = MutableLiveData<Int>()
//
//    var seriesLiveData: LiveData<FullSeries?> = seriesIdLiveData.switchMap { seriesId ->
//        repository.getSeries(seriesId).asLiveData()
//    }
//
//    var allPublishersLiveData: LiveData<List<Publisher>> = repository.allPublishers.asLiveData()
//
//    fun updateSeries(series: Series) {
//        repository.saveSeries(series)
//    }
//
//    fun addSeries(series: Series) {
//        // TODO: Check if series exists
//        repository.saveSeries(series)
//    }
//
//    fun deleteSeries(series: Series) {
//        repository.deleteSeries(series)
//    }
//
//    fun loadSeries(seriesId: Int) {
//        seriesIdLiveData.value = seriesId
//    }
}