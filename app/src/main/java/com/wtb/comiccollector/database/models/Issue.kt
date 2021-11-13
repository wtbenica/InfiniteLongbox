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
            onDelete = CASCADE,
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
        get() = "COVER_$issueId.jpg"

    val url: String
        get() = "https://infinite-longbox.uc.r.appspot.com/db_query/issue/id/$id/cover"

    override val id: Int
        get() = issueId

    override fun toString(): String {
        return if (variantOf == null) {
            "$issueNumRaw"
        } else {
            "$issueNumRaw $variantName"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Issue

        if (issueId != other.issueId) return false
        if (series != other.series) return false
        if (issueNum != other.issueNum) return false
        if (releaseDate != other.releaseDate) return false
        if (upc != other.upc) return false
        if (variantName != other.variantName) return false
        if (variantOf != other.variantOf) return false
        if (sortCode != other.sortCode) return false
        if (coverDateLong != other.coverDateLong) return false
        if (onSaleDateUncertain != other.onSaleDateUncertain) return false
        if (coverDate != other.coverDate) return false
        if (notes != other.notes) return false
        if (brandId != other.brandId) return false
        if (issueNumRaw != other.issueNumRaw) return false
        if (coverFileName != other.coverFileName) return false
        if (url != other.url) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = issueId
        result = 31 * result + series
        result = 31 * result + issueNum
        result = 31 * result + (releaseDate?.hashCode() ?: 0)
        result = 31 * result + (upc?.hashCode() ?: 0)
        result = 31 * result + variantName.hashCode()
        result = 31 * result + (variantOf ?: 0)
        result = 31 * result + sortCode
        result = 31 * result + (coverDateLong?.hashCode() ?: 0)
        result = 31 * result + onSaleDateUncertain.hashCode()
        result = 31 * result + (coverDate?.hashCode() ?: 0)
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + (brandId ?: 0)
        result = 31 * result + (issueNumRaw?.hashCode() ?: 0)
        result = 31 * result + coverFileName.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + id
        return result
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
    val collectionItems: List<CollectionItem> = emptyList(),
) : ListItem {
    val series: Series
        get() = seriesAndPublisher.series

    val publisher: Publisher
        get() = seriesAndPublisher.publisher

    val coverUri: Uri?
        get() = cover?.coverUri

    override fun toString(): String = "${series.seriesName} #${issue.issueNumRaw}"


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FullIssue

        if (issue != other.issue) return false
        if (seriesAndPublisher != other.seriesAndPublisher) return false
        if (cover != other.cover) return false
        if (collectionItems != other.collectionItems) return false
        if (series != other.series) return false
        if (publisher != other.publisher) return false
        if (coverUri != other.coverUri) return false

        return true
    }

    override fun hashCode(): Int {
        var result = issue.hashCode()
        result = 31 * result + seriesAndPublisher.hashCode()
        result = 31 * result + (cover?.hashCode() ?: 0)
        result = 31 * result + collectionItems.hashCode()
        result = 31 * result + series.hashCode()
        result = 31 * result + publisher.hashCode()
        result = 31 * result + (coverUri?.hashCode() ?: 0)
        return result
    }

    companion object {
        internal fun getEmptyFullIssue(): FullIssue = FullIssue(Issue(issueNumRaw = null), SeriesAndPublisher(Series(), Publisher()))
    }
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
    val markedDelete: Boolean = true
) : DataModel() {
    override val id: Int
        get() = coverId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cover

        if (coverId != other.coverId) return false
        if (issue != other.issue) return false
        if (coverUri != other.coverUri) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = coverId
        result = 31 * result + issue
        result = 31 * result + (coverUri?.hashCode() ?: 0)
        result = 31 * result + id
        return result
    }
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