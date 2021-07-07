package com.wtb.comiccollector.database.models

import androidx.room.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Entity
data class Creator(
    @PrimaryKey(autoGenerate = true) val creatorId: Int = AUTO_ID,
    var name: String,
    var sortName: String
) : DataModel(), FilterOptionAutoCompletePopupItem {

    override val id: Int
        get() = creatorId

    override val tagName: String
        get() = "Creator"

    override val compareValue: String
        get() = sortName

    override fun toString(): String {
        return name
    }
}

@ExperimentalCoroutinesApi
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
) : DataModel(), FilterOptionAutoCompletePopupItem {
    override val id: Int
        get() = nameDetailId

    companion object : FilterTypeSpinnerOption {
        override val displayName: String
            get() = "Creator"
    }

    override val tagName: String
        get() = Companion.displayName

    override val compareValue: String
        get() = name
}

@ExperimentalCoroutinesApi
data class NameDetailAndCreator(
    @Embedded
    val nameDetail: NameDetail,

    @Relation(parentColumn = "creatorId", entityColumn = "creatorId")
    var creator: Creator
)