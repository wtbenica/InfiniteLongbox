package com.wtb.comiccollector.database.models

import androidx.room.*

@Entity
data class Creator(
    @PrimaryKey(autoGenerate = true) val creatorId: Int = AUTO_ID,
    var name: String,
    var sortName: String
) : FilterOption() {
    override val compareValue: String
        get() = sortName

    override val id: Int
        get() = creatorId

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
) : DataModel() {
    override val id: Int
        get() = nameDetailId
}

data class NameDetailAndCreator(
    @Embedded
    val nameDetail: NameDetail,

    @Relation(parentColumn = "creatorId", entityColumn = "creatorId")
    var creator: Creator
)

data class FullCreator(
    @Embedded
    val creator: Creator,

    @Relation(parentColumn = "creatorId", entityColumn = "creatorId")
    var nameDetail: NameDetail
)