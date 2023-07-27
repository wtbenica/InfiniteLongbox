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
import dev.benica.infinite_longbox.database.models.*
import dev.benica.infinite_longbox.repository.DUMMY_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.util.*

private const val TAG = APP + "SeriesDao"

@ExperimentalCoroutinesApi
@Dao
abstract class SeriesDao : BaseDao<Series>("series") {

    @Query("SELECT * FROM series")
    abstract fun getAll(): Flow<List<Series>>

    @Transaction
    @Query("SELECT * FROM series WHERE seriesId=:seriesId")
    abstract fun getSeries(seriesId: Int): Flow<FullSeries?>

    // FLOW FUNCTIONS
    @RawQuery(observedEntities = [Series::class])
    abstract fun getSeriesByQuery(query: SupportSQLiteQuery): Flow<List<FullSeries>>

    fun getSeriesByFilter(filter: SearchFilter): Flow<List<FullSeries>> {
        val query = getSeriesQuery(filter)

        return getSeriesByQuery(query)
    }

    // PAGING SOURCE FUNCTIONS
    @RawQuery(observedEntities = [FullSeries::class])
    abstract fun getSeriesByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullSeries>

    fun getSeriesByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullSeries> {
        val query = getSeriesQuery(filter)

        return getSeriesByQueryPagingSource(query)
    }

    // SUSPEND FUNCTIONS
    @RawQuery(observedEntities = [FullSeries::class])
    abstract suspend fun getSeriesByQuerySus(query: SupportSQLiteQuery): List<FullSeries>

    suspend fun getSeriesByFilterSus(filter: SearchFilter): List<FullSeries> {
        val query = getSeriesQuery(filter)

        return getSeriesByQuerySus(query)
    }

    companion object {
        private fun getSeriesQuery(filter: SearchFilter): SimpleSQLiteQuery {

            val table = StringBuilder()
            val conditions = StringBuilder()
            val args: ArrayList<Any> = arrayListOf()
            fun connectWord(): String = if (conditions.isEmpty()) "WHERE" else "AND"

            table.append(
                """SELECT DISTINCT ss.* 
                        FROM series ss 
                        """
            )

            conditions.append("${connectWord()} ss.seriesId != $DUMMY_ID ")

            if (filter.hasCreator() || filter.hasCharacter() || filter.mMyCollection
            ) {
                table.append(
                    """JOIN issue ie ON ie.series = ss.seriesId
                    """
                )
            }

            if (filter.hasCreator() || filter.hasCharacter())
                table.append(
                    """JOIN story sy ON sy.issue = ie.issueId
                       """
                )

            if (filter.mPublishers.isNotEmpty()) {
                val publisherList = modelsToSqlIdString(filter.mPublishers)

                conditions.append("""${connectWord()} ss.publisher IN $publisherList """)
            }

            if (filter.hasCreator()) {
                for (creatorId in filter.mCreators.ids) {
                    conditions.append(
                        """${connectWord()} (
                            sy.storyId IN (
                                SELECT ct.story
                                FROM credit ct
                                WHERE ct.nameDetail IN (
                                    SELECT nl.nameDetailId
                                    FROM namedetail nl
                                    WHERE nl.creator = $creatorId
                                )
                            )
                            OR sy.storyId IN (
                                SELECT ect.story
                                FROM excredit ect
                                WHERE ect.nameDetail IN (
                                    SELECT nl.nameDetailId
                                    FROM namedetail nl
                                    WHERE nl.creator = $creatorId
                                )
                            )
                        )
                    """
                    )
                }
            }

            if (filter.hasDateFilter()) {
                if (filter.hasCreator() || filter.hasCharacter() || filter.mMyCollection) {
                    conditions.append(
                        """${connectWord()} ((ie.releaseDate <= '${filter.mEndDate}' 
                        AND ie.releaseDate >= '${filter.mStartDate}'
                        AND ie.releaseDate IS NOT NULL)
                        OR (ie.coverDate <= '${filter.mEndDate}'
                        AND ie.coverDate >= '${filter.mStartDate}'
                        AND ie.releaseDate IS NULL))
                        """
                    )
                } else {
                    conditions.append(
                        """${connectWord()} ss.startDate <= '${filter.mEndDate}' 
                        AND ss.endDate >= '${filter.mStartDate}'
                        """
                    )
                }
            }

            filter.mCharacter?.characterId?.let {
                conditions.append(
                    """${connectWord()} sy.storyId IN (
                        SELECT ap.story
                        FROM appearance ap
                        WHERE ap.character = $it)
                         """
                )
            }

            if (filter.mMyCollection) {
                if (filter.hasCharacter() || filter.hasCreator()) {
                    conditions.append(
                        """${connectWord()} ie.issueId IN (
                    SELECT issue
                    FROM collectionItem
                    WHERE userCollection = ?)
                    """
                    )
                } else {
                    conditions.append(
                        """${connectWord()} ss.seriesId IN (
                    SELECT series
                    FROM collectionItem
                    WHERE userCollection = ?) 
                """
                    )
                }

                args.add(BaseCollection.MY_COLL.id)
            }

            filter.mTextFilter?.let { textFilter ->
                val text = textFilterToString(textFilter.text)

                conditions.append(
                    """${connectWord()} ss.seriesName like ? 
                """
                )

                args.add(text)
            }

            val sortClause: String = filter.mSortType?.let {
                val isValid =
                    if (filter.isComplex)
                        SortType.Companion.SortTypeOptions.SERIES_COMPLEX.options.containsSortType(
                            it
                        )
                    else
                        SortType.Companion.SortTypeOptions.SERIES.options.containsSortType(it)

                val sortString: String = if (isValid) {
                    it.sortString
                } else {
                    SortType.Companion.SortTypeOptions.SERIES.options[0].sortString
                }

                "ORDER BY $sortString"
            } ?: ""

            return SimpleSQLiteQuery(
                "$table$conditions$sortClause",
                args.toArray()
            )
        }

        private fun addTypeFilterElse(
            lookup: List<String?>?,
            textFilter: TextFilter,
            conditions: StringBuilder,
        ) {
            var first = true
            lookup?.forEach {
                addTextFilterCondition(
                    it,
                    first,
                    conditions,
                    textFilter
                )
                    .also { first = false }
            }
        }

        private fun addTextFilterCondition(
            column: String?,
            first: Boolean,
            conditions: StringBuilder,
            textFilter: TextFilter,
        ) {
            if (!first) {
                conditions.append("""OR """)
            }

            conditions.append("""$column like '%${textFilter.text}%' """)
        }
    }
}

@ExperimentalCoroutinesApi
@Dao
abstract class BondTypeDao : BaseDao<BondType>("bondtype")

@ExperimentalCoroutinesApi
@Dao
abstract class SeriesBondDao : BaseDao<SeriesBond>("seriesbond")