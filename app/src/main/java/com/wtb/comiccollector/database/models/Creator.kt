package com.wtb.comiccollector.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity
data class Creator(
    @PrimaryKey(autoGenerate = true) val creatorId: Int = AUTO_ID,
    var name: String,
    var sortName: String
) : FilterOption {

    override fun compareTo(other: FilterOption): Int = when (other) {
            is Series -> -1
            is Creator -> this.sortName.compareTo(other.sortName)
            else -> 1 // is Publisher
        }

    override fun toString(): String {
        return name
    }
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Creator::class,
            parentColumns = arrayOf("creatorId"),
            childColumns = arrayOf("creatorId"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = arrayOf("creatorId")),
    ]
)
data class NameDetail(
    @PrimaryKey(autoGenerate = true) val nameDetailId: Int = AUTO_ID,
    var creatorId: Int,
    var name: String
) : DataModel