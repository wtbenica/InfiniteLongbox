package com.wtb.comiccollector

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// TODO: do all these var need to be nullable?
class Item<T : GcdJson<U>, U>(
    @SerializedName("model")
    @Expose
    var model: String?,
    @SerializedName("pk")
    @Expose
    var pk: Int,
    @SerializedName("fields")
    @Expose
    var fields: T?,
) {
    override fun toString(): String {
        return "$pk " + fields.toString()
    }
}

interface GcdJson<U> {
    fun <T: GcdJson<U>> toRoomModel(item: Item<T, U>): U
}

class GcdSeriesJson(
    @SerializedName("name")
    @Expose
    var name: String?,
    @SerializedName("sort_name")
    @Expose
    var sortName: String?,
    @SerializedName("year_began")
    @Expose
    var yearBegan: Int?,
    @SerializedName("year_began_uncertain")
    @Expose
    var yearBeganUncertain: Int?,
    @SerializedName("year_ended")
    @Expose
    var yearEnded: Int?,
    @SerializedName("year_ended_uncertain")
    @Expose
    var yearEndedUncertain: Int?,
    @SerializedName("publication_dates")
    @Expose
    var publicationDates: String?,
    @SerializedName("publisher")
    @Expose
    var publisherInfo: Array<String>?,
    @SerializedName("publication_format")
    @Expose
    var publishingFormat: String?
) : GcdJson<Series> {
    override fun toString(): String {
        return "$name ($yearBegan - $yearEnded)"
    }

    override fun <T : GcdJson<Series>> toRoomModel(item: Item<T, Series>): Series {
        return Series(
            seriesId = item.pk,
            seriesName = name ?: "",
            publisherId = publisherInfo?.get(0)?.toInt() ?: AUTO_ID,
            startDate = when (yearBeganUncertain as Int) {
                0 -> LocalDate.of(
                    yearBegan ?: LocalDate.MIN.year,
                    1,
                    1
                )
                else -> null
            },
            endDate = when (yearEndedUncertain) {
                0 -> yearEnded?.let {
                    LocalDate.of(
                        it,
                        1,
                        1
                    )
                }
                else -> null
            },
            publishingFormat = publishingFormat
        )
    }
}

class GcdPublisherJson(
    @SerializedName("name")
    @Expose
    var name: String,
    @SerializedName("year_began")
    @Expose
    var yearBegan: Int?,
    @SerializedName("year_ended")
    @Expose
    var yearEnded: Int?
) : GcdJson<Publisher> {
    override fun <T : GcdJson<Publisher>> toRoomModel(item: Item<T, Publisher>): Publisher {
        return Publisher(
            publisherId = item.pk,
            publisher = name,
        )
    }
}

class GcdRoleJson(
    @SerializedName("name")
    @Expose
    var name: String,
    @SerializedName("sort_code")
    @Expose
    var sortCode: Int
) : GcdJson<Role> {
    override fun <T : GcdJson<Role>> toRoomModel(item: Item<T, Role>): Role {
        return Role(
            roleId = item.pk,
            roleName = name,
            sortOrder = sortCode
        )
    }
}

class GcdIssueJson(
    @SerializedName("number")
    @Expose
    var number: String,
    @SerializedName("series")
    @Expose
    var seriesId: Int,
    @SerializedName("sort_code")
    @Expose
    var sortCode: Int,
    @SerializedName("editing")
    @Expose
    var editing: String,
    @SerializedName("publication_date")
    @Expose
    var publicationDate: String?,
    @SerializedName("key_date")
    @Expose
    var onSaleDate: String,
    @SerializedName("no_barcode")
    @Expose
    var noBarcode: Int,
    @SerializedName("barcode")
    @Expose
    var barcode: String,
    @SerializedName("variant_name")
    @Expose
    var variantName: String,
    @SerializedName("variant_of")
    @Expose
    var variantOf: Int?
) : GcdJson<Issue> {
    override fun <T : GcdJson<Issue>> toRoomModel(item: Item<T, Issue>): Issue {
        return Issue(
            issueId = item.pk,
            seriesId = seriesId,
            issueNum = number.toIntOrNull() ?: 1,
            releaseDate = if (onSaleDate == "") {
                null
            } else {
                LocalDate.parse(
                    Issue.formatDate(onSaleDate),
                    DateTimeFormatter.ofPattern("uuuu-MM-dd")
                )

            },
            upc = barcode.toLongOrNull(),
            variantName = variantName,
            variantOf = variantOf
        )
    }
}

class GcdIssueCredit(

) : GcdJson<Credit> {
    override fun <T : GcdJson<Credit>> toRoomModel(item: Item<T, Credit>): Credit {
        TODO("Not yet implemented")
    }
}

class GcdCreator(

) : GcdJson<Creator> {
    override fun <T : GcdJson<Creator>> toRoomModel(item: Item<T, Creator>): Creator {
        TODO("Not yet implemented")
    }
}