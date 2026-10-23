/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.infinite_longbox.database.models

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Suppress("unused")
private const val TAG = "GcdJson"

/**
 * One record as the server sends it: Django's serialization of a model, with the key in [pk] and
 * the model's columns in [fields]. Only the columns [G] names are read.
 */
@Serializable
class Item<G : GcdJson<*>>(
    val model: String? = null,
    val pk: Int,
    val fields: G,
) {
    override fun toString(): String {
        return "$pk $fields"
    }
}

val <G : GcdJson<M>, M : DataModel> List<Item<G>>.models: List<M>
    get() = this.map { it.fields.toRoomModel(it.pk) }

val <M : DataModel> Collection<M>.ids: List<Int>
    get() = this.map { it.id }

/**
 * Class model for json object that can convert it to a [DataModel]
 *
 * @param M The resulting [DataModel]
 */
interface GcdJson<M : DataModel> {
    fun toRoomModel(pk: Int): M
}

@ExperimentalCoroutinesApi
@Serializable
class GcdSeries(
    @SerialName("name")
    val name: String,
    @SerialName("sort_name")
    val sortName: String,
    @SerialName("year_began")
    val yearBegan: Int?,
    @SerialName("year_began_uncertain")
    val yearBeganUncertain: Int,
    @SerialName("year_ended")
    val yearEnded: Int?,
    @SerialName("year_ended_uncertain")
    val yearEndedUncertain: Int,
    @SerialName("publication_dates")
    val publicationDates: String,
    @SerialName("publisher")
    val publisher: Int?,
    @SerialName("publishing_format")
    val publishingFormat: String,
    @SerialName("tracking_notes")
    val trackingNotes: String,
    @SerialName("first_issue")
    val firstIssueId: Int?,
    @SerialName("notes")
    val notes: String,
    @SerialName("issue_count")
    val issueCount: Int,
) : GcdJson<Series> {
    override fun toString(): String {
        return "$name ($yearBegan - $yearEnded)"
    }

    override fun toRoomModel(pk: Int): Series {
        return Series(
            seriesId = pk,
            seriesName = name,
            sortName = sortName,
            publisher = publisher ?: AUTO_ID,
            startDate = LocalDate.of(
                yearBegan ?: LocalDate.MIN.year,
                1,
                1
            ),
            endDate = yearEnded?.let {
                LocalDate.of(
                    it,
                    1,
                    1
                )
            },
            publishingFormat = if (publishingFormat == "") null else publishingFormat,
            description = if (trackingNotes == "") null else trackingNotes,
            firstIssue = firstIssueId,
            notes = if (notes == "") null else notes,
            issueCount = issueCount
        )
    }
}

@ExperimentalCoroutinesApi
@Serializable
class GcdPublisher(
    @SerialName("name")
    val name: String,
    @SerialName("year_began")
    val yearBegan: Int?,
    @SerialName("year_began_uncertain")
    val yearBeganUncertain: Int,
    @SerialName("year_ended")
    val yearEnded: Int?,
    @SerialName("year_ended_uncertain")
    val yearEndedUncertain: Int,
    @SerialName("url")
    var url: String,
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
                    yearEnded,
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

@Serializable
class GcdRole(
    @SerialName("name")
    val name: String,
    @SerialName("sort_code")
    val sortCode: Int,
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
@Serializable
class GcdIssue(
    @SerialName("number")
    val number: String,
    @SerialName("series")
    val seriesId: Int,
    @SerialName("sort_code")
    val sortCode: Int,
    @SerialName("editing")
    val editing: String,
    @SerialName("publication_date")
    val publicationDate: String,
    @SerialName("on_sale_date")
    val onSaleDate: String,
    @SerialName("on_sale_date_uncertain")
    val onSaleDateUncertain: Int,
    @SerialName("key_date")
    val keyDate: String,
    @SerialName("notes")
    val notes: String,
    @SerialName("no_barcode")
    val noBarcode: Int,
    @SerialName("barcode")
    val barcode: String,
    @SerialName("variant_name")
    val variantName: String,
    @SerialName("variant_of")
    val variantOf: Int?,
) : GcdJson<Issue> {
    override fun toRoomModel(pk: Int): Issue {
        return Issue(
            issueId = pk,
            series = seriesId,
            issueNum = getIntFromString(number),
            releaseDate = Issue.formatDate(onSaleDate),
            upc = barcode.toLongOrNull(),
            variantName = variantName,
            variantOf = variantOf,
            sortCode = sortCode,
            coverDateLong = if (publicationDate == "") null else publicationDate,
            onSaleDateUncertain = onSaleDateUncertain == 1,
            coverDate = Issue.formatDate(keyDate),
            notes = if (notes == "") null else notes,
            issueNumRaw = number
        )
    }


    private fun getIntFromString(s: String): Int {
        val s1 = s.split(Regex("[\\s(\\[{]"))
        return s1[0].toIntOrNull() ?: 1
    }
}

@ExperimentalCoroutinesApi
@Serializable
class GcdCredit(
    @SerialName("creator")
    val nameDetailId: Int,
    @SerialName("credit_type")
    val roleId: Int,
    @SerialName("story")
    val storyId: Int,
    @SerialName("issue")
    val issue: Int,
    @SerialName("series")
    val series: Int,
) : GcdJson<Credit> {
    override fun toRoomModel(pk: Int): Credit {
        return Credit(
            creditId = pk,
            story = storyId,
            nameDetail = nameDetailId,
            role = roleId,
            issue = issue,
            series = series
        )
    }
}

@ExperimentalCoroutinesApi
@Serializable
class GcdExCredit(
    @SerialName("creator")
    val nameDetailId: Int,
    @SerialName("credit_type")
    val roleId: Int,
    @SerialName("story")
    val storyId: Int,
    @SerialName("issue")
    val issue: Int,
    @SerialName("series")
    val series: Int,
) : GcdJson<ExCredit> {
    override fun toRoomModel(pk: Int): ExCredit {
        return ExCredit(
            creditId = pk,
            story = storyId,
            nameDetail = nameDetailId,
            role = roleId,
            issue = issue,
            series = series
        )
    }
}

@ExperimentalCoroutinesApi
@Serializable
class GcdCreator(
    @SerialName("gcd_official_name")
    val name: String,
    @SerialName("sort_name")
    val sortName: String,
    @SerialName("bio")
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

@Serializable
class GcdStory(
    @SerialName("title")
    val title: String,
    @SerialName("feature")
    val feature: String,
    @SerialName("sequence_number")
    val sequenceNumber: Int,
    @SerialName("issue")
    val issueId: Int,
    @SerialName("script")
    val script: String,
    @SerialName("pencils")
    val pencils: String,
    @SerialName("inks")
    val inks: String,
    @SerialName("colors")
    val colors: String,
    @SerialName("letters")
    val letters: String,
    @SerialName("editing")
    val editing: String,
    @SerialName("characters")
    val characters: String,
    @SerialName("synopsis")
    val synopsis: String,
    @SerialName("notes")
    val notes: String,
    @SerialName("type")
    val typeId: Int,
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
            issue = issueId
        )
    }
}

@Serializable
class GcdStoryType(
    @SerialName("name")
    val name: String,
    @SerialName("sort_code")
    val sortCode: Int,
) : GcdJson<StoryType> {
    override fun toRoomModel(pk: Int): StoryType {
        return StoryType(
            storyTypeId = pk,
            name = name,
            sortCode = sortCode
        )
    }
}

@ExperimentalCoroutinesApi
@Serializable
class GcdNameDetail(
    @SerialName("creator")
    val creatorId: Int,
    @SerialName("name")
    val name: String,
    @SerialName("sort_name")
    val sortName: String,
) : GcdJson<NameDetail> {
    override fun toRoomModel(pk: Int): NameDetail {
        return NameDetail(
            nameDetailId = pk,
            creator = creatorId,
            name = name,
            sortName = if (sortName == "") null else sortName
        )
    }
}

@Serializable
class GcdBondType(
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String,
    @SerialName("notes")
    val notes: String,
) : GcdJson<BondType> {
    override fun toRoomModel(pk: Int): BondType {
        return BondType(
            bondTypeId = pk,
            name = name,
            description = description,
            notes = notes
        )
    }
}

@ExperimentalCoroutinesApi
@Serializable
class GcdSeriesBond(
    @SerialName("origin")
    val origin: Int,
    @SerialName("target")
    val target: Int,
    @SerialName("origin_issue")
    val originIssue: Int? = null,
    @SerialName("target_issue")
    val targetIssue: Int? = null,
    @SerialName("bond_type")
    val bondType: Int,
    @SerialName("notes")
    val notes: String,
) : GcdJson<SeriesBond> {
    override fun toRoomModel(pk: Int): SeriesBond {
        return SeriesBond(
            bondId = pk,
            origin = origin,
            target = target,
            originIssue = originIssue,
            targetIssue = targetIssue,
            bondType = bondType,
            notes = notes
        )
    }
}

@ExperimentalCoroutinesApi
@Serializable
class GcdCharacter(
    @SerialName("name")
    val name: String,
    @SerialName("alter_ego")
    val alterEgo: String?,
    @SerialName("publisher")
    val publisherId: Int,
) : GcdJson<Character> {
    override fun toRoomModel(pk: Int): Character {
        return Character(
            characterId = pk,
            name = name,
            alterEgo = alterEgo,
            publisher = publisherId
        )
    }
}

@ExperimentalCoroutinesApi
@Serializable
class GcdCharacterAppearance(
    @SerialName("details")
    val details: String?,
    @SerialName("character")
    val characterId: Int,
    @SerialName("story")
    val storyId: Int,
    @SerialName("notes")
    val notes: String?,
    @SerialName("membership")
    val membership: String?,
    @SerialName("issue")
    val issue: Int,
    @SerialName("series")
    val series: Int,
) : GcdJson<Appearance> {
    override fun toRoomModel(pk: Int): Appearance {
        return Appearance(
            appearanceId = pk,
            details = details,
            story = storyId,
            character = characterId,
            notes = notes,
            membership = membership,
            issue = issue,
            series = series
        )
    }
}

