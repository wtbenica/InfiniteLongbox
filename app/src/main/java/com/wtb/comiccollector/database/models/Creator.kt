package com.wtb.comiccollector.database.models

import androidx.room.*
import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.R
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Entity(
    indices = [
        Index(value = ["name"]),
        Index(value = ["sortName"])
    ]
)
data class Creator(
    @PrimaryKey(autoGenerate = true) val creatorId: Int = AUTO_ID,
    var name: String,
    var sortName: String,
    var bio: String? = null
) : DataModel(), FilterAutoCompleteType {

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
        Index(value = ["name"]),
        Index(value = ["sortName"])
    ]
)
data class NameDetail(
    @PrimaryKey(autoGenerate = true) val nameDetailId: Int = AUTO_ID,
    var creatorId: Int,
    var name: String,
    var sortName: String? = null
) : DataModel(), FilterAutoCompleteType {
    override val id: Int
        get() = nameDetailId

    companion object : FilterTypeSpinnerOption {
        override val displayName: String = context!!.getString(R.string.filter_type_creator)

        override fun toString(): String = displayName
    }

    override val tagName: String
        get() = displayName

    override val compareValue: String
        get() = name
}

@ExperimentalCoroutinesApi
data class NameDetailAndCreator(
    @Embedded
    val nameDetail: NameDetail,

    @Relation(parentColumn = "creatorId", entityColumn = "creatorId")
    var creator: Creator
) : ListItem

@ExperimentalCoroutinesApi
data class FullCreator(
    @Embedded
    val creator: Creator,

    @Relation(parentColumn = "creatorId", entityColumn = "creatorId")
    var nameDetail: List<NameDetail>
)