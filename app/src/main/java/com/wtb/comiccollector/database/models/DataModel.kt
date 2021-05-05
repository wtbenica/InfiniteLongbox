package com.wtb.comiccollector.database.models

interface DataModel {
    val id: Int
}

interface FilterOption: DataModel, Comparable<FilterOption>