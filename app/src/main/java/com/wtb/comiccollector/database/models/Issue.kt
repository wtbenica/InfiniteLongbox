package com.wtb.comiccollector.database.models

import android.net.Uri
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.wtb.comiccollector.DUMMY_ID
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
) : DataModel, Comparable<Issue> {
    val coverFileName: String
        get() = "IMG_$issueId.jpg"

    val url: String
        get() = "https://www.comics.org/issue/$issueId/"
    override val id: Int
        get() = issueId

    override fun toString(): String {
        return if (variantOf == null) {
            "$issueNum"
        } else {
            "$issueNum $variantName"
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
                            newDate.subSequence(0, newDate.length).toString() + "-01",
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

    override fun compareTo(other: Issue): Int = this.issueNum.compareTo(other.issueNum)
}

data class FullIssue(
    @Embedded
    val issue: Issue,

    @Relation(parentColumn = "seriesId", entityColumn = "seriesId", entity = Series::class)
    var seriesAndPublisher: SeriesAndPublisher,

    @Relation(parentColumn = "issueId", entityColumn = "issueId")
    var myCollection: MyCollection?
) {
    val series: Series
        get() = seriesAndPublisher.series

    val publisher: Publisher
        get() = seriesAndPublisher.publisher
}