package com.wtb.comiccollector.GroupListViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.NewIssueRepository
import com.wtb.comiccollector.Series
import java.time.LocalDate

private const val TAG = APP + "SeriesListViewModel"

class SeriesListViewModel : ViewModel(), Filter.FilterObserver {
    val issueRepository: NewIssueRepository = NewIssueRepository.get()
    val filterLiveData = MutableLiveData<Filter?>(null)

    val seriesListLiveData: LiveData<List<Series>> =
        Transformations.switchMap(filterLiveData) { filter ->
            filter?.let { issueRepository.newGetSeriesByFilter(it) }
        }

    fun filter(
        creatorIdsFilter: MutableSet<Int>?,
        publisherIdsFilter: MutableSet<Int>?,
        dateFilterStart: LocalDate?,
        dateFilterEnd: LocalDate?
    ) {
        filterLiveData.value = Filter(
            this,
            creators = creatorIdsFilter,
            publishers = publisherIdsFilter,
            startDate = dateFilterStart,
            endDate = dateFilterEnd
        )
    }

    override fun onUpdate() {
        // Do nothing
    }

}
