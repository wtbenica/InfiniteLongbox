package com.wtb.comiccollector.GroupListViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.IssueRepository
import com.wtb.comiccollector.database.models.Series

private const val TAG = APP + "SeriesListViewModel"

class SeriesListViewModel : ViewModel() {
    private val issueRepository: IssueRepository = IssueRepository.get()
    private val filterLiveData = MutableLiveData<Filter?>(null)

    val seriesListLiveData: LiveData<List<Series>> =
        Transformations.switchMap(filterLiveData) { filter ->
            filter?.let { issueRepository.getSeriesByFilter(it) }
        }

    fun setFilter(filter: Filter) {
        filterLiveData.value = filter
    }
}
