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
            entity = UserCollection::class,
            parentColumns = arrayOf("userCollectionId"),
            childColumns = arrayOf("userCollection"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("issue", "userCollection", unique = true),
        Index("series"),
    ]
)
data class CollectionItem(
    @PrimaryKey(autoGenerate = true) val collectionItemId: Int = AUTO_ID,
    var issue: Int,
    var series: Int,
    var userCollection: Int
) : DataModel() {
    override val id: Int
        get() = collectionItemId
}

@Entity
data class UserCollection(
    @PrimaryKey(autoGenerate = true) val userCollectionId: Int = AUTO_ID,
    var name: String,
    var permanent: Boolean = false
) : DataModel() {
    override val id: Int
        get() = userCollectionId
}

enum class BaseCollection(val id: Int) {
    MY_COLL(1), WISH_LIST(2)
}

