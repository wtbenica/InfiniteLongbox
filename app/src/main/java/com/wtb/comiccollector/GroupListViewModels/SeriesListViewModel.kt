package com.wtb.comiccollector.GroupListViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Creator
import com.wtb.comiccollector.Series

private const val TAG = APP + "SeriesListViewModel"

class SeriesListViewModel : GroupListViewModel<Series, Creator>() {

    override val objectListLiveData: LiveData<List<Series>> =
        Transformations.switchMap(filterLiveData) { filter ->
            if (filter == null || filter.isEmpty()) {
                issueRepository.allSeries
            } else {
                issueRepository.getSeriesByFilter(filter)
            }
        }

    override fun filterUpdate(id: Int) = issueRepository.updateCreator(id)
}
