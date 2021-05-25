package com.wtb.comiccollector.database.models

import java.io.Serializable
import java.time.LocalDateTime

abstract class DataModel: Serializable {
    init {
        val lastUpdated = LocalDateTime.now()
    }

    abstract val id: Int
}

abstract class FilterOption: DataModel(), Comparable<FilterOption> {
    abstract val compareValue: String

    override fun compareTo(other: FilterOption): Int = this.compareValue.compareTo(other.compareValue)
}