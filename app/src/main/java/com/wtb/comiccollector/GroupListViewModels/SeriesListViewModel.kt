package com.wtb.comiccollector.GroupListViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.IssueRepository
import com.wtb.comiccollector.database.Daos.REQUEST_LIMIT
import com.wtb.comiccollector.database.models.Series

private const val TAG = APP + "SeriesListViewModel"

class SeriesListViewModel : ViewModel() {
    private val issueRepository: IssueRepository = IssueRepository.get()
    private val filterLiveData = MutableLiveData<Filter?>(null)

    val seriesListLiveData: LiveData<PagedList<Series>> =
        Transformations.switchMap(filterLiveData) { filter ->
            filter?.let { issueRepository.getSeriesByFilter(it).toLiveData(REQUEST_LIMIT) }
        }

    fun setFilter(filter: Filter) {
        Log.d(TAG, "SETTING FILTER!!!! ${filter.mCurrentItems} " +
                "*************************************************************************************************************************************************************")
        filterLiveData.value = filter
    }
}
