package com.wtb.comiccollector

import android.net.Uri
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

const val AUTO_ID = 0

interface DataModel {
    fun id(): Int
}

interface Filterable : DataModel, Comparable<Filterable> {
    fun sortValue(): String

    override fun compareTo(other: Filterable): Int {
        return sortValue().compareTo(other.sortValue())
    }

}

fun <E: Filterable> MutableSet<E>.ids(): String {
    return (this.map { it.id() }).toString()
}

// TODO: For all entities, need to add onDeletes: i.e. CASCADE, etc.
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Series::class,
            parentColumns = arrayOf("seriesId"),
            childColumns = arrayOf("seriesId"),
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = Issue::class,
            parentColumns = arrayOf("issueId"),
            childColumns = arrayOf("variantOf"),
            onDelete = CASCADE
        )
    ],
    indices = [
        Index(value = ["seriesId", "issueNum"]),
        Index(value = ["variantOf"]),
    ]
)
data class Issue(
    @PrimaryKey(autoGenerate = true) val issueId: Int = AUTO_ID,
    var seriesId: Int = DUMMY_ID,
    var issueNum: Int = 1,
    var coverUri: Uri? = null,
    var releaseDate: LocalDate? = null,
    var upc: Long? = null,
    var variantName: String = "",
    var variantOf: Int? = null,
    var sortCode: Int = 0
) : DataModel {
    val coverFileName: String
        get() = "IMG_$issueId.jpg"

    override fun id(): Int = issueId

    override fun toString(): String {
        return if (variantName == "") {
            "Regular"
        } else {
            variantName
        }
    }

    companion object {
        fun formatDate(date: String): LocalDate? {
            val res: LocalDate?

            if (date == "") {
                res = null
            } else {
                val newDate = date.replace("-00", "-01")
                res = try {
                    LocalDate.parse(
                        newDate,
                        DateTimeFormatter.ofPattern("uuuu-MM-dd")
                    )
                } catch (e: DateTimeParseException) {
                    try {
                        LocalDate.parse(
                            newDate.subSequence(0, 8).toString() + "-01",
                            DateTimeFormatter.ofPattern(("uuuu-MM-dd"))
                        )
                    } catch (e: DateTimeParseException) {
                        try {
                            LocalDate.parse(
                                newDate.subSequence(0, 4).toString() + "-01-01",
                                DateTimeFormatter.ofPattern("uuuu-MM-dd")
                            )
                        } catch (e: DateTimeParseException) {
                            throw e
                        }
                    }
                } catch (e: DateTimeParseException) {
                    throw e
                }
            }
            return res
        }
    }
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Publisher::class,
            parentColumns = arrayOf("publisherId"),
            childColumns = arrayOf("publisherId"),
            onDelete = CASCADE
        )
    ],
    indices = [
        Index(value = ["seriesName"]),
        Index(value = ["publisherId"])
    ]
)
data class Series(
    @PrimaryKey(autoGenerate = true) var seriesId: Int = AUTO_ID,
    var seriesName: String = "New Series",
    var sortName: String? = null,
    var volume: Int = 1,
    var publisherId: Int = AUTO_ID,
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,
    var description: String? = null,
    var publishingFormat: String? = null
) : DataModel, Filterable {

    override fun id(): Int = seriesId

    override fun sortValue(): String = seriesName

    override fun toString(): String = "$seriesName $dateRange"

    val fullDescription: String
        get() = "$seriesName vol. $volume $dateRange".removeSuffix(" ")

    val dateRange: String
        get() = startDate?.let {
            "(${it.format(DateTimeFormatter.ofPattern("yyyy"))} - ${
                endDate?.format(
                    DateTimeFormatter.ofPattern("yyyy")
                ) ?: " "
            })"
        } ?: ""

}

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

@Entity
data class Character(
    @PrimaryKey(autoGenerate = true) val characterId: Int = AUTO_ID,
    var name: String,
    var aka: String? = null
) : DataModel {
    override fun id(): Int = characterId

    val sortName: String
        get() {
            val shortName = name.removePrefix("The ")
            return if (shortName == name) {
                "$name [$aka]"
            } else {
                "$shortName, The [$aka]"
            }
        }
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = arrayOf("storyId"),
            childColumns = arrayOf("characterId"),
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = Character::class,
            parentColumns = arrayOf("characterId"),
            childColumns = arrayOf("characterId"),
            onDelete = CASCADE
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
) : DataModel {
    override fun id(): Int = appearanceId
}

@Entity(
    indices = [
        Index(value = ["publisher"]),
    ]
)
data class Publisher(
    @PrimaryKey(autoGenerate = true) val publisherId: Int = AUTO_ID,
    val publisher: String = ""
) : Filterable {
    override fun sortValue(): String = publisher

    override fun id(): Int = publisherId

    override fun toString(): String {
        return publisher
    }
}


@Entity
data class Role(
    @PrimaryKey(autoGenerate = true) val roleId: Int = AUTO_ID,
    var roleName: String = "",
    var sortOrder: Int
) : DataModel {
    override fun toString(): String = roleName

    override fun id(): Int = roleId

    companion object {
        enum class Name(val value: Int) {
            SCRIPT(1), PENCILS(2), INKS(3), COLORS(4), LETTERS(5), EDITING(6), PENCILS_INKS(7),
            PENCILS_INKS_COLORS(8), PAINTING(9), SCRIPT_PENCILS_INKS(10),
            SCRIPT_PENCILS_INKS_COLORS(11), SCRIPT_PENCILS_INKS_LETTERS(12),
            SCRIPT_PENCILS_INKS_COLORS_LETTERS(13), PENCILS_INKS_LETTERS(14)
        }
    }
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Creator::class,
            parentColumns = arrayOf("creatorId"),
            childColumns = arrayOf("creatorId"),
            onDelete = CASCADE
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

@Entity
data class StoryType(
    @PrimaryKey(autoGenerate = true) val typeId: Int = AUTO_ID,
    val name: String,
    val sortCode: Int
) : DataModel {
    override fun id(): Int = typeId
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = StoryType::class,
            parentColumns = arrayOf("typeId"),
            childColumns = arrayOf("storyType")
        )
    ],
    indices = [
        Index(value = ["storyType"]),
    ]
)
data class Story(
    @PrimaryKey(autoGenerate = true) val storyId: Int = AUTO_ID,
    var storyType: Int,
    var title: String? = null,
    var feature: String? = null,
    var characters: String? = null,
    var synopsis: String? = null,
    var notes: String? = null,
    var sequenceNumber: Int = 0,
    val issueId: Int,
) : DataModel {
    override fun id(): Int = storyId
}

@Entity(
    indices = [
        Index(value = ["storyId", "nameDetailId", "roleId"], unique = true),
        Index(value = ["storyId"]),
        Index(value = ["nameDetailId"]),
        Index(value = ["roleId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = arrayOf("storyId"),
            childColumns = arrayOf("storyId"),
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = NameDetail::class,
            parentColumns = arrayOf("nameDetailId"),
            childColumns = arrayOf("nameDetailId"),
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = Role::class,
            parentColumns = arrayOf("roleId"),
            childColumns = arrayOf("roleId"),
            onDelete = CASCADE
        )
    ]
)
data class Credit(
    @PrimaryKey(autoGenerate = true) val creditId: Int = AUTO_ID,
    var storyId: Int,
    var nameDetailId: Int,
    var roleId: Int
) : DataModel {
    override fun id(): Int = creditId
}

fun Int.format(width: Int): String {
    return String.format("%${width}d", this)
}

@Entity(
    indices = [
        Index(value = ["storyId", "creatorId", "roleId"], unique = true),
        Index(value = ["storyId"]),
        Index(value = ["creatorId"]),
        Index(value = ["roleId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = arrayOf("storyId"),
            childColumns = arrayOf("storyId"),
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = Creator::class,
            parentColumns = arrayOf("creatorId"),
            childColumns = arrayOf("creatorId"),
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = Role::class,
            parentColumns = arrayOf("roleId"),
            childColumns = arrayOf("roleId"),
            onDelete = CASCADE
        )
    ]
)
data class MyCredit(
    @PrimaryKey(autoGenerate = true) val creditId: Int = AUTO_ID,
    var storyId: Int,
    var creatorId: Int,
    var roleId: Int
) : DataModel {
    override fun id(): Int = creditId
}

data class FullIssue(
    @Embedded
    val issue: Issue,
    val seriesName: String,
    val publisher: String
)

data class IssueCredits(
    val roleName: String,
    val name: String
)

data class SeriesAndPublisher(
    @Embedded
    val series: Series,

    @Relation(
        parentColumn = "publisherId",
        entityColumn = "publisherId"
    )
    val publisher: Publisher
)

data class FullCredit(
    @Embedded
    val credit: Credit,

    @Relation(
        parentColumn = "nameDetailId",
        entityColumn = "nameDetailId"
    )
    var nameDetail: NameDetail,

    @Embedded
    val creator: Creator,

    @Relation(
        parentColumn = "roleId",
        entityColumn = "roleId"
    )
    val role: Role,

    @Relation(
        parentColumn = "storyId",
        entityColumn = "storyId"
    )
    val story: Story,
)

data class IssueAndSeries(
    @Embedded
    val issue: Issue,

    @Relation(
        parentColumn = "seriesId",
        entityColumn = "seriesId"
    )
    var series: Series
)