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

package dev.benica.infinite_longbox.fragments_view_models

import androidx.lifecycle.*
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.SortType
import dev.benica.infinite_longbox.database.models.*
import dev.benica.infinite_longbox.fragments.CharacterListFragment
import dev.benica.infinite_longbox.fragments.CreatorListFragment
import dev.benica.infinite_longbox.fragments.IssueListFragment
import dev.benica.infinite_longbox.fragments.SeriesListFragment
import dev.benica.infinite_longbox.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

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

    private val seriesOptions: LiveData<List<FilterModel>> =
        filter.switchMap { repository.getFilterOptionsSeries(it).asLiveData() }

    private val publisherOptions: LiveData<List<FilterModel>> =
        filter.switchMap { repository.getFilterOptionsPublisher(it).asLiveData() }

    private val creatorOptions: LiveData<List<FilterModel>> =
        filter.switchMap { repository.getFilterOptionsCreator(it).asLiveData() }

    private val characterOptions: LiveData<List<FilterModel>> =
        filter.switchMap { repository.getFilterOptionsCharacter(it).asLiveData() }

    private val allOptions: LiveData<List<FilterModel>> = combine(
        seriesOptions.asFlow(),
        creatorOptions.asFlow(),
        publisherOptions.asFlow(),
        characterOptions.asFlow()
    )
    {
            series: List<FilterModel>,
            creators: List<FilterModel>,
            publishers: List<FilterModel>,
            characters: List<FilterModel>,
        ->
        val res: List<FilterModel> = series + creators + publishers + characters
        res.sorted()
    }.asLiveData()

    var filterOptions: LiveData<List<FilterModel>> = filterType.switchMap {
        when (it) {
            Series.Companion::class -> seriesOptions
            Publisher.Companion::class -> publisherOptions
            Character.Companion::class -> characterOptions
            NameDetail.Companion::class -> creatorOptions
            All.Companion::class -> allOptions
            else -> throw IllegalStateException("filterOption can't be $it")
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

        override fun <S : Any?> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
            throw UnsupportedOperationException()
        }

        override fun <S : Any?> removeSource(toRemote: LiveData<S>) {
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

    fun myCollection(isChecked: Boolean) {
        val newVal = _filter.value?.let { SearchFilter(it) } ?: SearchFilter()
        newVal.mMyCollection = isChecked
        newVal.mShowVariants = isChecked
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

    val fragment =
        filter.switchMap {
            liveData {
                emit(
                    when (it.mViewOption) {
                        FullIssue::class -> issueListFragment
                        Character::class -> characterListFragment
                        FullSeries::class -> seriesListFragment
                        FullCreator::class -> creatorListFragment
                        else -> throw IllegalStateException("illegal viewOption: ${it.mViewOption}")
                    }
                )
            }
        }

    companion object {
        val seriesListFragment: SeriesListFragment
            get() = SeriesListFragment.newInstance()
        val issueListFragment: IssueListFragment
            get() = IssueListFragment.newInstance()
        val characterListFragment: CharacterListFragment
            get() = CharacterListFragment.newInstance()
        val creatorListFragment: CreatorListFragment
            get() = CreatorListFragment.newInstance()
    }
}
