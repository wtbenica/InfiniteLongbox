package com.wtb.comiccollector.database.models

import androidx.room.*
import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Publisher::class,
            parentColumns = arrayOf("publisherId"),
            childColumns = arrayOf("publisherId"),
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = ["seriesName"]),
        Index(value = ["publisherId"]),
    ]
)
data class Series(
    @PrimaryKey(autoGenerate = true) var seriesId: Int = AUTO_ID,
    var seriesName: String = "New Series",
    var sortName: String? = null,
    var volume: Int = 1,
    var publisherId: Int = AUTO_ID,
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,
    var description: String? = null,
    var publishingFormat: String? = null,
    val firstIssueId: Int? = null,
    val notes: String? = null,
    val issueCount: Int = 0
) : DataModel(), FilterOptionAutoCompletePopupItem, Serializable {
    override val tagName: String
        get() = "Series"

    override val compareValue: String
        get() = sortName ?: seriesName

    override val id: Int
        get() = seriesId

    override fun toString(): String = "$seriesName $dateRange"

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

    companion object : FilterTypeSpinnerOption {
        override val displayName: String = context!!.getString(R.string.filter_type_series)

        override fun toString(): String = displayName
    }
}

@ExperimentalCoroutinesApi
@Entity(
    indices = [
        Index(value = ["publisher"]),
    ]
)
data class Publisher(
    @PrimaryKey(autoGenerate = true) val publisherId: Int = AUTO_ID,
    val publisher: String = "",
    var yearBegan: LocalDate? = null,
    var yearBeganUncertain: Boolean = true,
    var yearEnded: LocalDate? = null,
    var yearEndedUncertain: Boolean = true,
    var url: String? = null,
) : DataModel(), FilterOptionAutoCompletePopupItem {
    override val tagName: String
        get() = "Publisher"

    override val compareValue: String
        get() = publisher

    override val id: Int
        get() = publisherId

    override fun toString(): String {
        return publisher
    }

    companion object : FilterTypeSpinnerOption {
        override val displayName: String = context!!.getString(R.string.filter_type_publisher)

        override fun toString(): String = displayName
    }
}

@Entity
data class BondType(
    @PrimaryKey(autoGenerate = true) val bondTypeId: Int = AUTO_ID,
    val name: String,
    var description: String,
    var notes: String? = null,
): DataModel() {
    override val id: Int
        get() = bondTypeId
}

@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Series::class,
            parentColumns = arrayOf("seriesId"),
            childColumns = arrayOf("originId"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Series::class,
            parentColumns = arrayOf("seriesId"),
            childColumns = arrayOf("targetId"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Issue::class,
            parentColumns = arrayOf("issueId"),
            childColumns = arrayOf("originIssueId"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Issue::class,
            parentColumns = arrayOf("issueId"),
            childColumns = arrayOf("targetIssueId"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BondType::class,
            parentColumns = arrayOf("bondTypeId"),
            childColumns = arrayOf("bondTypeId"),
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["originId"]),
        Index(value = ["targetId"]),
        Index(value = ["originIssueId"]),
        Index(value = ["targetIssueId"]),
        Index(value = ["bondTypeId"]),
    ]
)
data class SeriesBond(
    @PrimaryKey(autoGenerate = true) var bondId: Int = AUTO_ID,
    val originId: Int,
    val targetId: Int,
    val originIssueId: Int?,
    val targetIssueId: Int?,
    val bondTypeId: Int,
    val notes: String?
) : DataModel() {
    override val id: Int
        get() = bondId
}

// it might be tempting to remove the second class and just use the first, but the second one is
// used in an Issue POJO where the first one can't be: firstIssue can create a circular reference
@ExperimentalCoroutinesApi
data class FullSeries(
    @Embedded
    val series: Series,

    @Relation(parentColumn = "publisherId", entityColumn = "publisherId")
    var publisher: Publisher,

    @Relation(parentColumn = "firstIssueId", entityColumn = "issueId", entity = Issue::class)
    var firstIssue: FullIssue?
) : ListItem

@ExperimentalCoroutinesApi
data class SeriesAndPublisher(
    @Embedded
    val series: Series,

    @Relation(parentColumn = "publisherId", entityColumn = "publisherId")
    var publisher: Publisher
)
