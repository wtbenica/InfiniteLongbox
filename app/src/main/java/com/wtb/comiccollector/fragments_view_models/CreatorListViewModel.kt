package com.wtb.comiccollector.fragments_view_models

import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.FullCreator
import com.wtb.comiccollector.database.models.FullSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "SeriesListViewModel"

@ExperimentalCoroutinesApi
class CreatorListViewModel : ListViewModel<FullSeries>() {

    val creatorList: Flow<PagingData<FullCreator>> = filter.switchMap {
        repository.getCreatorsByFilterPaged(it).asLiveData()
    }.asFlow().cachedIn(viewModelScope)
}
