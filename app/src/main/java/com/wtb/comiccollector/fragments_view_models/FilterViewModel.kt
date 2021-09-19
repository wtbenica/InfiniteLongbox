package com.wtb.comiccollector.fragments_view_models

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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlin.reflect.KClass

private const val TAG = APP + "FilterViewModel"

@ExperimentalCoroutinesApi
class FilterViewModel : ViewModel() {

    private val repository: Repository = Repository.get()

    private val _filter = MutableLiveData(SearchFilter())
    val filter: LiveData<SearchFilter>
        get() = _filter

    private val _filterType: MutableLiveData<KClass<*>> =
        MutableLiveData(All.Companion::class as KClass<*>)
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
            Series.Companion::class     -> seriesOptions
            Publisher.Companion::class  -> publisherOptions
            Character.Companion::class  -> characterOptions
            NameDetail.Companion::class -> creatorOptions
            All.Companion::class        -> allOptions
            else                        -> throw IllegalStateException("filterOption can't be $it")
        }
    }

    fun setFilter(filter: SearchFilter) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                filter.mSeries?.let {
                    repository.updateSeries(it.series.seriesId)
                }
            }.let {
                withContext(Dispatchers.Default) {
                    filter.mCharacter?.let {
                        repository.updateCharacter(it.characterId)
                    }
                }.let {
                    if (filter.mCreators.isNotEmpty()) {
                        repository.updateCreators(filter.mCreators.ids)
                    }
                }
            }
        }
        _filter.value = SearchFilter(filter)
    }

    fun setFilterType(filterType: KClass<*>) {
        this._filterType.value = filterType
    }

    fun addFilterItem(item: FilterItem) {
        val newVal = _filter.value?.let { SearchFilter(it) } ?: SearchFilter()
        newVal.addFilter(item)
        when (item) {
            is Creator -> newVal.mShowVariants = true
            is NameDetail -> newVal.mShowVariants
            is Character -> newVal.mShowVariants = true
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

    fun showIssues(show: Boolean) {
        val newVal = _filter.value?.let { SearchFilter(it) } ?: SearchFilter()
        newVal.mShowIssues = show
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
                emit(when (it.mViewOption) {
                         FullIssue::class            -> issueListFragment
                         Character::class            -> characterListFragment
                         FullSeries::class           -> seriesListFragment
                         NameDetailAndCreator::class -> creatorListFragment
                         else                        -> throw IllegalStateException("illegal viewOption: ${it.mViewOption}")
                     })
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
