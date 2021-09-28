package com.wtb.comiccollector.fragments_view_models

import android.util.Log
import androidx.lifecycle.*
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.SortType
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.fragments.CharacterListFragment
import com.wtb.comiccollector.fragments.CreatorListFragment
import com.wtb.comiccollector.fragments.IssueListFragment
import com.wtb.comiccollector.fragments.SeriesListFragment
import com.wtb.comiccollector.repository.Repository
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
        Log.d(TAG, "Switching filters to: $it")
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
    val updateCompleteSeries: LiveData<Boolean> = _updateCompleteSeries
    private var _updateCompleteCharacter = MutableLiveData(false)
    val updateCompleteCharacter: LiveData<Boolean> = _updateCompleteCharacter
    private var _updateCompleteCreator = MutableLiveData(false)
    val updateCompleteCreator: LiveData<Boolean> = _updateCompleteCreator

    val updateComplete =
        CombinedLiveData(
            updateCompleteSeries,
            updateCompleteCharacter,
            updateCompleteCreator
        ) { d1, d2, d3 ->
            Log.d("$${APP}UPDATES_COMPLETE", "d1: $d1 d2: $d2 d3: $d3")
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
                    repository.updateSeriesAsync(seriesFilter.series.seriesId).await().let {
                        _updateCompleteSeries.postValue(true)
                        Log.d(TAG, "SERIES FILTER UPDATE COMPLETE")
                    }
                } else {
                    _updateCompleteSeries.postValue(true)
                    Log.d(TAG, "SERIES FILTER UPDATE NOT REQUIRED")
                }
            }.let {
                withContext(Dispatchers.Default) {
                    val characterFilter = filter.mCharacter
                    if (characterFilter != null) {
                        _updateCompleteCharacter.postValue(false)
                        repository.updateCharacterAsync(characterFilter.characterId).await().let {
                            _updateCompleteCharacter.postValue(true)
                            Log.d(TAG, "CHARACTER FILTER UPDATE COMPLETE")
                        }
                    } else {
                        _updateCompleteCharacter.postValue(true)
                        Log.d(TAG, "CHARACTER FILTER UPDATE NOT REQUIRED")
                    }
                }.let {
                    if (filter.mCreators.isNotEmpty()) {
                        _updateCompleteCreator.postValue(false)
                        repository.updateCreatorsAsync(filter.mCreators.ids).await().let {
                            _updateCompleteCreator.postValue(true)
                            Log.d(TAG, "CREATOR FILTER UPDATE COMPLETE")
                        }
                    } else {
                        _updateCompleteCreator.postValue(true)
                        Log.d(TAG, "CREATOR FILTER UPDATE NOT REQUIRED")
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
