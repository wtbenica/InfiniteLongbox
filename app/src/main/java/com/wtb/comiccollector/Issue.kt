package com.wtb.comiccollector

import android.net.Uri
import android.util.Log
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.wtb.comiccollector.GroupListFragments.GroupListFragment
import java.io.InvalidObjectException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val AUTO_ID = 0

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
    ]
)
data class Issue(
    @PrimaryKey(autoGenerate = true) val issueId: Int = AUTO_ID,
    var seriesId: Int = DUMMY_ID,
    var issueNum: Int = 1,
    var coverUri: Uri? = null,
    var releaseDate: LocalDate? = null,
    var upc: Long? = null,
    var variantName: String? = null,
    var variantOf: Int? = null
) {
    val coverFileName: String
        get() = "IMG_$issueId.jpg"

    companion object {
        fun fromItem(item: Item<GcdIssueJson, Issue>): Issue {
            val fields: GcdIssueJson =
                item.fields ?: throw InvalidObjectException("fields must not be null")

            Log.d("Issue.fromItem", fields.onSaleDate)

            return Issue(
                issueId = item.pk,
                seriesId = fields.seriesId,
                issueNum = fields.number.toIntOrNull() ?: 1,
                releaseDate = if (fields.onSaleDate == "") {
                    null
                } else {
                    LocalDate.parse(
                        formatDate(fields.onSaleDate),
                        DateTimeFormatter.ofPattern("uuuu-MM-dd")
                    )

                },
                upc = fields.barcode.toLongOrNull(),
                variantName = fields.variantName,
                variantOf = fields.variantOf
            )
        }

        fun formatDate(date: String): String {
            var res = date
            while (res.length < 10) {
                res = res.plus("-01")
            }
            return res.replace("-00", "-01")
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
        // TODO: This is creating conflicts bc 
        Index(value = ["seriesName", "volume", "publisherId", "publishingFormat"], unique = true),
        Index(value = ["publisherId"])
    ]
)
public data class Series(
    @PrimaryKey(autoGenerate = true) var seriesId: Int = AUTO_ID,
    var seriesName: String = "New Series",
    var volume: Int = 1,
    var publisherId: Int = AUTO_ID,
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,
    var description: String? = null,
    var publishingFormat: String? = null
) : GroupListFragment.Indexed {

    override fun getIndex(): Char = seriesName.get(0).toUpperCase()

    override fun toString(): String = "$seriesId $seriesName $dateRange"

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

//    companion object {
//        fun fromItem(item: Item<GcdSeriesJson, Series>): Series {
//            val fields: GcdSeriesJson =
//                item.fields ?: throw InvalidObjectException("fields must not be null")
//            return Series(
//                seriesId = item.pk,
//                seriesName = fields.name ?: "",
//                publisherId = fields.publisher?.get(0)?.toInt() ?: AUTO_ID,
//                startDate = when (fields.yearBeganUncertain as Int) {
//                    0 -> LocalDate.of(
//                        fields.yearBegan ?: LocalDate.MIN.year,
//                        1,
//                        1
//                    )
//                    else -> null
//                },
//                endDate = when (fields.yearEndedUncertain) {
//                    0 -> fields.yearEnded?.let {
//                        LocalDate.of(
//                            it,
//                            1,
//                            1
//                        )
//                    }
//                    else -> null
//                },
//            )
//        }
//    }
}

@Entity(
    indices = [
        Index(value = ["firstName", "middleName", "lastName", "suffix", "number"])
    ]
)
data class Creator(
    @PrimaryKey(autoGenerate = true) val creatorId: Int = AUTO_ID,
    var firstName: String,
    var middleName: String? = null,
    var lastName: String? = null,
    var suffix: String? = null,
    var number: Int = 1
) : GroupListFragment.Indexed {
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

    override fun getIndex(): Char {
        return sortName.get(0)
    }

    override fun toString(): String {
        return name
    }
}

@Entity
data class Publisher(
    @PrimaryKey(autoGenerate = true) val publisherId: Int = AUTO_ID,
    val publisher: String = ""
) {
    override fun toString(): String {
        return publisher
    }

//    companion object {
//        fun fromItem(item: Item<GcdPublisherJson, Publisher>): Publisher {
//            val fields: GcdPublisherJson =
//                item.fields ?: throw InvalidObjectException("fields must not be null")
//            return Publisher(
//                publisherId = item.pk,
//                publisher = fields.name,
//            )
//        }
//    }
}


@Entity
data class Role(
    @PrimaryKey(autoGenerate = true) val roleId: Int = AUTO_ID,
    var roleName: String = "",
    var sortOrder: Int
) {
    override fun toString(): String = roleName

    companion object {
        fun fromItem(item: Item<GcdRoleJson, Role>): Role {
            val fields: GcdRoleJson =
                item.fields ?: throw InvalidObjectException("fields must not be null")
            Log.d("Issue", "roleId: ${item.pk}")
            return Role(
                roleId = item.pk,
                roleName = fields.name,
                sortOrder = fields.sortCode
            )
        }
    }
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
    @PrimaryKey(autoGenerate = true) val creditId: Int = AUTO_ID,
    var issueId: Int,
    var creatorId: Int,
    var roleId: Int
) {

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
    var series: Series
)