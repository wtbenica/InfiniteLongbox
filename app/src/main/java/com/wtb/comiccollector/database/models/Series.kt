package com.wtb.comiccollector.database.models

import androidx.room.*
import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// TODO: Add fk restraint back to firstIssueId. I don't want to do it right now, in case it 
//  breaks things. Not time to deal with a new problem
@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Publisher::class,
            parentColumns = arrayOf("publisherId"),
            childColumns = arrayOf("publisher"),
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = ["seriesName"]),
        Index(value = ["publisher"]),
    ]
)
data class Series(
    @PrimaryKey(autoGenerate = true) val seriesId: Int = AUTO_ID,
    var seriesName: String = "New Series",
    val sortName: String? = null,
    var volume: Int = 1,
    var publisher: Int = AUTO_ID,
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,
    val description: String? = null,
    val publishingFormat: String? = null,
    val firstIssue: Int? = null,
    val notes: String? = null,
    val issueCount: Int = 0,
) : DataModel(), Serializable {
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

    companion object : FilterType {
        override var displayName: String = context!!.getString(R.string.filter_type_series)

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
    val yearBegan: LocalDate? = null,
    val yearBeganUncertain: Boolean = true,
    val yearEnded: LocalDate? = null,
    val yearEndedUncertain: Boolean = true,
    val url: String? = null,
) : DataModel(), FilterModel {
    override val tagName: String
        get() = "Publisher"

    override val compareValue: String
        get() = publisher

    override val id: Int
        get() = publisherId

    override fun toString(): String = publisher

    companion object : FilterType {
        override var displayName: String = context!!.getString(R.string.filter_type_publisher)

        override fun toString(): String = displayName
    }
}

@Entity
data class BondType(
    @PrimaryKey(autoGenerate = true) val bondTypeId: Int = AUTO_ID,
    val name: String,
    val description: String,
    val notes: String? = null,
) : DataModel() {
    override val id: Int
        get() = bondTypeId
}

@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Series::class,
            parentColumns = arrayOf("seriesId"),
            childColumns = arrayOf("origin"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Series::class,
            parentColumns = arrayOf("seriesId"),
            childColumns = arrayOf("target"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Issue::class,
            parentColumns = arrayOf("issueId"),
            childColumns = arrayOf("originIssue"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Issue::class,
            parentColumns = arrayOf("issueId"),
            childColumns = arrayOf("targetIssue"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BondType::class,
            parentColumns = arrayOf("bondTypeId"),
            childColumns = arrayOf("bondType"),
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["origin"]),
        Index(value = ["target"]),
        Index(value = ["originIssue"]),
        Index(value = ["targetIssue"]),
        Index(value = ["bondType"]),
    ]
)
data class SeriesBond(
    @PrimaryKey(autoGenerate = true) val bondId: Int = AUTO_ID,
    val origin: Int,
    val target: Int,
    val originIssue: Int?,
    val targetIssue: Int?,
    val bondType: Int,
    val notes: String?,
) : DataModel() {
    override val id: Int
        get() = bondId
}

@ExperimentalCoroutinesApi
data class FullSeries(
    @Embedded
    val series: Series = Series(),

    @Relation(parentColumn = "publisher", entityColumn = "publisherId")
    val publisher: Publisher = Publisher(),

    @Relation(parentColumn = "firstIssue", entityColumn = "issueId", entity = Issue::class)
    val firstIssue: FullIssue? = null,

    @Relation(parentColumn = "seriesId", entityColumn = "origin", entity = SeriesBond::class)
    val seriesBondTo: Bond? = null,

    @Relation(parentColumn = "seriesId", entityColumn = "target", entity = SeriesBond::class)
    val seriesBondFrom: Bond? = null,

    @Relation(parentColumn = "firstIssue", entityColumn = "issue", entity = Cover::class)
    val firstIssueCover: Cover? = null
) : ListItem, FilterModel {
    override val tagName: String
        get() = "Series"

    override val compareValue: String
        get() = series.sortName ?: series.seriesName

    override fun toString(): String = "${series.seriesName} ${series.dateRange}"
}

// it might be tempting to remove SeriesAndPublisher and just use FullSeries, but S&P is
// used in an Issue POJO where the FullSeries can't be: firstIssue can create a circular reference
@ExperimentalCoroutinesApi
data class SeriesAndPublisher(
    @Embedded
    val series: Series,

    @Relation(parentColumn = "publisher", entityColumn = "publisherId")
    val publisher: Publisher,
)

@ExperimentalCoroutinesApi
data class Bond(
    @Embedded
    val seriesBond: SeriesBond,

    @Relation(parentColumn = "target", entityColumn = "seriesId")
    val targetSeries: Series,

    @Relation(parentColumn = "origin", entityColumn = "seriesId")
    val originSeries: Series,
)