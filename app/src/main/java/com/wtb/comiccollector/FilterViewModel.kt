package com.wtb.comiccollector

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.wtb.comiccollector.database.models.FilterOption
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest

private const val TAG = APP + "FilterViewModel"

@ExperimentalCoroutinesApi
class FilterViewModel : ViewModel() {

    private val repository: Repository = Repository.get()

    private val _filter = MutableStateFlow(Filter())
    
    val filter: StateFlow<Filter> = _filter
    
    val filterOptions: LiveData<List<FilterOption>> = _filter.flatMapLatest {
        repository.getValidFilterOptions(it)
    }.asLiveData()
    

    fun addFilterItem(item: FilterOption) {
        Log.d(TAG, "ADDING FILTER")
        val newVal = Filter(_filter.value)
        newVal.addFilter(item)
        _filter.value = newVal
    }

    fun removeFilterItem(item: FilterOption) {
        Log.d(TAG, "REMOVING FILTER")
        val newVal = Filter(_filter.value)
        newVal.removeFilter(item)
        _filter.value = newVal
    }

    fun setSortOption(sortOption: SortOption) {
        Log.d(TAG, "ADDING SORT OPTION")
        val newVal = Filter(_filter.value)
        newVal.mSortOption = sortOption
        _filter.value = newVal
    }

    fun myCollection(isChecked: Boolean) {
        Log.d(TAG, "SETTING MY COLLECTION")
        val newVal = Filter(_filter.value)
        newVal.setMyCollection(isChecked)
        _filter.value = newVal
    }
}