package com.wtb.comiccollector.fragments_view_models

import android.util.Log
import androidx.lifecycle.ViewModel
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.SortType
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

private const val TAG = APP + "FilterViewModel"

@ExperimentalCoroutinesApi
class FilterViewModel : ViewModel() {

    private val repository: Repository = Repository.get()

    private val theOneTrueFilter: MutableStateFlow<SearchFilter> = MutableStateFlow(SearchFilter())
    private val filterTypeSpinnerOption: MutableStateFlow<FilterTypeSpinnerOption> =
        MutableStateFlow(All.Companion::class.objectInstance as FilterTypeSpinnerOption)

    val filter: StateFlow<SearchFilter> = theOneTrueFilter

    private val seriesOptions: Flow<List<FilterAutoCompleteType>> =
        filter.flatMapLatest {
            repository.getFilterOptionsSeries(it)
        }

    private val publisherOptions = filter.flatMapLatest {
        repository.getFilterOptionsPublisher(it)
    }

    private val creatorOptions = filter.flatMapLatest {
        repository.getFilterOptionsCreator(it)
    }

    private val characterOptions = filter.flatMapLatest {
        repository.getFilterOptionsCharacter(it)
    }

    private val allOptions: Flow<List<FilterAutoCompleteType>> = combine(
        seriesOptions,
        creatorOptions,
        publisherOptions,
        characterOptions
    )
    { series: List<FilterAutoCompleteType>, creators: List<FilterAutoCompleteType>, publishers:
    List<FilterAutoCompleteType>, characters: List<FilterAutoCompleteType> ->
        val res: List<FilterAutoCompleteType> = series + creators + publishers + characters
        Log.d(TAG, "filterOptions: ${res.size}")
        res.sorted()
    }

    val filterOptions = filterTypeSpinnerOption.flatMapLatest {
        when (it) {
            Series     -> seriesOptions
            Publisher  -> publisherOptions
            Character  -> characterOptions
            NameDetail -> creatorOptions
            All        -> allOptions
        }
    }

    fun setFilter(filter: SearchFilter) {
        theOneTrueFilter.value = filter
    }

    fun setFilterOptionType(filterTypeSpinnerOption: FilterTypeSpinnerOption) {
        this.filterTypeSpinnerOption.value = filterTypeSpinnerOption
    }

    fun addFilterItem(item: FilterType) {
        Log.d(TAG, "ADDING ITEM: $item")
        val newVal = SearchFilter(theOneTrueFilter.value)
        newVal.addFilter(item)
        setFilter(newVal)
    }

    fun removeFilterItem(item: FilterType) {
        val newVal = SearchFilter(theOneTrueFilter.value)
        newVal.removeFilter(item)
        setFilter(newVal)
    }

    fun setSortOption(sortType: SortType) {
        Log.d(TAG, "setSortOption: ${sortType.sortString} ${sortType.order}")
        val newVal = SearchFilter(theOneTrueFilter.value)
        newVal.mSortType = sortType
        setFilter(newVal)
    }

    fun myCollection(isChecked: Boolean) {
        val newVal = SearchFilter(theOneTrueFilter.value)
        newVal.mMyCollection = isChecked
        setFilter(newVal)
    }

    fun showVariants(isChecked: Boolean) {
        val newVal = SearchFilter(theOneTrueFilter.value)
        newVal.mShowVariants = isChecked
        setFilter(newVal)
    }

    fun showIssues(show: Boolean) {
        val newVal = SearchFilter(theOneTrueFilter.value)
        newVal.mShowIssues = show
        setFilter(newVal)
    }

    fun nextView() {
        val newVal = SearchFilter(theOneTrueFilter.value)
        newVal.nextOption()
        setFilter(newVal)
    }
}