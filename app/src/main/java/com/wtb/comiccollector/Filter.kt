package com.wtb.comiccollector

import androidx.fragment.app.Fragment
import com.wtb.comiccollector.GroupListFragments.IssueListFragment
import com.wtb.comiccollector.GroupListFragments.SeriesListFragment
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.Filterable
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Series
import java.io.Serializable
import java.time.LocalDate

const val ARG_FILTER = "Filter"

class Filter(
    creators: MutableSet<Creator>? = null,
    series: Series? = null,
    publishers: MutableSet<Publisher>? = null,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
    myCollection: Boolean = false
) : Serializable {

    override fun equals(other: Any?): Boolean {
        return other is Filter && hashCode() == other.hashCode()
    }

    var mCurrentItems: Int = 0
    var mCreators: MutableSet<Creator> = creators ?: mutableSetOf()
    var mSeries: Series? = series
    var mPublishers: MutableSet<Publisher> = publishers ?: mutableSetOf()
    var mStartDate: LocalDate = startDate ?: LocalDate.MIN
    var mEndDate: LocalDate = endDate ?: LocalDate.MAX
    var mMyCollection: Boolean = myCollection
    var mSortOption: SortOption = getSortOptions()[0]
    var mSortOrder: (Filterable, Filterable) -> Int = mSortOption.compare


    fun hasCreator() = mCreators.isNotEmpty()
    fun returnsIssueList() = mSeries != null
    fun hasPublisher() = mPublishers.isNotEmpty()
    fun hasDateFilter() = mStartDate != LocalDate.MIN || mEndDate != LocalDate.MAX

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
        mSortOption = getSortOptions()[0]
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

    fun addItem(vararg items: Filterable) {
        items.forEach { item ->
            when (item) {
                is Series -> addSeries(item)
                is Creator -> addCreator(item)
                is Publisher -> addPublisher(item)
            }
        }
    }

    fun removeItem(vararg items: Filterable) {
        items.forEach { item ->
            when (item) {
                is Series -> removeSeries()
                is Creator -> removeCreator(item)
                is Publisher -> removePublisher(item)
            }
        }
    }

    fun getFragment(callback: SeriesListFragment.Callbacks): Fragment {
        return when (mSeries) {
            null -> SeriesListFragment.newInstance(callback, this)
            else -> IssueListFragment.newInstance(this)
        }
    }

    fun getSortOptions(): List<SortOption> = when (mSeries) {
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

    fun getAll(): Set<Filterable> =
        if (mSeries != null) {
            mCreators + mPublishers + mSeries!!
        } else {
            mCreators + mPublishers
        }

    override fun hashCode(): Int {
        var result =  mCurrentItems
        result = 31 * result + mCreators.hashCode()
        result = 31 * result + (mSeries?.hashCode() ?: 0)
        result = 31 * result + mPublishers.hashCode()
        result = 31 * result + mStartDate.hashCode()
        result = 31 * result + mEndDate.hashCode()
        result = 31 * result + mMyCollection.hashCode()
        result = 31 * result + mSortOption.hashCode()
        result = 31 * result + mSortOrder.hashCode()
        return result
    }

    companion object {
        fun deserialize(str: String?): MutableSet<Int> {
            return str?.removePrefix("[")?.removeSuffix("]")?.split(", ")
                ?.mapNotNull { it.toIntOrNull() }?.toMutableSet() ?: mutableSetOf()
        }
    }
}
