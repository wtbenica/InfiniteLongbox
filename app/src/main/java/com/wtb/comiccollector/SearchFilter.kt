package com.wtb.comiccollector

import androidx.fragment.app.Fragment
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FilterOption
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.fragments.IssueListFragment
import com.wtb.comiccollector.fragments.SeriesListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.Serializable
import java.time.LocalDate

const val ARG_FILTER = "Filter"

private const val TAG = APP + "Filter_SortChipGroup"

val SERIES_SORT_OPTIONS: List<SortOption> = listOf(
    SeriesSortOption("Series Name (A-Z)", "sortName ASC"),
    SeriesSortOption("Series Name (Z-A)", "sortName DESC"),
    SeriesSortOption("Date (Oldest to Newest)", "startDate ASC"),
    SeriesSortOption("Date (Newest to Oldest)", "startDate DESC")
)

val ISSUE_SORT_OPTIONS: List<SortOption> = listOf(
    IssueSortOption("Issue Number (Low to High)", "issueNum ASC"),
    IssueSortOption("Issue Number (High to Low)", "issueNum DESC"),
    IssueSortOption("Date (Oldest to Newest)", "releaseDate ASC"),
    IssueSortOption("Date (Newest to Oldest)", "releaseDate DESC")
)

@ExperimentalCoroutinesApi
class SearchFilter(
    creators: Set<Creator>? = null,
    series: Series? = null,
    publishers: Set<Publisher>? = null,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
    myCollection: Boolean = true,
    sortOption: SortOption? = null,
    textFilter: TextFilter? = null,
    showVariants: Boolean = false
) : Serializable {

    constructor(filter: SearchFilter) : this(
        filter.mCreators,
        filter.mSeries,
        filter.mPublishers,
        filter.mStartDate,
        filter.mEndDate,
        filter.mMyCollection,
        filter.mSortOption,
        filter.mTextFilter,
        filter.mShowVariants
    )

    override fun equals(other: Any?): Boolean {
        return other is SearchFilter && hashCode() == other.hashCode()
    }

    var mCreators: Set<Creator> = creators ?: setOf()
    var mSeries: Series? = series
        set(value) {
            // sets selected sort option to default if it's not of the same type
            if (getSortOptions(value) != getSortOptions()) {
                mSortOption = getSortOptions(value)[0]
            }
            field = value
        }
    var mPublishers: Set<Publisher> = publishers ?: setOf()
    var mStartDate: LocalDate = startDate ?: LocalDate.MIN
    var mEndDate: LocalDate = endDate ?: LocalDate.MAX
    var mMyCollection: Boolean = myCollection
    var mSortOption: SortOption = sortOption ?: getSortOptions()[0]
    var mTextFilter: TextFilter? = textFilter
    var mShowVariants: Boolean = showVariants

    fun hasCreator() = mCreators.isNotEmpty()
    fun returnsIssueList() = mSeries != null
    fun hasPublisher() = mPublishers.isNotEmpty()
    fun hasDateFilter() = mStartDate != LocalDate.MIN || mEndDate != LocalDate.MAX
    fun isEmpty(): Boolean {
        return mCreators.isEmpty() && mSeries == null && mPublishers.isEmpty() && mStartDate ==
                LocalDate.MIN && mEndDate == LocalDate.MAX && !mMyCollection
    }

    fun addFilter(vararg items: FilterOption) {
        items.forEach { item ->
            when (item) {
                is Series     -> addSeries(item)
                is Creator    -> addCreator(item)
                is Publisher  -> addPublisher(item)
                is TextFilter -> addTextFilter(item)
            }
        }
    }

    private fun addCreator(vararg creator: Creator) {
        val newCreators = mCreators + creator.toSet()
        mCreators = newCreators
    }

    private fun addSeries(series: Series) {
        this.mSeries = series
    }

    private fun addPublisher(vararg publisher: Publisher) {
        val newPublishers = mPublishers + publisher.toSet()
        mPublishers = newPublishers
    }

    private fun addTextFilter(item: TextFilter) {
        mTextFilter = item
    }

    fun removeFilter(vararg items: FilterOption) {
        items.forEach { item ->
            when (item) {
                is Series     -> removeSeries()
                is Creator    -> removeCreator(item)
                is Publisher  -> removePublisher(item)
                is TextFilter -> removeTextFilter(item)
            }
        }
    }

    private fun removeCreator(vararg creator: Creator) {
        val newCreators = mCreators - creator.toSet()
        mCreators = newCreators
    }

    private fun removeSeries() {
        this.mSeries = null
    }

    private fun removePublisher(vararg publisher: Publisher) {
        val newPublishers = mPublishers - publisher.toSet()
        mPublishers = newPublishers
    }

    private fun removeTextFilter(item: TextFilter) {
        if (item == mTextFilter)
            mTextFilter = null
    }

    fun getFragment(): Fragment =
        when (mSeries) {
            null -> SeriesListFragment.newInstance()
            else -> IssueListFragment.newInstance()
        }

    fun getSortOptions(series: Series? = mSeries): List<SortOption> =
        when (series) {
            null -> SERIES_SORT_OPTIONS
            else -> ISSUE_SORT_OPTIONS
        }

    fun getReturnTypes(): List<ReturnType> {
        val result: MutableList<ReturnType> = mutableListOf()

        if (mSeries == null) {
            result.add(ReturnType.SERIES)
        }

        if (mCreators.isNotEmpty() || mPublishers.isNotEmpty()||mSeries!=null) {
            result.add(ReturnType.ISSUE)
        }

        return result
    }

    fun getAll(): Set<FilterOption> {
        val series = mSeries?.let { setOf(it) } ?: emptySet()
        val textFilter = mTextFilter?.let { setOf(it) } ?: emptySet()
        return mCreators + mPublishers + series + textFilter
    }

    override fun hashCode(): Int {
        var result = mCreators.hashCode()
        result = 31 * result + (mSeries?.hashCode() ?: 0)
        result = 31 * result + mPublishers.hashCode()
        result = 31 * result + mStartDate.hashCode()
        result = 31 * result + mEndDate.hashCode()
        result = 31 * result + mMyCollection.hashCode()
        result = 31 * result + mSortOption.hashCode()
        result = 31 * result + mTextFilter.hashCode()
        result = 31 * result + mShowVariants.hashCode()
        return result
    }

    override fun toString(): String =
        "Series: $mSeries Creators: ${mCreators.size} Pubs: " +
                "${mPublishers.size} MyCol: $mMyCollection T: ${mTextFilter?.text}"
}

abstract class SortOption(
    val tag: String,
    val sortColumn: String
) : Serializable {

    override fun toString() = tag
}

class IssueSortOption(
    tag: String, sortColumn: String
) : SortOption(tag, sortColumn)

class SeriesSortOption(
    tag: String, sortColumn: String
) : SortOption(tag, sortColumn)

data class TextFilter(val text: String) : FilterOption {
    override val compareValue: String
        get() = text

    override fun toString(): String = "\"$text\""
}

enum class ReturnType {
    SERIES, ISSUE, CREATOR, PUBLISHER, CHARACTER
}

