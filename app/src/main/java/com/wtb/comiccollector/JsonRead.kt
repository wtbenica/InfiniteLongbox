package com.wtb.comiccollector

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

// TODO: do all these var need to be nullable?
class JsonRead {
    class Item(
        @SerializedName("model")
        @Expose
        var model: String?,
        @SerializedName("pk")
        @Expose
        var pk: Int,
        @SerializedName("fields")
        @Expose
        var fields: Fields?,
    ) {
        class Fields(
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
        )
    }
}
