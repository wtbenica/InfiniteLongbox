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
    sortOption: (a: Series, b: Series) -> Int = SortOption.ALPHA.compare,
    myCollection: Boolean = false
) : Serializable {

    var mCreators: MutableSet<Creator> = creators ?: mutableSetOf()
    var mSeries: Series? = series
    var mPublishers: MutableSet<Publisher> = publishers ?: mutableSetOf()
    var mStartDate: LocalDate = startDate ?: LocalDate.MIN
    var mEndDate: LocalDate = endDate ?: LocalDate.MAX
    var mSortOrder: (a: Series, b: Series) -> Int = sortOption
    var mMyCollection: Boolean = myCollection

    fun hasCreator() = mCreators.isNotEmpty()
    fun hasSeries() = mSeries != null
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

    companion object {
        fun deserialize(str: String?): MutableSet<Int> {
            return str?.removePrefix("[")?.removeSuffix("]")?.split(", ")
                ?.mapNotNull { it.toIntOrNull() }?.toMutableSet() ?: mutableSetOf()
        }
    }
}
