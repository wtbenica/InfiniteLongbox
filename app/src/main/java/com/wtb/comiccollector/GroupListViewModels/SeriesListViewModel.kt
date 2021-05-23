package com.wtb.comiccollector.GroupListViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.IssueRepository
import com.wtb.comiccollector.database.Daos.REQUEST_LIMIT
import com.wtb.comiccollector.database.models.Series
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "SeriesListViewModel"

class SeriesListViewModel : ViewModel() {
    private val issueRepository: IssueRepository = IssueRepository.get()
    private val filterLiveData = MutableLiveData<Filter?>(null)

    var seriesListLiveData: LiveData<List<Series>> = Transformations.switchMap(filterLiveData) {
        it?.let { filter ->
            issueRepository.getSeriesByFilterLiveData(filter)
        }
    }

    fun seriesList(filter: Filter): Flow<PagingData<Series>> = Pager(
        config = PagingConfig(
            pageSize = REQUEST_LIMIT,
            enablePlaceholders = true,
            maxSize = 200
        )
    ) {
        issueRepository.getSeriesByFilter(filter)
    }.flow


    fun setFilter(filter: Filter) {
        filterLiveData.value = filter
    }
}
