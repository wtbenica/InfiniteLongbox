package com.wtb.comiccollector.database.models

import android.net.Uri
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.wtb.comiccollector.repository.DUMMY_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject

const val AUTO_ID = 0

@ExperimentalCoroutinesApi
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
    var releaseDate: LocalDate? = null,
    var upc: Long? = null,
    var variantName: String = "",
    val variantOf: Int? = null,
    var sortCode: Int = 0,
    var coverDateLong: String? = null,
    var onSaleDateUncertain: Boolean = false,
    var coverDate: LocalDate? = null,
    var notes: String? = null,
    var brandId: Int? = null,
    var issueNumRaw: String?,
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

    fun dumpMe(): String {
        return "Issue(num:$issueNum, ser:$seriesId, var:$variantOf"
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

    @Relation(parentColumn = "seriesId", entityColumn = "seriesId", entity = Series::class)
    var seriesAndPublisher: SeriesAndPublisher,

    @Relation(parentColumn = "issueId", entityColumn = "issueId")
    var cover: Cover? = null,

    @Relation(parentColumn = "issueId", entityColumn = "issueId")
    var myCollection: MyCollection? = null,
) : ListItem {
    val series: Series
        get() = seriesAndPublisher.series

    val publisher: Publisher
        get() = seriesAndPublisher.publisher

    val coverUri: Uri?
        get() = cover?.coverUri
}

@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Issue::class,
            parentColumns = arrayOf("issueId"),
            childColumns = arrayOf("issueId"),
            onDelete = CASCADE
        ),
    ],
    indices = [
        Index(value = ["issueId"], unique = true),
    ]
)
data class Cover(
    @PrimaryKey(autoGenerate = true) val coverId: Int = AUTO_ID,
    var issueId: Int,
    var coverUri: Uri? = null,
) : DataModel() {
    override val id: Int
        get() = coverId
}

@Entity
data class Brand(
    @PrimaryKey(autoGenerate = true) val brandId: Int = AUTO_ID,
    var name: String,
    var yearBegan: LocalDate? = null,
    var yearEnded: LocalDate? = null,
    var notes: String? = null,
    var url: String? = null,
    var issueCount: Int,
    var yearBeganUncertain: Boolean,
    var yearEndedUncertain: Boolean,
) : DataModel() {
    override val id: Int
        get() = brandId
}