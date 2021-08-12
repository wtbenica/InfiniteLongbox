package com.wtb.comiccollector.fragments_view_models

import android.util.Log
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.SortType
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.fragments.*
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass

private const val TAG = APP + "FilterViewModel"

@ExperimentalCoroutinesApi
class FilterViewModel : ViewModel() {

    private val repository: Repository = Repository.get()

    private val _filter = MutableStateFlow(SearchFilter())

    private val _filterType: MutableLiveData<KClass<*>> =
        MutableLiveData(All.Companion::class as KClass<*>)

    val filter: StateFlow<SearchFilter> = _filter
    val filterType: LiveData<KClass<*>> = _filterType

    private val seriesOptions: Flow<List<FilterModel>> =
        filter.flatMapLatest { repository.getFilterOptionsSeries(it) }

    private val publisherOptions =
        filter.flatMapLatest { repository.getFilterOptionsPublisher(it) }

    private val creatorOptions =
        filter.flatMapLatest { repository.getFilterOptionsCreator(it) }

    private val characterOptions =
        filter.flatMapLatest { repository.getFilterOptionsCharacter(it) }

    private val allOptions: Flow<List<FilterModel>> = combine(
        seriesOptions,
        creatorOptions,
        publisherOptions,
        characterOptions
    )
    {
            series: List<FilterModel>,
            creators: List<FilterModel>,
            publishers: List<FilterModel>,
            characters: List<FilterModel>,
        ->
        val res: List<FilterModel> = series + creators + publishers + characters
        Log.d(TAG, "filterOptions: ${res.size}")
        res.sorted()
    }

    var filterOptions: LiveData<List<FilterModel>> = filterType.switchMap {
        when (it) {
            Series.Companion::class     -> seriesOptions
            Publisher.Companion::class  -> publisherOptions
            Character.Companion::class  -> characterOptions
            NameDetail.Companion::class -> creatorOptions
            All.Companion::class        -> allOptions
            else                        -> throw IllegalStateException("filterOption can't be $it")
        }.asLiveData()
    }

    fun setFilter(filter: SearchFilter) {
        filter.mCharacter?.let {
            repository.updateCharacter(it.characterId)
        }
        filter.mSeries?.let {
            repository.updateSeries(it.series.seriesId)
        }
        if (filter.mCreators.isNotEmpty()) {
            repository.updateCreators(filter.mCreators.ids)
        }
        _filter.value = SearchFilter(filter)
    }

    fun setFilterType(filterType: KClass<*>) {
        Log.d(TAG, "FiltER TYpe IS : %$#$#@$${filterType.simpleName}")
        val d = Log.d(TAG, when (filterType) {
            Series.Companion::class     -> "Petunia"
            Character.Companion::class  -> "Shemale"
            Publisher.Companion::class  -> "Vanquish"
            NameDetail.Companion::class -> "Lantern"
            All.Companion::class        -> "Alabama"
            else                        -> "bricabrac"
        })
        this._filterType.value = filterType
    }

    fun addFilterItem(item: FilterItem) {
        Log.d(TAG, "ADDING ITEM: $item")
        val newVal = SearchFilter(_filter.value)
        newVal.addFilter(item)
        setFilter(newVal)
    }

    fun removeFilterItem(item: FilterItem) {
        val newVal = SearchFilter(_filter.value)
        newVal.removeFilter(item)
        setFilter(newVal)
    }

    fun setSortOption(sortType: SortType) {
        Log.d(TAG, "setSortOption: ${sortType.sortString} ${sortType.order}")
        val newVal = SearchFilter(_filter.value)
        newVal.mSortType = sortType
        setFilter(newVal)
    }

    fun myCollection(isChecked: Boolean) {
        val newVal = SearchFilter(_filter.value)
        newVal.mMyCollection = isChecked
        setFilter(newVal)
    }

    fun showVariants(isChecked: Boolean) {
        val newVal = SearchFilter(_filter.value)
        newVal.mShowVariants = isChecked
        setFilter(newVal)
    }

    fun showIssues(show: Boolean) {
        val newVal = SearchFilter(_filter.value)
        newVal.mShowIssues = show
        setFilter(newVal)
    }

    fun nextView() {
        val newVal = SearchFilter(_filter.value)
        newVal.nextOption()
        setFilter(newVal)
    }

    val fragment: Flow<ListFragment<out ListItem, out RecyclerView.ViewHolder>?> =
        filter.mapLatest {
            when (it.mViewOption) {
                FullIssue::class            -> issueListFragment
                Character::class            -> characterListFragment
                FullSeries::class           -> seriesListFragment
                NameDetailAndCreator::class -> creatorListFragment
                else                        -> throw IllegalStateException("illegal viewOption: ${it.mViewOption}")
            }
        }

    companion object {
        private var _seriesListFragment: SeriesListFragment? = null
        private var _issueListFragment: IssueListFragment? = null
        private var _characterListFragment: CharacterListFragment? = null
        private var _creatorListFragment: CreatorListFragment? = null

        private val seriesListFragment: SeriesListFragment
            get() {
                val result = _seriesListFragment ?: SeriesListFragment.newInstance()
                if (_seriesListFragment == null) {
                    _seriesListFragment = result
                }
                return result
            }
        private val issueListFragment: IssueListFragment
            get() {
                val result = _issueListFragment ?: IssueListFragment.newInstance()
                if (_issueListFragment == null) {
                    _issueListFragment = result
                }
                return result
            }
        private val characterListFragment: CharacterListFragment
            get() {
                val result = _characterListFragment ?: CharacterListFragment.newInstance()
                if (_characterListFragment == null) {
                    _characterListFragment = result
                }
                return result
            }
        private val creatorListFragment: CreatorListFragment
            get() {
                val result = _creatorListFragment ?: CreatorListFragment.newInstance()
                if (_creatorListFragment == null) {
                    _creatorListFragment = result
                }
                return result
            }
    }
}
