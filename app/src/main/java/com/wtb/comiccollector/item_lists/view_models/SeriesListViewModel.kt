package com.wtb.comiccollector.item_lists.view_models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "SeriesListViewModel"

class SeriesListViewModel : ViewModel() {
    private val repository: Repository = Repository.get()
    private val filterLiveData = MutableLiveData<Filter?>(null)

    //    var seriesList: LiveData<PagingData<FullSeries>> = Transformations.switchMap(filterLiveData)
//    {
//        it?.let { filter ->
//            Log.d(TAG, "seriesList switchMap filterLiveData $filter")
//            Pager(
//                config = PagingConfig(
//                    pageSize = REQUEST_LIMIT,
//                    enablePlaceholders = true,
//                    maxSize = 200
//                )
//            ) {
//                repository.getSeriesByFilterPagingSource(filter)
//            }.liveData
//        }
//    }
//
    fun seriesList(): Flow<PagingData<FullSeries>>? {
        val filterValue = filterLiveData.value

        return filterValue?.let { filter ->
            repository.getSeriesByFilterPaged(filter)
        }
    }

    fun setFilter(filter: Filter) {
        filterLiveData.value = filter.clone()
    }
}
