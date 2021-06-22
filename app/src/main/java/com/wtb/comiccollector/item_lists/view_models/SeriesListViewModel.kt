package com.wtb.comiccollector.item_lists.view_models

import android.util.Log
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
        Log.d(
            TAG, "Setting filter: S: ${filter.mSeries?.seriesName} P: ${filter.mPublishers.size} " +
                    "C: ${filter.mCreators.size} M: ${filter.mMyCollection} ${filter.mStartDate} " +
                    "${filter.mEndDate} T: ${filter.mTextFilter?.text}"
        )
        filterLiveData.value = filter
    }
}
