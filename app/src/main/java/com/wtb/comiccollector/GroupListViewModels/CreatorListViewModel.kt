package com.wtb.comiccollector.GroupListViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.wtb.comiccollector.Creator

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

