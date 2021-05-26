package com.wtb.comiccollector

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.wtb.comiccollector.database.models.FilterOption
import com.wtb.comiccollector.repository.Repository

private const val TAG = APP + "SearchViewModel"

class SearchViewModel : ViewModel() {

    private val repository: Repository = Repository.get()

    var filterLiveData = MutableLiveData(Filter())

    val filterOptionsLiveData = Transformations.switchMap(filterLiveData) {
        repository.getValidFilterOptions(it)
    }

    fun addFilterItem(item: FilterOption) {
        Log.d(TAG, "addItem $item")
        val newVal = filterLiveData.value
        newVal?.addFilter(item)
        filterLiveData.value = newVal
    }

    fun removeFilterItem(item: FilterOption) {
        Log.d(TAG, "removeItem $item")
        val newVal = filterLiveData.value
        newVal?.removeFilter(item)
        filterLiveData.value = newVal
    }

    fun setSortOption(sortOption: SortOption) {
        val newVal = filterLiveData.value
        newVal?.mSortOption = sortOption
        filterLiveData.value = newVal
    }

    fun myCollection(isChecked: Boolean) {
        val newVal = filterLiveData.value
        newVal?.setMyCollection(isChecked)
        filterLiveData.value = newVal
    }
}