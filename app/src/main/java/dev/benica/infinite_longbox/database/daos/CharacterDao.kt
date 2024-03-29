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

package dev.benica.infinite_longbox.database.daos

import android.util.Log
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.SortType
import dev.benica.infinite_longbox.SortType.Companion.containsSortType
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.database.models.FullCharacter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.util.*
import dev.benica.infinite_longbox.database.models.Character as Character

@ExperimentalCoroutinesApi
@Dao
abstract class CharacterDao : BaseDao<Character>("character") {

    @Query("SELECT * FROM character ORDER BY name ASC")
    abstract fun getAll(): Flow<List<Character>>

    @Transaction
    @Query("SELECT * FROM character WHERE characterId=:characterId")
    abstract fun getCharacter(characterId: Int): Flow<List<FullCharacter>?>

    // FLOW FUNCTIONS
    @RawQuery(observedEntities = [Character::class])
    abstract fun getCharacterByQuery(query: SupportSQLiteQuery): Flow<List<Character>>

    fun getCharacterFilterOptions(filter: SearchFilter): Flow<List<Character>> {
        val query = getCharacterQuery(filter)

        return getCharacterByQuery(query)
    }

    // PAGING SOURCE FUNCTIONS
    @RawQuery(observedEntities = [Character::class])
    abstract fun getCharactersByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullCharacter>

    fun getCharactersByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullCharacter> {
        val query = getCharacterQuery(filter)

        return getCharactersByQueryPagingSource(query)
    }

    private fun getCharacterQuery(filter: SearchFilter): SimpleSQLiteQuery {
        val tableJoinString = StringBuilder()
        val conditionsString = StringBuilder()
        val args: ArrayList<Any> = arrayListOf()

        fun connectWord(): String = if (conditionsString.isEmpty()) "WHERE" else "AND"

        tableJoinString.append(
            """SELECT DISTINCT ch.* 
            FROM character ch 
            """
        )

        if (filter.hasDateFilter() || filter.hasCharacter() || filter.hasCreator() || filter
                .hasSeries() || filter.mMyCollection
        ) {
            tableJoinString.append(
                """JOIN appearance ap ON ap.character = ch.characterId 
                JOIN issue ie ON ap.issue = ie.issueId 
                """
            )
        }


        if (filter.hasDateFilter()) {
            conditionsString.append(
                """${connectWord()} ie.releaseDate <= '${filter.mEndDate}'
                AND ie.releaseDate > '${filter.mStartDate}'
                """
            )
        }

        if (filter.mPublishers.isNotEmpty()) {
            val publisherList = modelsToSqlIdString(filter.mPublishers)

            conditionsString.append(
                """${connectWord()} ch.publisher IN $publisherList  
                """
            )
        }

        if (filter.hasCharacter()) {
            tableJoinString.append(
                """JOIN story sy2 ON sy2.storyId = ap.story
                """
            )

            conditionsString.append(
                """${connectWord()} sy2.storyId IN (
                    SELECT story
                    FROM appearance
                    WHERE character = ${filter.mCharacter!!.id}
                )
                """
            )

            conditionsString.append(
                """${connectWord()} ch.characterId != ${filter.mCharacter!!.id}
                """
            )
        }

        if (filter.hasCreator()) {
            val creatorsList = modelsToSqlIdString(filter.mCreators)

            tableJoinString.append(
                """JOIN story sy ON sy.storyId = ap.story 
                """
            )

            conditionsString.append(
                """${connectWord()} (
                    sy.storyId IN (
                        SELECT story
                        FROM credit ct
                        JOIN namedetail nl on nl.nameDetailId = ct.nameDetail
                        WHERE nl.creator IN $creatorsList
                    )
                    OR sy.storyId IN (
                        SELECT story
                        FROM excredit ect
                        JOIN namedetail nl on nl.nameDetailId = ect.nameDetail
                        WHERE nl.creator IN $creatorsList
                    )
                )
                """
            )
        }

        filter.mSeries?.let {
            conditionsString.append(
                """${connectWord()} ie.series = ? 
                """
            )

            args.add(it.series.seriesId)
        }

        if (filter.mMyCollection) {
            conditionsString.append(
                """${connectWord()} ie.issueId IN (
                    SELECT issue
                    FROM collectionItem
                    WHERE userCollection = ?
                ) 
                """
            )
            args.add(BaseCollection.MY_COLL.id)
        }

        filter.mTextFilter?.let { textFilter ->
            val text = textFilterToString(textFilter.text)
            conditionsString.append(
                """${connectWord()} (ch.name LIKE ?
                OR ch.alterEgo LIKE ?)
                """
            )

            args.addAll(listOf(text, text))
        }

        val sortClause: String = filter.mSortType?.let {
            val isValid =
                SortType.Companion.SortTypeOptions.CHARACTER.options.containsSortType(it)
            val sortString: String =
                if (isValid) {
                    it.sortString
                } else {
                    SortType.Companion.SortTypeOptions.CHARACTER.options[0].sortString
                }
            "ORDER BY $sortString"
        } ?: ""

        val simpleSQLiteQuery = SimpleSQLiteQuery(
            "$tableJoinString$conditionsString$sortClause", args.toArray()
        )
        Log.d(TAG, "XYZ ${simpleSQLiteQuery.sql}")
        return simpleSQLiteQuery
    }

    companion object {
        private const val TAG = APP + "CharacterDao"

    }
}
