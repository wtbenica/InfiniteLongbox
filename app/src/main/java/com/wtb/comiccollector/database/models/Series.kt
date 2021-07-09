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
    val firstIssueId: Int? = null
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

    companion object: FilterTypeSpinnerOption {
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
    val publisher: String = ""
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

    companion object: FilterTypeSpinnerOption {
        override val displayName: String = context!!.getString(R.string.filter_type_publisher)

        override fun toString(): String = displayName
    }
}

@ExperimentalCoroutinesApi
data class SeriesAndPublisher(
    @Embedded
    val series: Series,

    @Relation(parentColumn = "publisherId", entityColumn = "publisherId")
    var publisher: Publisher
)

@ExperimentalCoroutinesApi
data class FullSeries(
    @Embedded
    val series: Series,

    @Relation(parentColumn = "publisherId", entityColumn = "publisherId")
    var publisher: Publisher,

    @Relation(parentColumn = "firstIssueId", entityColumn = "issueId", entity = Issue::class)
    var firstIssue: FullIssue?
) : ListItem