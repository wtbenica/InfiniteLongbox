package com.wtb.comiccollector.database.models

import androidx.room.*
import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Publisher::class,
            parentColumns = arrayOf("publisherId"),
            childColumns = arrayOf("publisherId"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["seriesName"]),
        Index(value = ["publisherId"])
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
    var publishingFormat: String? = null
) : FilterOption, Serializable {

    override fun compareTo(other: FilterOption): Int = when (other) {
        is Series -> this.seriesName.compareTo(other.seriesName)
        else -> 1
    }

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
}

@Entity(
    indices = [
        Index(value = ["publisher"]),
    ]
)
data class Publisher(
    @PrimaryKey(autoGenerate = true) val publisherId: Int = AUTO_ID,
    val publisher: String = ""
) : FilterOption {

    override fun compareTo(other: FilterOption): Int =
        when (other) {
            is Series -> -1
            is Publisher -> this.publisher.compareTo(other.publisher)
            else -> 1 // is Creator
        }

    override val id: Int
        get() = publisherId

    override fun toString(): String {
        return publisher
    }
}

data class SeriesAndPublisher(
    @Embedded
    val series: Series,

    @Relation(parentColumn = "publisherId", entityColumn = "publisherId")
    var publisher: Publisher
)