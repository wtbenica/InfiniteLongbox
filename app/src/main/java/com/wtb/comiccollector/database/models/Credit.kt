package com.wtb.comiccollector.database.models

import androidx.room.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class CreditX : DataModel() {
    abstract val creditId: Int
    abstract val story: Int
    abstract val nameDetail: Int
    abstract val role: Int
    abstract val issue: Int?
    abstract val series: Int?

    override val id: Int
        get() = creditId
}

@ExperimentalCoroutinesApi
@Entity(
    indices = [
        Index(value = ["story", "nameDetail", "role"], unique = true),
        Index(value = ["story"]),
        Index(value = ["nameDetail"]),
        Index(value = ["role"]),
        Index(value = ["issue"]),
        Index(value = ["series"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = arrayOf("storyId"),
            childColumns = arrayOf("story"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NameDetail::class,
            parentColumns = arrayOf("nameDetailId"),
            childColumns = arrayOf("nameDetail"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Role::class,
            parentColumns = arrayOf("roleId"),
            childColumns = arrayOf("role"),
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
    ]
)
class Credit(
    @PrimaryKey(autoGenerate = true) override val creditId: Int = AUTO_ID,
    override val story: Int,
    override val nameDetail: Int,
    override val role: Int,
    override val issue: Int?,
    override val series: Int?,
) : CreditX()

@ExperimentalCoroutinesApi
@Entity(
    indices = [
        Index(value = ["story", "nameDetail", "role"], unique = true),
        Index(value = ["story"]),
        Index(value = ["nameDetail"]),
        Index(value = ["role"]),
        Index(value = ["issue"]),
        Index(value = ["series"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = arrayOf("storyId"),
            childColumns = arrayOf("story"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NameDetail::class,
            parentColumns = arrayOf("nameDetailId"),
            childColumns = arrayOf("nameDetail"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Role::class,
            parentColumns = arrayOf("roleId"),
            childColumns = arrayOf("role"),
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
    ]
)
data class ExCredit(
    @PrimaryKey(autoGenerate = true) override val creditId: Int = AUTO_ID,
    override val story: Int,
    override val nameDetail: Int,
    override val role: Int,
    override val issue: Int?,
    override val series: Int?,
) : CreditX() 

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = StoryType::class,
            parentColumns = arrayOf("storyTypeId"),
            childColumns = arrayOf("storyType")
        )
    ],
    indices = [
        Index(value = ["storyType"]),
    ]
)
data class Story(
    @PrimaryKey(autoGenerate = true) val storyId: Int = AUTO_ID,
    val storyType: Int,
    val title: String? = null,
    val feature: String? = null,
    val characters: String? = null,
    val synopsis: String? = null,
    val notes: String? = null,
    val sequenceNumber: Int = 0,
    val issue: Int,
) : DataModel() {
    override val id: Int
        get() = storyId
}

@Entity
data class StoryType(
    @PrimaryKey(autoGenerate = true) val storyTypeId: Int = AUTO_ID,
    val name: String,
    val sortCode: Int,
) : DataModel() {
    override val id: Int
        get() = storyTypeId
}

@Entity
data class Role(
    @PrimaryKey(autoGenerate = true) val roleId: Int = AUTO_ID,
    val roleName: String = "",
    val sortOrder: Int,
) : DataModel() {
    override val id: Int
        get() = roleId

    override fun toString(): String = roleName

    // TODO: Marked for deletion 8/6/21
    companion object {
        enum class Name(val value: Int) {
            SCRIPT(1), PENCILS(2), INKS(3), COLORS(4), LETTERS(5), EDITING(6), PENCILS_INKS(7),
            PENCILS_INKS_COLORS(8), PAINTING(9), SCRIPT_PENCILS_INKS(10),
            SCRIPT_PENCILS_INKS_COLORS(11), SCRIPT_PENCILS_INKS_LETTERS(12),
            SCRIPT_PENCILS_INKS_COLORS_LETTERS(13), PENCILS_INKS_LETTERS(14)
        }
    }
}

@ExperimentalCoroutinesApi
data class FullCredit(
    @Embedded
    val credit: Credit,

    @Relation(
        parentColumn = "nameDetail",
        entityColumn = "nameDetailId",
        entity = NameDetail::class
    )
    val nameDetail: NameDetailAndCreator,

    @Relation(parentColumn = "role", entityColumn = "roleId")
    val role: Role,

    @Relation(parentColumn = "story", entityColumn = "storyId")
    val story: Story,

    @Relation(parentColumn = "issue", entityColumn = "issueId")
    val issue: Issue,

    @Relation(parentColumn = "series", entityColumn = "seriesId")
    val series: Series,

    val sortCode: Int,
) : Comparable<FullCredit> {
    override fun compareTo(other: FullCredit): Int {
        val sortCodeCompare = this.sortCode.compareTo(other.sortCode)

        return if (sortCodeCompare == 0) {
            val seqNumCompare = this.story.sequenceNumber.compareTo(other.story.sequenceNumber)
            if (seqNumCompare == 0) {
                this.role.sortOrder.compareTo(other.role.sortOrder)
            } else {
                seqNumCompare
            }
        } else {
            sortCodeCompare
        }
    }
}

@ExperimentalCoroutinesApi
data class FullStory(
    @Embedded
    val story: Story,

    @Relation(parentColumn = "storyType", entityColumn = "storyTypeId")
    val storyType: StoryType,

    @Relation(parentColumn = "issue", entityColumn = "issueId", entity = Issue::class)
    val issue: FullIssue,
)