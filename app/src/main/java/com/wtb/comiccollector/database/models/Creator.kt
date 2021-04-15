package com.wtb.comiccollector.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wtb.comiccollector.AUTO_ID

@Entity
data class Creator(
    @PrimaryKey(autoGenerate = true) val creatorId: Int = AUTO_ID,
    var name: String,
    var sortName: String
) : Filterable {

    override fun id(): Int = creatorId

    override fun sortValue(): String = name

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
) : DataModel {
    override fun id(): Int = nameDetailId
}