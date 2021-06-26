package com.wtb.comiccollector.database.models

import java.io.Serializable
import java.time.LocalDate

abstract class DataModel(var lastUpdated: LocalDate = LocalDate.now()) : Serializable {

    abstract val id: Int
}

interface FilterOption : Comparable<FilterOption>, Serializable {
    val compareValue: String

    override fun compareTo(other: FilterOption): Int =
        this.compareValue.compareTo(other.compareValue)
}

interface ListItem