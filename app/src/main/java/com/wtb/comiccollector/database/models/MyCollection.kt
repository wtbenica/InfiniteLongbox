package com.wtb.comiccollector.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wtb.comiccollector.AUTO_ID
import com.wtb.comiccollector.Issue

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
) : DataModel {
    override fun id(): Int = collectionId
}

