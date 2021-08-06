package com.wtb.comiccollector.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Issue::class,
            parentColumns = arrayOf("issueId"),
            childColumns = arrayOf("issue"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Series::class,
            parentColumns = arrayOf("seriesId"),
            childColumns = arrayOf("series"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("issue", unique = true),
        Index("series", unique = true)
    ]
)
data class MyCollection(
    @PrimaryKey(autoGenerate = true) val collectionId: Int = AUTO_ID,
    var issue: Int,
    var series: Int,
) : DataModel() {
    override val id: Int
        get() = collectionId
}

