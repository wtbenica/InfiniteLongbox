package com.wtb.comiccollector.fragments_view_models

import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.database.models.ListItem
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

// TODO: Not sure what the point of giving this a param type. it isn't used.
@ExperimentalCoroutinesApi
abstract class ListViewModel<T: ListItem> : ViewModel() {
    protected val repository: Repository = Repository.get()
    protected val filterLiveData = MutableLiveData(SearchFilter())
    val filter: LiveData<SearchFilter> = filterLiveData

    val itemList: Flow<PagingData<FullSeries>> = filter.switchMap { filter ->
        filter.let {
            repository.getSeriesByFilterPaged(it).asLiveData()
        }
    }.asFlow().cachedIn(viewModelScope)

    open fun setFilter(filter: SearchFilter) {
        filter.getSortOptions()
        filterLiveData.value = filter
    }
}