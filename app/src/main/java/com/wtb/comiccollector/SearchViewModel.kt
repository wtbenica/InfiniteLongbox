package com.wtb.comiccollector

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wtb.comiccollector.database.models.Filterable

private const val TAG = APP + "SearchViewModel"

class SearchViewModel : ViewModel() {

    private val issueRepository: IssueRepository = IssueRepository.get()

    val filterOptionsLiveData = issueRepository.everything

    var filterLiveData = MutableLiveData(Filter())

    fun addItem(item: Filterable) {
        Log.d(TAG, "addItem $item")
        val newVal = filterLiveData.value
        newVal?.addItem(item)
        filterLiveData.value = newVal
//        filterLiveData.value?.addItem(item)
    }

    fun removeItem(item: Filterable) {
        Log.d(TAG, "removeItem $item")
        val newVal = filterLiveData.value
        newVal?.removeItem(item)
        filterLiveData.value = newVal
//        filterLiveData.value?.removeItem(item)
    }

    fun myCollection(isChecked: Boolean) {
        filterLiveData.value?.setMyCollection(isChecked)
    }
}