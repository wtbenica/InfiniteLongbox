package com.wtb.comiccollector.item_lists.view_models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.Daos.REQUEST_LIMIT
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.repository.Repository

private const val TAG = APP + "SeriesListViewModel"

class SeriesListViewModel : ViewModel() {
    private val repository: Repository = Repository.get()
    private val filterLiveData = MutableLiveData<Filter?>(null)

    var seriesListLiveData: LiveData<List<Series>> = Transformations.switchMap(filterLiveData) {
        it?.let { filter ->
            repository.getSeriesByFilterLiveData(filter)
        }
    }

    var seriesList: LiveData<PagingData<FullSeries>> = Transformations.switchMap(filterLiveData)
    {
        it?.let { filter ->
            Pager(
                config = PagingConfig(
                    pageSize = REQUEST_LIMIT,
                    enablePlaceholders = true,
                    maxSize = 200
                )
            ) {
                repository.getSeriesByFilterPagingSource(filter)
            }.liveData
        }
    }

    fun setFilter(filter: Filter) {
        Log.d(TAG, "setFilter: ${filter.mSeries}")
        filterLiveData.value = filter
    }
}
