package com.wtb.comiccollector.database.models

import android.net.Uri
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.wtb.comiccollector.repository.DUMMY_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

const val AUTO_ID = 0

@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Series::class,
            parentColumns = arrayOf("seriesId"),
            childColumns = arrayOf("series"),
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
        Index(value = ["series", "issueNum"]),
        Index(value = ["variantOf"]),
    ]
)
data class Issue(
    @PrimaryKey(autoGenerate = true) val issueId: Int = AUTO_ID,
    val series: Int = DUMMY_ID,
    val issueNum: Int = 1,
    val releaseDate: LocalDate? = null,
    val upc: Long? = null,
    val variantName: String = "",
    val variantOf: Int? = null,
    val sortCode: Int = 0,
    val coverDateLong: String? = null,
    val onSaleDateUncertain: Boolean = false,
    val coverDate: LocalDate? = null,
    val notes: String? = null,
    val brandId: Int? = null,
    val issueNumRaw: String?,
) : DataModel() {

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
                            e.printStackTrace()
                            null
                        }
                    }
                }
            }

            return res
        }
    }
}

@ExperimentalCoroutinesApi
data class FullIssue @ExperimentalCoroutinesApi constructor(
    @Embedded
    val issue: Issue,

    @Relation(parentColumn = "series", entityColumn = "seriesId", entity = Series::class)
    val seriesAndPublisher: SeriesAndPublisher,

    @Relation(parentColumn = "issueId", entityColumn = "issue")
    val cover: Cover? = null,

    @Relation(parentColumn = "issueId", entityColumn = "issue")
    val myCollection: MyCollection? = null,
) : ListItem {
    val series: Series
        get() = seriesAndPublisher.series

    val publisher: Publisher
        get() = seriesAndPublisher.publisher

    val coverUri: Uri?
        get() = cover?.coverUri
}

// TODO: This doesn't need a separate table... right? I forget why I did in the first place. it
//  seems like it was done more out of necessity than purpose
@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Issue::class,
            parentColumns = arrayOf("issueId"),
            childColumns = arrayOf("issue"),
            onDelete = CASCADE
        ),
    ],
    indices = [
        Index(value = ["issue"], unique = true),
    ]
)
data class Cover(
    @PrimaryKey(autoGenerate = true) val coverId: Int = AUTO_ID,
    val issue: Int,
    val coverUri: Uri? = null,
) : DataModel() {
    override val id: Int
        get() = coverId
}

@Entity
data class Brand(
    @PrimaryKey(autoGenerate = true) val brandId: Int = AUTO_ID,
    val name: String,
    val yearBegan: LocalDate? = null,
    val yearEnded: LocalDate? = null,
    val notes: String? = null,
    val url: String? = null,
    val issueCount: Int,
    val yearBeganUncertain: Boolean,
    val yearEndedUncertain: Boolean,
) : DataModel() {
    override val id: Int
        get() = brandId
}