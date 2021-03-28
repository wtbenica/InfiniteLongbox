package com.wtb.comiccollector.GroupListViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Series

private const val TAG = APP + "SeriesListViewModel"

class SeriesListViewModel : GroupListViewModel<Series>() {

    override val objectListLiveData: LiveData<List<Series>> =
        Transformations.switchMap(filterIdLiveData) { creatorId ->
            if (creatorId == null) {
                issueRepository.allSeries
            } else {
                issueRepository.getSeriesByCreator(creatorId)
            }
        }
}
