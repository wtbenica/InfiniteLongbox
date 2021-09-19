package com.wtb.comiccollector.database.models

import androidx.room.*
import com.wtb.comiccollector.ComicCollectorApplication
import com.wtb.comiccollector.R
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Publisher::class,
            parentColumns = ["publisherId"],
            childColumns = ["publisher"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["publisher"]),
        Index(value = ["name"]),
        Index(value = ["alterEgo"])
    ]
)
data class Character(
    @PrimaryKey(autoGenerate = true) val characterId: Int = AUTO_ID,
    val name: String,
    val alterEgo: String? = null,
    val publisher: Int,
) : DataModel(), FilterModel, ListItem {

    val sortName: String
        get() {
            val shortName = name.removePrefix("The ")
            return if (shortName == name) {
                "$name [$alterEgo]"
            } else {
                "$shortName, The [$alterEgo]"
            }
        }

    override val id: Int
        get() = characterId

    override val tagName: String
        get() = ComicCollectorApplication.context!!.getString(R.string.filter_type_character)

    override val compareValue: String
        get() = name

    override fun toString(): String = name

    companion object : FilterType {
        override val displayName: String =
            ComicCollectorApplication.context!!.getString(R.string.filter_type_character)

        override fun toString(): String = displayName
    }
}

@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = arrayOf("storyId"),
            childColumns = arrayOf("story"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Character::class,
            parentColumns = arrayOf("characterId"),
            childColumns = arrayOf("character"),
            onDelete = ForeignKey.CASCADE
        ),
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
        ),
    ],
    indices = [
        Index(value = ["story"]),
        Index(value = ["character"]),
        Index(value = ["issue"]),
        Index(value = ["series"])
    ]
)
data class Appearance(
    @PrimaryKey(autoGenerate = true) val appearanceId: Int = AUTO_ID,
    val story: Int,
    val character: Int,
    val details: String?,
    val notes: String?,
    val membership: String?,
    val issue: Int,
    val series: Int,
) : DataModel() {
    override val id: Int
        get() = appearanceId
}

@ExperimentalCoroutinesApi
data class FullAppearance(
    @Embedded
    val appearance: Appearance,

    @Relation(parentColumn = "character", entityColumn = "characterId", entity = Character::class)
    val character: FullCharacter,
)

@ExperimentalCoroutinesApi
data class FullCharacter(
    @Embedded
    val character: Character,

    @Relation(parentColumn = "publisher", entityColumn = "publisherId")
    val publisher: Publisher,
) : ListItem

