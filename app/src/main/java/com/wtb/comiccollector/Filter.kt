package com.wtb.comiccollector

import androidx.fragment.app.Fragment
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FilterOption
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.item_lists.fragments.IssueListFragment
import com.wtb.comiccollector.item_lists.fragments.SeriesListFragment
import java.io.Serializable
import java.time.LocalDate

const val ARG_FILTER = "Filter"

private const val TAG = APP + "Filter_SortChipGroup"

class Filter(
    creators: MutableSet<Creator>? = null,
    series: Series? = null,
    publishers: MutableSet<Publisher>? = null,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
    myCollection: Boolean = false
) : Serializable {

    constructor(filter: Filter) : this(
        filter.mCreators,
        filter.mSeries,
        filter.mPublishers,
        filter.mStartDate,
        filter.mEndDate,
        filter.mMyCollection
    )

    fun clone() : Filter {
        return Filter(this)
    }

    override fun equals(other: Any?): Boolean {
        return other is Filter && hashCode() == other.hashCode()
    }

    var mCreators: MutableSet<Creator> = creators ?: mutableSetOf()
    var mSeries: Series? = series
        set(value) {
            if (getSortOptions(value) != getSortOptions()) {
                mSortOption = getSortOptions(value)[0]
            }

            field = value
        }
    var mPublishers: MutableSet<Publisher> = publishers ?: mutableSetOf()
    var mStartDate: LocalDate = startDate ?: LocalDate.MIN
    var mEndDate: LocalDate = endDate ?: LocalDate.MAX
    var mMyCollection: Boolean = myCollection
    var mSortOption: SortOption = getSortOptions()[0]
    fun hasCreator() = mCreators.isNotEmpty()
    fun returnsIssueList() = mSeries != null
    fun hasPublisher() = mPublishers.isNotEmpty()
    fun hasDateFilter() = mStartDate != LocalDate.MIN || mEndDate != LocalDate.MAX
    fun isEmpty(): Boolean {
        return mCreators.isEmpty() && mSeries == null && mPublishers.isEmpty() && mStartDate ==
                LocalDate.MIN && mEndDate == LocalDate.MAX && !mMyCollection
    }

    private fun addCreator(vararg creator: Creator) {
        mCreators.addAll(creator)
    }

    private fun removeCreator(vararg creator: Creator) {
        mCreators.removeAll(creator)
    }

    private fun addSeries(series: Series) {
        this.mSeries = series
    }

    private fun removeSeries() {
        this.mSeries = null
    }

    private fun addPublisher(vararg publisher: Publisher) {
        mPublishers.addAll(publisher)
    }

    private fun removePublisher(vararg publisher: Publisher) {
        mPublishers.removeAll(publisher)
    }

    fun setMyCollection(value: Boolean) {
        this.mMyCollection = value
    }

    fun addFilter(vararg items: FilterOption) {
        items.forEach { item ->
            when (item) {
                is Series    -> addSeries(item)
                is Creator   -> addCreator(item)
                is Publisher -> addPublisher(item)
            }
        }
    }

    fun removeFilter(vararg items: FilterOption) {
        items.forEach { item ->
            when (item) {
                is Series    -> removeSeries()
                is Creator   -> removeCreator(item)
                is Publisher -> removePublisher(item)
            }
        }
    }

    fun getFragment(callback: SeriesListFragment.SeriesListCallbacks): Fragment =
        when (mSeries) {
            null -> SeriesListFragment.newInstance(callback, this)
            else -> IssueListFragment.newInstance(this)
        }


    fun getSortOptions(series: Series? = mSeries): List<SortOption> =
        when (series) {
            null -> seriesSortOptions
            else -> issueSortOptions
        }


    fun updateCreators(creators: List<Creator>?) {
        mCreators.clear()
        creators?.let { mCreators.addAll(it) }
    }

    fun updatePublishers(publishers: List<Publisher>?) {
        mPublishers.clear()
        publishers?.let { mPublishers.addAll(it) }
    }

    fun updateSeries(series: Series?) {
        mSeries = series
    }

    fun getAll(): Set<FilterOption> =
        if (mSeries != null) {
            mCreators + mPublishers + mSeries!!
        } else {
            mCreators + mPublishers
        }

    override fun hashCode(): Int {
        var result = mCreators.hashCode()
        result = 31 * result + (mSeries?.hashCode() ?: 0)
        result = 31 * result + mPublishers.hashCode()
        result = 31 * result + mStartDate.hashCode()
        result = 31 * result + mEndDate.hashCode()
        result = 31 * result + mMyCollection.hashCode()
        result = 31 * result + mSortOption.hashCode()
        return result
    }

    override fun toString(): String = "Series: $mSeries"

//    companion object {
//        fun deserialize(str: String?): MutableSet<Int> {
//            return str?.removePrefix("[")?.removeSuffix("]")?.split(", ")
//                ?.mapNotNull { it.toIntOrNull() }?.toMutableSet() ?: mutableSetOf()
//        }
//    }
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

val issueSortOptions: List<SortOption> = listOf(
    IssueSortOption("Issue Number (Low to High)", "issueNum ASC"),
    IssueSortOption("Issue Number (High to Low)", "issueNum DESC"),
    IssueSortOption("Date (Oldest to Newest)", "releaseDate ASC"),
    IssueSortOption("Date (Newest to Oldest)", "releaseDate DESC")
)

class SeriesSortOption(
    tag: String, sortColumn: String
) : SortOption(tag, sortColumn)

val seriesSortOptions: List<SortOption> = listOf(
    SeriesSortOption("Series Name (A-Z)", "sortName ASC"),
    SeriesSortOption("Series Name (Z-A)", "sortName DESC"),
    SeriesSortOption("Date (Oldest to Newest)", "startDate ASC"),
    SeriesSortOption("Date (Newest to Oldest)", "startDate DESC")
)