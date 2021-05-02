package com.wtb.comiccollector.database.models

interface DataModel {
    fun id(): Int
}

interface Filterable : DataModel, Comparable<Filterable> {
    fun sortValue(): String

    override fun compareTo(other: Filterable): Int {
        return sortValue().compareTo(other.sortValue())
    }
}

