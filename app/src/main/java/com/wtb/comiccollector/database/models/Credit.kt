package com.wtb.comiccollector.database.models

import androidx.room.*

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
data class Credit(
    @PrimaryKey(autoGenerate = true) val creditId: Int = AUTO_ID,
    var storyId: Int,
    var nameDetailId: Int,
    var roleId: Int
) : DataModel {
    override fun id(): Int = creditId
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
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Creator::class,
            parentColumns = arrayOf("creatorId"),
            childColumns = arrayOf("creatorId"),
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
data class MyCredit(
    @PrimaryKey(autoGenerate = true) val creditId: Int = AUTO_ID,
    var storyId: Int,
    var creatorId: Int,
    var roleId: Int
) : DataModel {
    override fun id(): Int = creditId
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

@Entity
data class StoryType(
    @PrimaryKey(autoGenerate = true) val typeId: Int = AUTO_ID,
    val name: String,
    val sortCode: Int
) : DataModel {
    override fun id(): Int = typeId
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