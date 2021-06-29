package com.wtb.comiccollector.fragments_view_models

import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "SeriesListViewModel"

@ExperimentalCoroutinesApi
class SeriesListViewModel : ViewModel() {
    private val repository: Repository = Repository.get()

    private val filterLiveData = MutableLiveData(SearchFilter())

    val seriesList: Flow<PagingData<FullSeries>> = filterLiveData.switchMap { filter ->
        filter.let {
            repository.getSeriesByFilterPaged(it).asLiveData()
        }
    }.asFlow().cachedIn(viewModelScope)

    fun setFilter(filter: SearchFilter) {
        filterLiveData.value = filter
    }
}
