package com.wtb.comiccollector

import androidx.fragment.app.Fragment
import com.wtb.comiccollector.NewGroupListFragments.NewIssueListFragment
import com.wtb.comiccollector.NewGroupListFragments.NewSeriesListFragment
import java.time.LocalDate

class Filter(val observer: FilterObserver) {
     var creators: MutableSet<Int> = mutableSetOf()
     var series: Int? = null
     var publishers: MutableSet<Int> = mutableSetOf()
     var startDate: LocalDate? = null
     var endDate: LocalDate? = null

    fun addCreator(vararg creator: Creator) {
        creators.addAll(creator.map { it.creatorId })
    }

    fun removeCreator(vararg creator: Creator) {
        creators.removeAll(creator.map { it.creatorId })
    }

    fun addSeries(series: Series) {
        this.series = series.seriesId
    }

    fun removeSeries() {
        this.series = null
    }

    fun addPublisher(vararg publisher: Publisher) {
        publishers.addAll(publisher.map { it.publisherId })
    }

    fun removePublisher(vararg publisher: Publisher) {
        publishers.removeAll(publisher.map { it.publisherId })
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

    fun getFragment(callback: NewSeriesListFragment.Callbacks): Fragment {
        return when {
            series == null -> NewSeriesListFragment.newInstance(callback,this)
            else -> NewIssueListFragment.newInstance(this)
        }
    }


    fun creatorIds(): String = creators.toString()

    fun publisherIds(): String {
        return publishers.toString()
    }

    fun seriesIds(): String {
        return series.toString()
    }

    interface FilterObserver {
        fun onUpdate()
    }

    companion object {
        fun deserialize(str: String?): MutableSet<Int> {
            return str?.removePrefix("[")?.removeSuffix("[")?.split(", ")?.mapNotNull {
                it
                    .toIntOrNull()
            }?.toMutableSet() ?: mutableSetOf()
        }
    }
}
