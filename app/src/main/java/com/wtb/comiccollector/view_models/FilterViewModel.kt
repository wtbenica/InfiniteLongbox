package com.wtb.comiccollector.view_models

import android.util.Log
import androidx.lifecycle.ViewModel
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.SortOption
import com.wtb.comiccollector.database.models.FilterOption
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

private const val TAG = APP + "FilterViewModel"

@ExperimentalCoroutinesApi
class FilterViewModel : ViewModel() {

    private val repository: Repository = Repository.get()

    private val _filter = MutableStateFlow(SearchFilter())

    val filter: StateFlow<SearchFilter> = _filter

    val seriesOptions: Flow<List<FilterOption>> = _filter.flatMapLatest {
        repository.getSeriesByFilter(it)
    }

    val publisherOptions = _filter.flatMapLatest {
        repository.getPublishersByFilter(it)
    }

    val creatorOptions = _filter.flatMapLatest {
        repository.getCreatorsByFilter(it)
    }

    val filterOptions: Flow<List<FilterOption>> = combine(
        seriesOptions,
        creatorOptions,
        publisherOptions
    )
    { series: List<FilterOption>, creators: List<FilterOption>, publishers: List<FilterOption> ->
        Log.d(TAG, "filterOptions: ${series.size} series; ${creators.size} creators; ${publishers
            .size} publishers;")
        val res: List<FilterOption> = series + creators + publishers
        res.sorted()
    }

    fun setFilter(filter: SearchFilter) {
        this._filter.value = filter
    }

    fun addFilterItem(item: FilterOption) {
        Log.d(TAG, "ADDING ITEM: $item")
        val newVal = SearchFilter(_filter.value)
        newVal.addFilter(item)
        _filter.value = newVal
    }

    fun removeFilterItem(item: FilterOption) {
        val newVal = SearchFilter(_filter.value)
        newVal.removeFilter(item)
        _filter.value = newVal
    }

    fun setSortOption(sortOption: SortOption) {
        val newVal = SearchFilter(_filter.value)
        newVal.mSortOption = sortOption
        _filter.value = newVal
    }

    fun myCollection(isChecked: Boolean) {
        val newVal = SearchFilter(_filter.value)
        newVal.mMyCollection = isChecked
        _filter.value = newVal
    }
}