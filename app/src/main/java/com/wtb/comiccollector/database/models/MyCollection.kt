package com.wtb.comiccollector.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Issue::class,
            parentColumns = arrayOf("issueId"),
            childColumns = arrayOf("issueId"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("issueId", unique = true)
    ]
)
data class MyCollection(
    @PrimaryKey(autoGenerate = true) val collectionId: Int = AUTO_ID,
    var issueId: Int
) : DataModel

