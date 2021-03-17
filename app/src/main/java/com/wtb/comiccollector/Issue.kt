package com.wtb.comiccollector

import android.net.Uri
import android.util.Log
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.wtb.comiccollector.GroupListFragments.GroupListFragment
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
    var variantName: String? = null,
    var variantOf: Int? = null
) {
    val coverFileName: String
        get() = "IMG_$issueId.jpg"

    companion object {
        fun fromItem(item: Item<GcdIssueJson, Issue>): Issue {
            val fields: GcdIssueJson =
                item.fields

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

}

@Entity
data class Creator(
    @PrimaryKey(autoGenerate = true) val creatorId: Int = AUTO_ID,
    var name: String,
    var sortName: String
) : GroupListFragment.Indexed {

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
                item.fields
            Log.d("Issue", "roleId: ${item.pk}")
            return Role(
                roleId = item.pk,
                roleName = fields.name,
                sortOrder = fields.sortCode
            )
        }
    }
}

@Entity
data class StoryType(
    @PrimaryKey(autoGenerate = true) val typeId: Int = AUTO_ID,
    val name: String,
    val sortCode: Int
)

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
)

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
data class Credit(
    @PrimaryKey(autoGenerate = true) val creditId: Int = AUTO_ID,
    var storyId: Int,
    var creatorId: Int,
    var roleId: Int
) {

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