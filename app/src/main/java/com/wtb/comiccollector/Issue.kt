package com.wtb.comiccollector

import android.net.Uri
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate
import java.util.*

@Entity
data class Issue(
    @PrimaryKey val issueId: UUID = UUID.randomUUID(),
    var series: String = "New Issue",
    var seriesId: UUID,
    var volume: Int = 1,
    var issueNum: Int = 1,
    var writer: String = "",
    var penciller: String = "",
    var inker: String = "",
    var coverUri: Uri? = null
) {
    val coverFileName: String
        get() = "IMG_$issueId.jpg"
}

@Entity
data class Series(
    @PrimaryKey val seriesId: UUID = UUID.randomUUID(),
    var seriesName: String = "",
    var volume: Int = 1,
    var publisher: String = "",
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null
) {
    override fun toString(): String {
        return if (startDate != null && endDate != null) {
            "$seriesName v$volume (${startDate!!.year}-${endDate!!.year})"
        } else {
            "$seriesName v$volume"
        }
    }
}

data class FullIssue(
    @Embedded
    val issue: Issue,

    @Relation(parentColumn = "seriesId", entityColumn = "seriesId")
    val series: Series
)

@Entity
data class Creator(
    @PrimaryKey val creatorId: UUID = UUID.randomUUID(),
    var name: String = ""
)

@Entity
data class Role(
    @PrimaryKey val roleId: UUID = UUID.randomUUID(),
    var roleName: String = ""
)

@Entity(primaryKeys = ["issueId", "creatorId", "roleId"])
data class Credit(
    var issueId: UUID,
    var creatorId: UUID,
    var roleId: UUID
)