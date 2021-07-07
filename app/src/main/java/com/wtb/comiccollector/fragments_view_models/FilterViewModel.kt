package com.wtb.comiccollector.fragments_view_models

import android.util.Log
import androidx.lifecycle.ViewModel
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.SortType
import com.wtb.comiccollector.database.models.FilterOptionAutoCompletePopupItem
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

private const val TAG = APP + "FilterViewModel"

@ExperimentalCoroutinesApi
class FilterViewModel : ViewModel() {

    private val repository: Repository = Repository.get()

    private val theOneTrueFilter = MutableStateFlow(SearchFilter())

    val filter: StateFlow<SearchFilter> = theOneTrueFilter

    private val seriesOptions: Flow<List<FilterOptionAutoCompletePopupItem>> = filter.flatMapLatest {
        repository.getSeriesByFilter(it)
    }

    private val publisherOptions = filter.flatMapLatest {
        repository.getPublishersByFilter(it)
    }

    private val creatorOptions = filter.flatMapLatest {
        repository.getCreatorsByFilter(it)
    }

    val filterOptions: Flow<List<FilterOptionAutoCompletePopupItem>> = combine(
        seriesOptions,
        creatorOptions,
        publisherOptions
    )
    { series: List<FilterOptionAutoCompletePopupItem>, creators: List<FilterOptionAutoCompletePopupItem>, publishers: List<FilterOptionAutoCompletePopupItem> ->
        Log.d(TAG, "filterOptions: ${series.size} series; ${creators.size} creators; ${publishers
            .size} publishers;")
        val res: List<FilterOptionAutoCompletePopupItem> = series + creators + publishers
        Log.d(TAG, "filterOptions: ${res.size}")
        res.sorted()
    }

    fun setFilter(filter: SearchFilter) {
        this.theOneTrueFilter.value = filter
    }

    fun addFilterItem(item: FilterOptionAutoCompletePopupItem) {
        Log.d(TAG, "ADDING ITEM: $item")
        val newVal = SearchFilter(theOneTrueFilter.value)
        newVal.addFilter(item)
        theOneTrueFilter.value = newVal
    }

    fun removeFilterItem(item: FilterOptionAutoCompletePopupItem) {
        val newVal = SearchFilter(theOneTrueFilter.value)
        newVal.removeFilter(item)
        theOneTrueFilter.value = newVal
    }

    fun setSortOption(sortType: SortType) {
        Log.d(TAG, "setSortOption: ${sortType.sortString} ${sortType.order}")
        val newVal = SearchFilter(theOneTrueFilter.value)
        newVal.mSortType = sortType
        theOneTrueFilter.value = newVal
    }

    fun myCollection(isChecked: Boolean) {
        val newVal = SearchFilter(theOneTrueFilter.value)
        newVal.mMyCollection = isChecked
        theOneTrueFilter.value = newVal
    }

    fun showVariants(isChecked: Boolean) {
        val newVal = SearchFilter(theOneTrueFilter.value)
        newVal.mShowVariants = isChecked
        theOneTrueFilter.value = newVal
    }

    fun showIssues(show: Boolean) {
        val newVal = SearchFilter(theOneTrueFilter.value)
        newVal.mShowIssues = show
        theOneTrueFilter.value = newVal
    }
}