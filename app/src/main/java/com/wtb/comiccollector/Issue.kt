package com.wtb.comiccollector

import android.net.Uri
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

val NEW_SERIES_ID = UUID(0, 0)

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
            entity = Creator::class,
            parentColumns = arrayOf("creatorId"),
            childColumns = arrayOf("writerId")
        )
    ],
    indices = [
        Index(value = ["seriesId", "issueNum"]),
        Index(value = ["writerId"])
    ]
)
data class Issue(
    @PrimaryKey val issueId: UUID = UUID.randomUUID(),
    var seriesId: UUID = NEW_SERIES_ID,
    var issueNum: Int = 1,
    var writer: String = "",
    var writerId: UUID? = null,
    var pencillerId: UUID? = null,
    var inkerId: UUID? = null,
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
    @PrimaryKey val seriesId: UUID = UUID.randomUUID(),
    var seriesName: String = "New Series",
    var volume: Int = 1,
    var publisherId: UUID = NEW_SERIES_ID,
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
}

@Entity(
    indices = [
        Index(value = ["firstName", "middleName", "lastName", "suffix", "number"])
    ]
)
data class Creator(
    @PrimaryKey val creatorId: UUID = UUID.randomUUID(),
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
    @PrimaryKey val publisherId: UUID = UUID.randomUUID(),
    val publisher: String = ""
) {
    override fun toString(): String {
        return publisher
    }
}

@Entity
data class Role(
    @PrimaryKey val roleId: UUID = UUID.randomUUID(),
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
            childColumns = arrayOf("issueId")
        ),
        ForeignKey(
            entity = Creator::class,
            parentColumns = arrayOf("creatorId"),
            childColumns = arrayOf("creatorId")
        ),
        ForeignKey(
            entity = Role::class,
            parentColumns = arrayOf("roleId"),
            childColumns = arrayOf("roleId")
        )
    ]
)
data class Credit(
    @PrimaryKey val creditId: UUID = UUID.randomUUID(),
    var issueId: UUID,
    var creatorId: UUID,
    var roleId: UUID
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

//    @Relation(
//        parentColumn = "issueId",
//        entityColumn = "issueId"
//    )
//    var issue: Issue,
//
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