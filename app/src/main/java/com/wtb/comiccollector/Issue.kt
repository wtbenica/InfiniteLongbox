package com.wtb.comiccollector

import android.net.Uri
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import java.io.InvalidObjectException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val NEW_SERIES_ID = 0

// TODO: For all entities, need to add onDeletes: i.e. CASCADE, etc.
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Series::class,
            parentColumns = arrayOf("seriesId"),
            childColumns = arrayOf("seriesId"),
            onDelete = CASCADE
        ),
    ],
    indices = [
        Index(value = ["seriesId", "issueNum"]),
    ]
)
data class Issue(
    @PrimaryKey val issueId: Int = 0,
    var seriesId: Int = NEW_SERIES_ID,
    var issueNum: Int = 1,
    var coverUri: Uri? = null,
    var releaseDate: LocalDate? = null,
    var upc: Long? = null
) {
    val coverFileName: String
        get() = "IMG_$issueId.jpg"
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Publisher::class,
            parentColumns = arrayOf("publisherId"),
            childColumns = arrayOf("publisherId")
        )
    ],
    indices = [
        Index(value = ["seriesName", "volume", "publisherId"], unique = true),
        Index(value = ["publisherId"])
    ]
)
data class Series(
    @PrimaryKey val seriesId: Int = 0,
    var seriesName: String = "New Series",
    var volume: Int = 1,
    var publisherId: Int = NEW_SERIES_ID,
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,
    var description: String? = null
) {
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

    companion object {
        fun fromItem(item: Item<GcdSeriesJson>): Series {
            val fields: GcdSeriesJson =
                item.fields ?: throw InvalidObjectException("fields must not be null")
            return Series(
                seriesId = item.pk,
                seriesName = fields.name ?: "",
                publisherId = fields.publisher?.get(0)?.toInt() ?: 0,
                startDate = when (fields.yearBeganUncertain as Int) {
                    0 -> LocalDate.of(
                        fields.yearBegan ?: LocalDate.MIN.year,
                        1,
                        1
                    )
                    else -> null
                },
                endDate = when (fields.yearEndedUncertain) {
                    0 -> fields.yearEnded?.let {
                        LocalDate.of(
                            it,
                            1,
                            1
                        )
                    }
                    else -> null
                },
            )
        }
    }
}

@Entity(
    indices = [
        Index(value = ["firstName", "middleName", "lastName", "suffix", "number"])
    ]
)
data class Creator(
    @PrimaryKey val creatorId: Int = 0,
    var firstName: String,
    var middleName: String? = null,
    var lastName: String? = null,
    var suffix: String? = null,
    var number: Int = 1
) {
    val name: String
        get() = firstName +
                (if (middleName != null) " $middleName" else "") +
                (if (lastName != null) " $lastName" else "") +
                (if (suffix != null) " $suffix" else "")

    val sortName: String
        get() = (if (suffix != null && lastName != null) "$lastName $suffix, "
        else (if (lastName != null) "$lastName, " else "")) +
                firstName +
                if (middleName != null) "$middleName" else ""

    override fun toString(): String {
        return name
    }
}

@Entity
data class Publisher(
    @PrimaryKey val publisherId: Int = 0,
    val publisher: String = ""
) {
    override fun toString(): String {
        return publisher
    }

    companion object {
        fun fromItem(item: Item<GcdPublisherJson>): Publisher {
            val fields: GcdPublisherJson =
                item.fields ?: throw InvalidObjectException("fields must not be null")
            return Publisher(
                publisherId = item.pk,
                publisher = fields.name ?: "",
            )
        }
    }
}


@Entity
data class Role(
    @PrimaryKey val roleId: Int = 0,
    var roleName: String = "",
    var sortOrder: Int
) {
    override fun toString(): String = roleName
}

@Entity(
    indices = [
        Index(value = ["issueId", "creatorId", "roleId"], unique = true),
        Index(value = ["issueId"]), Index(value = ["creatorId"]), Index(value = ["roleId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Issue::class,
            parentColumns = arrayOf("issueId"),
            childColumns = arrayOf("issueId"),
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
data class Credit(
    @PrimaryKey val creditId: Int = 0,
    var issueId: Int,
    var creatorId: Int,
    var roleId: Int
)

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
        parentColumn = "creatorId",
        entityColumn = "creatorId"
    )
    var creator: Creator,

    @Relation(
        parentColumn = "roleId",
        entityColumn = "roleId"
    )
    val role: Role
)

data class IssueAndSeries(
    @Embedded
    val issue: Issue,

    @Relation(
        parentColumn = "seriesId",
        entityColumn = "seriesId"
    )
    val series: Series
)