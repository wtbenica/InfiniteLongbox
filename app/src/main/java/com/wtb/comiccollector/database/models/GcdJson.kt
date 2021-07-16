package com.wtb.comiccollector.database.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

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
        return "$pk $fields"
    }

    fun toRoomModel(): M {
        return fields.toRoomModel(this.pk)
    }
}

interface GcdJson<M : DataModel> {
    fun toRoomModel(pk: Int): M
}

@ExperimentalCoroutinesApi
class GcdSeries(
    @SerializedName("name")
    @Expose
    val name: String,
    @SerializedName("sort_name")
    @Expose
    val sortName: String,
    @SerializedName("year_began")
    @Expose
    val yearBegan: Int?,
    @SerializedName("year_began_uncertain")
    @Expose
    val yearBeganUncertain: Int,
    @SerializedName("year_ended")
    @Expose
    val yearEnded: Int?,
    @SerializedName("year_ended_uncertain")
    @Expose
    val yearEndedUncertain: Int,
    @SerializedName("publication_dates")
    @Expose
    val publicationDates: String,
    @SerializedName("publisher")
    @Expose
    val publisher: Int?,
    @SerializedName("publishing_format")
    @Expose
    val publishingFormat: String,
    @SerializedName("tracking_notes")
    @Expose
    val trackingNotes: String,
    @SerializedName("first_issue")
    @Expose
    val firstIssueId: Int?,
    @SerializedName("notes")
    @Expose
    val notes: String,
    @SerializedName("issue_count")
    @Expose
    val issueCount: Int
) : GcdJson<Series> {
    override fun toString(): String {
        return "$name ($yearBegan - $yearEnded)"
    }

    override fun toRoomModel(pk: Int): Series {
        return Series(
            seriesId = pk,
            seriesName = name,
            sortName = sortName,
            publisherId = publisher ?: AUTO_ID,
            startDate = when (yearBeganUncertain) {
                0    -> LocalDate.of(
                    yearBegan ?: LocalDate.MIN.year,
                    1,
                    1
                )
                else -> null
            },
            endDate = when (yearEndedUncertain) {
                0    -> yearEnded?.let {
                    LocalDate.of(
                        it,
                        1,
                        1
                    )
                }
                else -> null
            },
            publishingFormat = if (publishingFormat == "") null else publishingFormat,
            description = if (trackingNotes == "") null else trackingNotes,
            firstIssueId = firstIssueId,
            notes = if (notes == "") null else notes,
            issueCount = issueCount
        )
    }
}

@ExperimentalCoroutinesApi
class GcdPublisher(
    @SerializedName("name")
    @Expose
    val name: String,
    @SerializedName("year_began")
    @Expose
    val yearBegan: Int?,
    @SerializedName("year_began_uncertain")
    @Expose
    val yearBeganUncertain: Int,
    @SerializedName("year_ended")
    @Expose
    val yearEnded: Int?,
    @SerializedName("year_ended_uncertain")
    @Expose
    val yearEndedUncertain: Int,
    @SerializedName("url")
    @Expose
    var url: String
) : GcdJson<Publisher> {
    override fun toRoomModel(pk: Int): Publisher {
        return Publisher(
            publisherId = pk,
            publisher = name,
            yearBegan = if (yearBegan != null) {
                LocalDate.of(
                    yearBegan,
                    1,
                    1
                )
            } else {
                null
            },
            yearBeganUncertain = when (yearBeganUncertain) {
                1    -> true
                else -> false
            },
            yearEnded = if (yearEnded != null) {
                LocalDate.of(
                    yearEnded ?: LocalDate.MIN.year,
                    1,
                    1
                )
            } else {
                null
            },
            yearEndedUncertain = when (yearEndedUncertain) {
                1    -> true
                else -> false
            },
            url = if (url == "") null else url
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

@ExperimentalCoroutinesApi
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
    val publicationDate: String,
    @SerializedName("on_sale_date")
    @Expose
    val onSaleDate: String,
    @SerializedName("on_sale_date_uncertain")
    @Expose
    val onSaleDateUncertain: Int,
    @SerializedName("key_date")
    @Expose
    val keyDate: String,
    @SerializedName("notes")
    @Expose
    val notes: String,
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
    val variantOf: Int?,
) : GcdJson<Issue> {
    override fun toRoomModel(pk: Int): Issue {
        val fixedPubDate = if (publicationDate == "") null else publicationDate
        val fixedNotes = if (notes == "") null else notes
        return Issue(
            issueId = pk,
            seriesId = seriesId,
            issueNum = number.toIntOrNull() ?: 1,
            releaseDate = Issue.formatDate(onSaleDate),
            upc = barcode.toLongOrNull(),
            variantName = variantName,
            variantOf = variantOf,
            sortCode = sortCode,
            coverDateLong = fixedPubDate,
            onSaleDateUncertain = onSaleDateUncertain == 1,
            coverDate = Issue.formatDate(keyDate),
            notes = fixedNotes
        )
    }
}

class GcdCredit(
    @SerializedName("creator")
    @Expose
    val nameDetailId: Int,
    @SerializedName("credit_type")
    @Expose
    val roleId: Int,
    @SerializedName("story")
    @Expose
    val storyId: Int
) : GcdJson<Credit> {
    override fun toRoomModel(pk: Int): Credit {
        return Credit(
            creditId = pk,
            storyId = storyId,
            nameDetailId = nameDetailId,
            roleId = roleId,
        )
    }
}

class GcdExCredit(
    @SerializedName("creator")
    @Expose
    val nameDetailId: Int,
    @SerializedName("credit_type")
    @Expose
    val roleId: Int,
    @SerializedName("story")
    @Expose
    val storyId: Int
) : GcdJson<ExCredit> {
    override fun toRoomModel(pk: Int): ExCredit {
        return ExCredit(
            creditId = pk,
            storyId = storyId,
            nameDetailId = nameDetailId,
            roleId = roleId,
        )
    }
}

enum class RoleKey(val pos: Int) {
    ID(0), NAME(1)
}

@ExperimentalCoroutinesApi
class GcdCreator(
    @SerializedName("gcd_official_name")
    @Expose
    val name: String,
    @SerializedName("sort_name")
    @Expose
    val sortName: String,
    @SerializedName("bio")
    @Expose
    val bio: String,
) : GcdJson<Creator> {
    override fun toRoomModel(pk: Int): Creator {
        return Creator(
            creatorId = pk,
            name = name,
            sortName = sortName,
            bio = if (bio == "") null else bio
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
            sequenceNumber = sequenceNumber,
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

@ExperimentalCoroutinesApi
class GcdNameDetail(
    @SerializedName("creator")
    @Expose
    val creatorId: Int,
    @SerializedName("name")
    @Expose
    val name: String,
    @SerializedName("sort_name")
    @Expose
    val sortName: String
) : GcdJson<NameDetail> {
    override fun toRoomModel(pk: Int): NameDetail {
        return NameDetail(
            nameDetailId = pk,
            creatorId = creatorId,
            name = name,
            sortName = if (sortName == "") null else sortName
        )
    }
}