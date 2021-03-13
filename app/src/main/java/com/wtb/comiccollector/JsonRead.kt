package com.wtb.comiccollector

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

// TODO: do all these var need to be nullable?
class Item<T : GcdJson>(
    @SerializedName("model")
    @Expose
    var model: String?,
    @SerializedName("pk")
    @Expose
    var pk: Int,
    @SerializedName("fields")
    @Expose
    var fields: T?,
)

interface GcdJson {}

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
    var publisher: Array<String>?,
) : GcdJson

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
) : GcdJson

class GcdRoleJson(
    @SerializedName("name")
    @Expose
    var name: String,
    @SerializedName("sort_code")
    @Expose
    var sortCode: Int
) : GcdJson