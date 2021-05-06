package com.wtb.comiccollector.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity
data class Character(
    @PrimaryKey(autoGenerate = true) val characterId: Int = AUTO_ID,
    var name: String,
    var aka: String? = null
) : DataModel() {

    val sortName: String
        get() {
            val shortName = name.removePrefix("The ")
            return if (shortName == name) {
                "$name [$aka]"
            } else {
                "$shortName, The [$aka]"
            }
        }

    override val id: Int
        get() = characterId
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = arrayOf("storyId"),
            childColumns = arrayOf("characterId"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Character::class,
            parentColumns = arrayOf("characterId"),
            childColumns = arrayOf("characterId"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["storyId"]),
        Index(value = ["characterId"])
    ]
)
data class Appearance(
    @PrimaryKey(autoGenerate = true) val appearanceId: Int = AUTO_ID,
    val storyId: Int,
    val characterId: Int,
    val details: String?
) : DataModel() {
    override val id: Int
        get() = appearanceId
}