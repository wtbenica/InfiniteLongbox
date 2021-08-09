package com.wtb.comiccollector.fragments_view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.paging.PagingData
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.FullSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "SeriesListViewModel"

@ExperimentalCoroutinesApi
class SeriesListViewModel : ListViewModel<FullSeries>() {

    val seriesList: LiveData<PagingData<FullSeries>> = filter.switchMap {
        repository.getSeriesByFilterPaged(it).asLiveData()
    }
}
