package com.wtb.comiccollector

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.wtb.comiccollector.database.models.FilterOption

private const val TAG = APP + "SearchViewModel"

class SearchViewModel : ViewModel() {

    private val issueRepository: IssueRepository = IssueRepository.get()

    var filterLiveData = MutableLiveData(Filter())

    val filterOptionsLiveData = Transformations.switchMap(filterLiveData) {
        issueRepository.filterOptions(it)
    }

    fun addItem(item: FilterOption) {
        Log.d(TAG, "addItem $item")
        val newVal = filterLiveData.value
        newVal?.addItem(item)
        filterLiveData.value = newVal
    }

    fun removeItem(item: FilterOption) {
        Log.d(TAG, "removeItem $item")
        val newVal = filterLiveData.value
        newVal?.removeItem(item)
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