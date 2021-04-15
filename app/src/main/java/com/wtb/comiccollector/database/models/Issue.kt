package com.wtb.comiccollector

import android.net.Uri
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.wtb.comiccollector.database.models.DataModel
import com.wtb.comiccollector.database.models.Series
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

const val AUTO_ID = 0

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

    val url: String
        get() = "https://www.comics.org/issue/$issueId/cover/2/"

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


data class FullIssue(
    @Embedded
    val issue: Issue,
    val seriesName: String,
    val publisher: String
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