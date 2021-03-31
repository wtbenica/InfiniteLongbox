package com.wtb.comiccollector.GroupListViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Creator
import com.wtb.comiccollector.Series

private const val TAG = APP + "CreatorListViewModel"

class CreatorListViewModel : GroupListViewModel<Creator, Series>() {

    override val objectListLiveData: LiveData<List<Creator>> =
        Transformations.switchMap(filterLiveData) { filter ->
            if (filter == null || filter.isEmpty()) {
                issueRepository.allCreators
            } else {
                issueRepository.getCreatorsByFilter(filter)
            }
        }

    override fun filterUpdate(id: Int) = issueRepository.updateIssuesBySeries(id)
}

