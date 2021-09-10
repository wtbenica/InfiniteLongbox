package com.wtb.comiccollector.fragments_view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.ListItem
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
abstract class ListViewModel<T : ListItem> : ViewModel() {
    protected val repository: Repository = Repository.get()
    abstract val itemList: Flow<PagingData<T>>

    private val _filter = MutableLiveData(SearchFilter())
    val filter: LiveData<SearchFilter>
        get() = _filter

    open fun setFilter(filter: SearchFilter) {
        _filter.value = filter
    }

    companion object {
        private const val TAG = APP + "ListViewModel"
    }
}