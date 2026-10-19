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
import dev.benica.infinite_longbox.LetterBucket
import dev.benica.infinite_longbox.LetterIndex
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

    // PAGING SOURCE FUNCTIONS
    @Transaction
    @RawQuery(observedEntities = [FullSeries::class])
    abstract fun getSeriesByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullSeries>

    fun getSeriesByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullSeries> {
        val query = getSeriesQuery(filter)

        return getSeriesByQueryPagingSource(query)
    }

    // SUSPEND FUNCTIONS
    @Transaction
    @RawQuery(observedEntities = [FullSeries::class])
    abstract suspend fun getSeriesByQuerySus(query: SupportSQLiteQuery): List<FullSeries>

    @RawQuery
    abstract suspend fun getLetterBuckets(query: SupportSQLiteQuery): List<LetterBucket>

    /** Where each letter starts in the list [filter] gives, for the letter strip. */
    suspend fun getLetterIndex(filter: SearchFilter): LetterIndex = LetterIndex(
        getLetterBuckets(getSeriesQuery(filter, letterBuckets = true)),
        descending = filter.mSortType?.order == SortType.SortOrder.DESC
    )

    /** A bounded list of series for the filter-box popup. */
    suspend fun getSeriesOptions(filter: SearchFilter, limit: Int): List<FullSeries> =
        getSeriesByQuerySus(getSeriesQuery(filter, limit))

    companion object {
        private fun getSeriesQuery(
            filter: SearchFilter,
            limit: Int? = null,
            letterBuckets: Boolean = false,
        ): SimpleSQLiteQuery {

            val table = StringBuilder()
            val conditions = StringBuilder()
            val args: ArrayList<Any> = arrayListOf()
            fun connectWord(): String = if (conditions.isEmpty()) "WHERE" else "AND"

            // The cover a card falls back to is part of the row, so that when it arrives the row
            // has changed and the list redraws it. The letter counts do not need it.
            val fallbackCover = if (letterBuckets) "" else """, (
                        SELECT cv.coverUri
                        FROM cover cv
                        JOIN issue ce ON ce.issueId = cv.issue
                        WHERE ce.series = ss.seriesId AND cv.coverUri IS NOT NULL
                        ORDER BY (ce.variantOf IS NOT NULL), ce.sortCode
                        LIMIT 1
                    ) AS earliestCoverUri"""
            table.append(
                """SELECT DISTINCT ss.*$fallbackCover
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

            if (letterBuckets) {
                // The same rows as the list, counted by what letter they start with
                val buckets = SortType.letterBucketSql("sortName").format("($table$conditions)")
                return SimpleSQLiteQuery(
                    "SELECT bucket, COUNT(*) AS n FROM ($buckets) GROUP BY bucket",
                    args.toArray()
                )
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

            val orderClause =
                if (limit != null)
                    SortType.optionsOrderBy("ss.seriesName", filter.mTextFilter?.text, args)
                else sortClause
            val limitClause = limit?.let { " LIMIT $it" } ?: ""

            return SimpleSQLiteQuery(
                "$table$conditions$orderClause$limitClause",
                args.toArray()
            )
        }

    }
}

@ExperimentalCoroutinesApi
@Dao
abstract class BondTypeDao : BaseDao<BondType>("bondtype")

@ExperimentalCoroutinesApi
@Dao
abstract class SeriesBondDao : BaseDao<SeriesBond>("seriesbond")