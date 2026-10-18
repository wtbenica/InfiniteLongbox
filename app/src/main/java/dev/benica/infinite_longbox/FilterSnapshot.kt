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

package dev.benica.infinite_longbox

import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.database.models.Creator
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.database.models.Publisher
import dev.benica.infinite_longbox.database.models.TextFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

/**
 * A [SearchFilter] as text that survives the app being killed. The filter itself holds whole
 * records (a series with its first issue, creators, ...), which can't be saved in a bundle; what
 * identifies it is only their ids, plus the plain settings. [restore] looks the records up again.
 */
@ExperimentalCoroutinesApi
object FilterSnapshot {

    fun save(filter: SearchFilter): String = JSONObject().apply {
        put("series", filter.mSeries?.series?.seriesId ?: JSONObject.NULL)
        put("creators", JSONArray(filter.mCreators.map { it.creatorId }))
        put("publishers", JSONArray(filter.mPublishers.map { it.publisherId }))
        put("character", filter.mCharacter?.characterId ?: JSONObject.NULL)
        if (filter.hasDateFilter()) {
            put("start", filter.mStartDate.toString())
            put("end", filter.mEndDate.toString())
        }
        put("myCollection", filter.mMyCollection)
        put("showVariants", filter.mShowVariants)
        put("text", filter.mTextFilter?.text ?: JSONObject.NULL)
        filter.mSortType?.let {
            put("sortTag", it.tag)
            put("sortOrder", it.order.name)
        }
        put("view", filter.viewOptionIndex)
    }.toString()

    /**
     * Rebuilds the filter [save] described, or null if it can't be: unreadable text, or a record
     * it names is gone.
     */
    suspend fun restore(
        snapshot: String,
        loadSeries: suspend (Int) -> FullSeries?,
        loadCreator: suspend (Int) -> Creator?,
        loadPublisher: suspend (Int) -> Publisher?,
        loadCharacter: suspend (Int) -> Character?,
    ): SearchFilter? = try {
        val json = JSONObject(snapshot)

        val series = if (json.isNull("series")) null
        else loadSeries(json.getInt("series")) ?: return null

        val creators = json.getJSONArray("creators").ints()
            .map { loadCreator(it) ?: return null }.toSet()
        val publishers = json.getJSONArray("publishers").ints()
            .map { loadPublisher(it) ?: return null }.toSet()

        val character = if (json.isNull("character")) null
        else loadCharacter(json.getInt("character")) ?: return null

        SearchFilter(
            creators = creators,
            series = series,
            publishers = publishers,
            startDate = json.optString("start").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
            endDate = json.optString("end").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
            myCollection = json.getBoolean("myCollection"),
            textFilter = if (json.isNull("text")) null else TextFilter(json.getString("text")),
            showVariants = json.getBoolean("showVariants"),
            character = character,
            viewOptionsIndex = json.getInt("view"),
        ).also { filter ->
            // The sort has to be chosen from this view's options, which depend on the rest
            json.optString("sortTag").takeIf { it.isNotEmpty() }?.let { tag ->
                filter.getSortOptions().firstOrNull { it.tag == tag }?.let { option ->
                    filter.mSortType = SortType(option).apply {
                        order = SortType.SortOrder.valueOf(json.getString("sortOrder"))
                    }
                }
            }
        }
    } catch (e: JSONException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun JSONArray.ints(): List<Int> = List(length()) { getInt(it) }
}
