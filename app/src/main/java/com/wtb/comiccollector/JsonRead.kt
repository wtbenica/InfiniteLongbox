package com.wtb.comiccollector

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// TODO: do all these val need to be nullable?
class Item<T : GcdJson<U>, U>(
    @SerializedName("model")
    @Expose
    val model: String?,
    @SerializedName("pk")
    @Expose
    val pk: Int,
    @SerializedName("fields")
    @Expose
    val fields: T,
) {
    override fun toString(): String {
        return "$pk " + fields.toString()
    }

    fun toRoomModel(): U {
        return fields.toRoomModel(this)
    }
}

interface GcdJson<U> {
    fun <T : GcdJson<U>> toRoomModel(item: Item<T, U>): U
}

class GcdSeriesJson(
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

enum class PublisherKey(val pos: Int) {
    ID(0), NAME(1)
}

class GcdPublisherJson(
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
    val name: String,
    @SerializedName("sort_code")
    @Expose
    val sortCode: Int
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

class GcdStoryCredit(
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
    override fun <T : GcdJson<Credit>> toRoomModel(item: Item<T, Credit>): Credit {
        return Credit(
            creditId = item.pk,
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
    override fun <T : GcdJson<Creator>> toRoomModel(item: Item<T, Creator>): Creator {
        return Creator(
            creatorId = item.pk,
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
    override fun <T : GcdJson<Story>> toRoomModel(item: Item<T, Story>): Story {
        return Story(
            storyId = item.pk,
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
    override fun <T : GcdJson<StoryType>> toRoomModel(item: Item<T, StoryType>): StoryType {
        return StoryType(
            typeId = item.pk,
            name = name,
            sortCode = sortCode
        )
    }
}