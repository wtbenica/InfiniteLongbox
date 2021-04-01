package com.wtb.comiccollector

import androidx.fragment.app.Fragment
import com.wtb.comiccollector.GroupListFragments.IssueListFragment
import com.wtb.comiccollector.GroupListFragments.SeriesListFragment
import java.time.LocalDate

class Filter(
    private val observer: FilterObserver,
    creators: MutableSet<Int>? = null,
    series: Int? = null,
    publishers: MutableSet<Int>? = null,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
) {

    var mCreators: MutableSet<Int> = creators ?: mutableSetOf()
    var mSeries: Int? = series
    var mPublishers: MutableSet<Int> = publishers ?: mutableSetOf()
    var mStartDate: LocalDate = startDate ?: LocalDate.MIN
    var mEndDate: LocalDate = endDate ?: LocalDate.MAX

    init {

    }

    fun hasCreator() = !mCreators.isEmpty()
    fun hasSeries() = mSeries != null
    fun hasPublisher() = !mPublishers.isEmpty()
    fun hasDateFilter() = mStartDate != LocalDate.MIN || mEndDate != LocalDate.MAX

    private fun addCreator(vararg creator: Creator) {
        mCreators.addAll(creator.map { it.creatorId })
    }

    private fun removeCreator(vararg creator: Creator) {
        mCreators.removeAll(creator.map { it.creatorId })
    }

    private fun addSeries(series: Series) {
        this.mSeries = series.seriesId
    }

    private fun removeSeries() {
        this.mSeries = null
    }

    private fun addPublisher(vararg publisher: Publisher) {
        mPublishers.addAll(publisher.map { it.publisherId })
    }

    private fun removePublisher(vararg publisher: Publisher) {
        mPublishers.removeAll(publisher.map { it.publisherId })
    }

    fun addItem(item: Filterable) {
        when (item) {
            is Series -> addSeries(item)
            is Creator -> addCreator(item)
            is Publisher -> addPublisher(item)
        }
        observer.onUpdate()
    }

    fun removeItem(item: Filterable) {
        when (item) {
            is Series -> removeSeries()
            is Creator -> removeCreator(item)
            is Publisher -> removePublisher(item)
        }
        observer.onUpdate()
    }

    fun getFragment(callback: SeriesListFragment.Callbacks): Fragment {
        return when {
            mSeries == null -> SeriesListFragment.newInstance(callback, this)
            else -> IssueListFragment.newInstance(this)
        }
    }


    fun creatorIds(): String = mCreators.toString()

    fun publisherIds(): String = mPublishers.toString()

    fun seriesIds(): String = mSeries.toString()

    interface FilterObserver {
        fun onUpdate()
    }

    companion object {
        fun deserialize(str: String?): MutableSet<Int> {
            return str?.removePrefix("[")?.removeSuffix("]")?.split(", ")
                ?.mapNotNull { it.toIntOrNull() }?.toMutableSet() ?: mutableSetOf()
        }
    }
}
