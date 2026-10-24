/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.infinite_longbox.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.filter.SearchFilter
import dev.benica.infinite_longbox.filter.SortType
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.database.models.Creator
import dev.benica.infinite_longbox.database.models.FilterItem
import dev.benica.infinite_longbox.database.models.FilterModel
import dev.benica.infinite_longbox.database.models.FilterType
import dev.benica.infinite_longbox.database.models.NameDetail
import dev.benica.infinite_longbox.database.models.Publisher
import dev.benica.infinite_longbox.database.models.Series
import dev.benica.infinite_longbox.database.models.TextFilter
import dev.benica.infinite_longbox.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

@Suppress("unused")
private const val TAG = APP + "FilterViewModel"

@ExperimentalCoroutinesApi
class FilterViewModel : ViewModel() {

    private val repository: Repository = Repository.get()

    private val _filter = MutableLiveData(SearchFilter())
    val filter: LiveData<SearchFilter>
        get() = _filter

    private val _filterType: MutableLiveData<KClass<*>> =
        MutableLiveData()
    private val filterType: LiveData<KClass<*>>
        get() = _filterType

    /**
     * Options for the filter-box popup: the first [OPTIONS_LIMIT] matches for [text] among the
     * selected filter type, under the current filter. Queries the database per call instead of
     * holding whole tables (hundreds of thousands of rows) in memory.
     */
    suspend fun searchOptions(text: String): List<FilterModel> {
        val trimmed = text.trim()
        val searchFilter = SearchFilter(_filter.value ?: SearchFilter()).apply {
            mTextFilter = if (trimmed.isEmpty()) null else TextFilter(trimmed)
            // The filter row is hidden while typing, so My Collection can't be seen or undone from
            // here. Finding a series or creator shouldn't depend on already owning something.
            mMyCollection = false
        }

        return when (_filterType.value) {
            Series.Companion::class ->
                repository.getFilterOptionsSeriesLimited(searchFilter, OPTIONS_LIMIT)
            Publisher.Companion::class ->
                repository.getFilterOptionsPublisherLimited(searchFilter, OPTIONS_LIMIT)
            Character.Companion::class ->
                repository.getFilterOptionsCharacterLimited(searchFilter, OPTIONS_LIMIT)
            NameDetail.Companion::class ->
                repository.getFilterOptionsCreatorLimited(searchFilter, OPTIONS_LIMIT)
            else -> (
                repository.getFilterOptionsSeriesLimited(searchFilter, OPTIONS_LIMIT) +
                    repository.getFilterOptionsCreatorLimited(searchFilter, OPTIONS_LIMIT) +
                    repository.getFilterOptionsPublisherLimited(searchFilter, OPTIONS_LIMIT) +
                    repository.getFilterOptionsCharacterLimited(searchFilter, OPTIONS_LIMIT)
                ).sorted()
        }
    }

    private val _updateCompleteSeries = MutableLiveData(false)
    private val updateCompleteSeries: LiveData<Boolean> = _updateCompleteSeries
    private var _updateCompleteCharacter = MutableLiveData(false)
    private val updateCompleteCharacter: LiveData<Boolean> = _updateCompleteCharacter
    private var _updateCompleteCreator = MutableLiveData(false)
    private val updateCompleteCreator: LiveData<Boolean> = _updateCompleteCreator

    val updateComplete =
        CombinedLiveData(
            updateCompleteSeries,
            updateCompleteCharacter,
            updateCompleteCreator
        ) { d1, d2, d3 ->
            d1 == true && d2 == true && d3 == true }

    class CombinedLiveData<T, K, S, R>(
        sourceA: LiveData<T>, sourceB: LiveData<K>, sourceC: LiveData<S>, private val
        combine: (d1: T?, d2: K?, d3: S?) -> R
    ) : MediatorLiveData<R>() {
        private var data1: T? = null
        private var data2: K? = null
        private var data3: S? = null

        init {
            super.addSource(sourceA) {
                data1 = it
                value = combine(data1, data2, data3)
            }
            super.addSource(sourceB) {
                data2 = it
                value = combine(data1, data2, data3)
            }
            super.addSource(sourceC) {
                data3 = it
                value = combine(data1, data2, data3)
            }
        }

        override fun <S> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
            throw UnsupportedOperationException()
        }

        override fun <S> removeSource(toRemote: LiveData<S>) {
            throw UnsupportedOperationException()
        }
    }

    fun setFilter(filter: SearchFilter) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val seriesFilter = filter.mSeries
                if (seriesFilter != null) {
                    _updateCompleteSeries.postValue(false)
                    repository.updateSeriesAsync(seriesFilter.series).await().let {
                        _updateCompleteSeries.postValue(true)
                    }
                } else {
                    _updateCompleteSeries.postValue(true)
                }
            }.let {
                withContext(Dispatchers.Default) {
                    val characterFilter = filter.mCharacter
                    if (characterFilter != null) {
                        _updateCompleteCharacter.postValue(false)
                        repository.updateCharacterAsync(characterFilter).await().let {
                            _updateCompleteCharacter.postValue(true)
                        }
                    } else {
                        _updateCompleteCharacter.postValue(true)
                    }
                }.let {
                    if (filter.mCreators.isNotEmpty()) {
                        _updateCompleteCreator.postValue(false)
                        repository.updateCreatorsAsync(filter.mCreators.toList()).await().let {
                            _updateCompleteCreator.postValue(true)
                        }
                    } else {
                        _updateCompleteCreator.postValue(true)
                    }
                }
            }
        }
        _filter.value = SearchFilter(filter)
    }

    fun setFilterType(filterType: KClass<out FilterType>) {
        this._filterType.value = filterType
    }

    fun addFilterItem(item: FilterItem) {
        val newVal = _filter.value?.let { SearchFilter(it) } ?: SearchFilter()
        newVal.addFilter(item)
        when (item) {
            is Creator -> newVal.mShowVariants = true
            is NameDetail -> newVal.mShowVariants
            is Character -> newVal.mShowVariants = true
            else -> Unit
        }
        setFilter(newVal)
    }

    fun removeFilterItem(item: FilterItem) {
        val newVal = _filter.value?.let { SearchFilter(it) } ?: SearchFilter()
        newVal.removeFilter(item)
        setFilter(newVal)
    }

    fun setSortOption(sortType: SortType) {
        val newVal = _filter.value?.let { SearchFilter(it) } ?: SearchFilter()
        newVal.mSortType = sortType
        setFilter(newVal)
    }

    fun myCollection(isChecked: Boolean) = inCollection(BaseCollection.MY_COLL.id, isChecked)

    fun wishList(isChecked: Boolean) = inCollection(BaseCollection.WISH_LIST.id, isChecked)

    /** Shows only what is in [collectionId], or everything when unchecked. */
    private fun inCollection(collectionId: Int, isChecked: Boolean) {
        val newVal = _filter.value?.let { SearchFilter(it) } ?: SearchFilter()
        newVal.mMyCollection = isChecked
        if (isChecked) newVal.mCollectionId = collectionId
        newVal.mShowVariants = isChecked
        // What is missing is asked of one collection: another (or none) starts the question again
        newVal.mMissingCover = false
        newVal.mMissingStorage = false
        setFilter(newVal)
    }

    /** Only copies in the collection whose issue has no cover. */
    fun missingCover(isChecked: Boolean) {
        val newVal = _filter.value?.let { SearchFilter(it) } ?: SearchFilter()
        newVal.mMissingCover = isChecked
        setFilter(newVal)
    }

    /** Only copies in the collection that aren't filed in a storage place. */
    fun missingStorage(isChecked: Boolean) {
        val newVal = _filter.value?.let { SearchFilter(it) } ?: SearchFilter()
        newVal.mMissingStorage = isChecked
        setFilter(newVal)
    }

    fun showVariants(isChecked: Boolean) {
        val newVal = _filter.value?.let { SearchFilter(it) } ?: SearchFilter()
        newVal.mShowVariants = isChecked
        setFilter(newVal)
    }

    fun nextViewOption() {
        val newVal = _filter.value?.let { SearchFilter(it) } ?: SearchFilter()
        newVal.nextViewOption()
        setFilter(newVal)
    }

    companion object {
        /** Max rows per type shown in the filter-box popup. */
        const val OPTIONS_LIMIT = 50

    }
}
