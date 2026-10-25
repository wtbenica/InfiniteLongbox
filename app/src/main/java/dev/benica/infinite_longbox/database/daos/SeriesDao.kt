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
import dev.benica.infinite_longbox.filter.LetterBucket
import dev.benica.infinite_longbox.filter.LetterIndex
import dev.benica.infinite_longbox.filter.SearchFilter
import dev.benica.infinite_longbox.filter.SortType
import dev.benica.infinite_longbox.filter.SortType.Companion.containsSortType
import dev.benica.infinite_longbox.database.models.*
import dev.benica.infinite_longbox.repository.DUMMY_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.util.*

@Suppress("unused")
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
    @RawQuery(observedEntities = [FullSeries::class, CollectionItem::class])
    abstract fun getSeriesByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullSeries>

    /** For a filter that joins stories, credits or appearances: those tables fill in while it is shown. */
    @Transaction
    @RawQuery(observedEntities = [FullSeries::class, Story::class, Credit::class, ExCredit::class, NameDetail::class, Creator::class, Appearance::class, Character::class, CollectionItem::class])
    abstract fun getSeriesByQueryPagingSourceJoined(query: SupportSQLiteQuery): PagingSource<Int, FullSeries>

    fun getSeriesByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullSeries> {
        val query = getSeriesQuery(filter)

        return if (filter.hasCreator() || filter.hasCharacter()) getSeriesByQueryPagingSourceJoined(query)
        else getSeriesByQueryPagingSource(query)
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

            // The fallback cover (first in issue order, a variant after its issue) is part of the row; the list redraws when it arrives
            val fallbackCover = """(
                        SELECT cv.coverUri
                        FROM cover cv
                        JOIN issue ce ON ce.issueId = cv.issue
                        WHERE ce.series = ss.seriesId AND cv.coverUri IS NOT NULL
                        ORDER BY ce.sortCode
                        LIMIT 1
                    ) AS earliestCoverUri"""
            // Joined with issues, a series is a row for each of them (and the sort may be by their columns), so the cover goes in the row itself
            val joinsIssues = filter.hasCreator() || filter.hasCharacter() || (filter.mMyCollection && filter.hasDateFilter())
            val coverInRow = joinsIssues && !letterBuckets
            table.append(
                """SELECT DISTINCT ss.*${if (coverInRow) ", $fallbackCover" else ""}
                        FROM series ss 
                        """
            )

            conditions.append("${connectWord()} ss.seriesId != $DUMMY_ID ")

            // A collection alone needs no issues: the series with a copy are picked without joining them
            if (joinsIssues) {
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
                if (joinsIssues) {
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

                args.add(filter.mCollectionId)

                // Series with at least one copy lacking a cover, or a place in storage
                if (filter.mMissingCover) {
                    conditions.append(
                        """${connectWord()} ss.seriesId IN (
                    SELECT mci.series FROM collectionItem mci
                    WHERE mci.userCollection = ?
                    AND mci.issue NOT IN (SELECT issue FROM cover WHERE coverUri IS NOT NULL)) 
                """
                    )
                    args.add(filter.mCollectionId)
                }
                if (filter.mMissingStorage) {
                    conditions.append(
                        """${connectWord()} ss.seriesId IN (
                    SELECT mci.series FROM collectionItem mci
                    WHERE mci.userCollection = ? AND (mci.storageLocation IS NULL OR mci.storageLocation = '')) 
                """
                    )
                    args.add(filter.mCollectionId)
                }
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

            // Without joins the cover is looked up for the rows that stay, after the paging picks them
            val query = if (joinsIssues || letterBuckets) "$table$conditions$orderClause$limitClause"
            else "SELECT ss.*, $fallbackCover FROM ($table$conditions) ss $orderClause$limitClause"
            return SimpleSQLiteQuery(query, args.toArray())
        }

    }
}

@ExperimentalCoroutinesApi
@Dao
abstract class BondTypeDao : BaseDao<BondType>("bondtype")

@ExperimentalCoroutinesApi
@Dao
abstract class SeriesBondDao : BaseDao<SeriesBond>("seriesbond")