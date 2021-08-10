package com.wtb.comiccollector.fragments_view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.ListItem
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

// TODO: Not sure what the point of giving this a param type. it isn't used.
@ExperimentalCoroutinesApi
abstract class ListViewModel<T: ListItem> : ViewModel() {
    protected val repository: Repository = Repository.get()
    protected val filterLiveData = MutableLiveData(SearchFilter())
    abstract val itemList: Flow<PagingData<T>>
    val filter: LiveData<SearchFilter> = filterLiveData

    open fun setFilter(filter: SearchFilter) {
        filter.getSortOptions()
        filterLiveData.value = filter
    }
}