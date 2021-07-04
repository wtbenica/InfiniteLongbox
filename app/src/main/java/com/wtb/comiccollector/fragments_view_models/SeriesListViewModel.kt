package com.wtb.comiccollector.fragments_view_models

import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.FullSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "SeriesListViewModel"

@ExperimentalCoroutinesApi
class SeriesListViewModel : ListViewModel() {

    val seriesList: Flow<PagingData<FullSeries>> = filter.switchMap { filter ->
        filter.let {
            repository.getSeriesByFilterPaged(it).asLiveData()
        }
    }.asFlow().cachedIn(viewModelScope)

    fun setFilter(filter: SearchFilter) {
        filterLiveData.value = filter
    }
}
