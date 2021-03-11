package com.wtb.comiccollector

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations

private const val TAG = "CreatorListViewModel"

class CreatorListViewModel : GroupListViewModel<Creator>() {

    override val objectListLiveData: LiveData<List<Creator>> =
        Transformations.switchMap(filterIdLiveData) { seriesId ->
            if (seriesId == null) {
                issueRepository.allCreators
            } else {
                issueRepository.getCreatorBySeries(seriesId)
            }
        }
}

