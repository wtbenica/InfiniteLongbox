package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations

private const val TAG = "SeriesListViewModel"

class SeriesListViewModel : GroupListViewModel<Series>() {

    override val objectListLiveData: LiveData<List<Series>> =
        Transformations.switchMap(filterIdLiveData) { creatorId ->
            if (creatorId == null) {
                issueRepository.getSeriesList()
            } else {
                issueRepository.getSeriesByCreator(creatorId)
            }
        }
}
