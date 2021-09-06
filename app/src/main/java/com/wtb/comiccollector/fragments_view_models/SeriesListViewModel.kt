package com.wtb.comiccollector.fragments_view_models

import android.util.Log
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.FullSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
class SeriesListViewModel : ListViewModel<FullSeries>() {

    override val itemList: Flow<PagingData<FullSeries>> = filter.switchMap {
        Log.d(TAG, "GOING TO GET SOME MORE SERIES FOR THIS NEW FILTER: $it")
        repository.getSeriesByFilterPaged(it).asLiveData()
    }.asFlow().cachedIn(viewModelScope)

    fun getIssue(issueId: Int) = repository.getIssue(issueId)

    companion object {
        private const val TAG = APP + "SeriesListViewModel"
    }
}
