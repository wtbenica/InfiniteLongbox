package com.wtb.comiccollector

import android.util.Log
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// TODO: do all these val need to be nullable?
class Item<G : GcdJson<M>, M : DataModel>(
    @SerializedName("model")
    @Expose
    val model: String?,
    @SerializedName("pk")
    @Expose
    val pk: Int,
    @SerializedName("fields")
    @Expose
    val fields: G,
) {
    override fun toString(): String {
        return "$pk " + fields.toString()
    }

    fun toRoomModel(): M {
        return fields.toRoomModel(this.pk)
    }
}

interface GcdJson<M : DataModel> {
    fun toRoomModel(pk: Int): M
}

class GcdSeries(
    @SerializedName("name")
    @Expose
    val name: String?,
    @SerializedName("sort_name")
    @Expose
    val sortName: String?,
    @SerializedName("year_began")
    @Expose
    val yearBegan: Int?,
    @SerializedName("year_began_uncertain")
    @Expose
    val yearBeganUncertain: Int?,
    @SerializedName("year_ended")
    @Expose
    val yearEnded: Int?,
    @SerializedName("year_ended_uncertain")
    @Expose
    val yearEndedUncertain: Int?,
    @SerializedName("publication_dates")
    @Expose
    val publicationDates: String?,
    @SerializedName("publisher")
    @Expose
    val publisherInfo: Array<String>?,
    @SerializedName("publication_format")
    @Expose
    val publishingFormat: String?
) : GcdJson<Series> {
    override fun toString(): String {
        return "$name ($yearBegan - $yearEnded)"
    }

    override fun toRoomModel(pk: Int): Series {
        return Series(
            seriesId = pk,
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

enum class PublisherKey(val pos: Int) {
    ID(0), NAME(1)
}

class GcdPublisher(
    @SerializedName("name")
    @Expose
    val name: String,
    @SerializedName("year_began")
    @Expose
    val yearBegan: Int?,
    @SerializedName("year_ended")
    @Expose
    val yearEnded: Int?
) : GcdJson<Publisher> {
    override fun toRoomModel(pk: Int): Publisher {
        return Publisher(
            publisherId = pk,
            publisher = name,
        )
    }
}

class GcdRole(
    @SerializedName("name")
    @Expose
    val name: String,
    @SerializedName("sort_code")
    @Expose
    val sortCode: Int
) : GcdJson<Role> {
    override fun toRoomModel(pk: Int): Role {
        return Role(
            roleId = pk,
            roleName = name,
            sortOrder = sortCode
        )
    }
}

class GcdIssue(
    @SerializedName("number")
    @Expose
    val number: String,
    @SerializedName("series")
    @Expose
    val seriesId: Int,
    @SerializedName("sort_code")
    @Expose
    val sortCode: Int,
    @SerializedName("editing")
    @Expose
    val editing: String,
    @SerializedName("publication_date")
    @Expose
    val publicationDate: String?,
    @SerializedName("key_date")
    @Expose
    val onSaleDate: String,
    @SerializedName("no_barcode")
    @Expose
    val noBarcode: Int,
    @SerializedName("barcode")
    @Expose
    val barcode: String,
    @SerializedName("variant_name")
    @Expose
    val variantName: String,
    @SerializedName("variant_of")
    @Expose
    val variantOf: Int?
) : GcdJson<Issue> {
    override fun toRoomModel(pk: Int): Issue {
        return Issue(
            issueId = pk,
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

class GcdCredit(
    @SerializedName("creator")
    @Expose
    val creatorId: Array<String>,
    @SerializedName("credit_type")
    @Expose
    val roleId: Array<String>,
    @SerializedName("story")
    @Expose
    val storyId: Int
) : GcdJson<Credit> {
    override fun toRoomModel(pk: Int): Credit {
        Log.d(
            "GcdCredit",
            "pk: ${pk} sid: $storyId cid: ${creatorId[CreatorKey.ID.pos].toInt()} " +
                    "rid: ${roleId[RoleKey.ID.pos].toInt()}"
        )
        return Credit(
            creditId = pk,
            storyId = storyId,
            creatorId = creatorId[CreatorKey.ID.pos].toInt(),
            roleId = roleId[RoleKey.ID.pos].toInt(),
        )
    }

    fun getCreatorModel(): Creator {
        return Creator(
            creatorId = creatorId[CreatorKey.ID.pos].toInt(),
            name = creatorId[CreatorKey.NAME.pos],
            sortName = creatorId[CreatorKey.SORT_NAME.pos]
        )
    }
}

enum class RoleKey(val pos: Int) {
    ID(0), NAME(1)
}

class GcdCreator(
    @SerializedName("gcd_official_name")
    @Expose
    val name: String,
    @SerializedName("sort_name")
    @Expose
    val sortName: String,
) : GcdJson<Creator> {
    override fun toRoomModel(pk: Int): Creator {
        return Creator(
            creatorId = pk,
            name = name,
            sortName = sortName
        )
    }
}

enum class CreatorKey(val pos: Int) {
    ID(0), NAME(1), SORT_NAME(2)
}

class GcdStory(
    @SerializedName("title")
    @Expose
    val title: String,
    @SerializedName("feature")
    @Expose
    val feature: String,
    @SerializedName("sequence_number")
    @Expose
    val sequenceNumber: Int,
    @SerializedName("issue")
    @Expose
    val issueId: Int,
    @SerializedName("script")
    @Expose
    val script: String,
    @SerializedName("pencils")
    @Expose
    val pencils: String,
    @SerializedName("inks")
    @Expose
    val inks: String,
    @SerializedName("colors")
    @Expose
    val colors: String,
    @SerializedName("letters")
    @Expose
    val letters: String,
    @SerializedName("editing")
    @Expose
    val editing: String,
    @SerializedName("characters")
    @Expose
    val characters: String,
    @SerializedName("synopsis")
    @Expose
    val synopsis: String,
    @SerializedName("notes")
    @Expose
    val notes: String,
    @SerializedName("type")
    @Expose
    val typeId: Int
) : GcdJson<Story> {
    override fun toRoomModel(pk: Int): Story {
        return Story(
            storyId = pk,
            storyType = typeId,
            title = title,
            feature = feature,
            characters = characters,
            synopsis = synopsis,
            notes = notes,
            sequenceNumber = 0,
            issueId = issueId
        )
    }
}

class GcdStoryType(
    @SerializedName("name")
    @Expose
    val name: String,
    @SerializedName("sort_code")
    @Expose
    val sortCode: Int
) : GcdJson<StoryType> {
    override fun toRoomModel(pk: Int): StoryType {
        return StoryType(
            typeId = pk,
            name = name,
            sortCode = sortCode
        )
    }
}