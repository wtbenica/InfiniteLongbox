package com.wtb.comiccollector.database.models

import androidx.room.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class CreditX : DataModel() {
    abstract val creditId: Int
    abstract var storyId: Int
    abstract var nameDetailId: Int
    abstract var roleId: Int
}

@ExperimentalCoroutinesApi
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
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NameDetail::class,
            parentColumns = arrayOf("nameDetailId"),
            childColumns = arrayOf("nameDetailId"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Role::class,
            parentColumns = arrayOf("roleId"),
            childColumns = arrayOf("roleId"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class Credit(
    @PrimaryKey(autoGenerate = true) override val creditId: Int = AUTO_ID,
    override var storyId: Int,
    override var nameDetailId: Int,
    override var roleId: Int
) : CreditX() {
    override val id: Int
        get() = creditId
}

@ExperimentalCoroutinesApi
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
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NameDetail::class,
            parentColumns = arrayOf("nameDetailId"),
            childColumns = arrayOf("nameDetailId"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Role::class,
            parentColumns = arrayOf("roleId"),
            childColumns = arrayOf("roleId"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExCredit(
    @PrimaryKey(autoGenerate = true) override val creditId: Int = AUTO_ID,
    override var storyId: Int,
    override var nameDetailId: Int,
    override var roleId: Int
) : CreditX() {
    override val id: Int
        get() = creditId
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
) : DataModel() {
    override val id: Int
        get() = storyId
}

@Entity
data class StoryType(
    @PrimaryKey(autoGenerate = true) val typeId: Int = AUTO_ID,
    val name: String,
    val sortCode: Int
) : DataModel() {
    override val id: Int
        get() = typeId
}

@Entity
data class Role(
    @PrimaryKey(autoGenerate = true) val roleId: Int = AUTO_ID,
    var roleName: String = "",
    var sortOrder: Int
) : DataModel() {
    override val id: Int
        get() = roleId

    override fun toString(): String = roleName

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
        parentColumn = "nameDetailId",
        entityColumn = "nameDetailId",
        entity = NameDetail::class
    )
    var nameDetail: NameDetailAndCreator,

    @Relation(parentColumn = "roleId", entityColumn = "roleId")
    val role: Role,

    @Relation(parentColumn = "storyId", entityColumn = "storyId")
    val story: Story,

    val sortCode: Int
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