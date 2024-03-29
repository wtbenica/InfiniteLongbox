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
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.Issue
import dev.benica.infinite_longbox.database.models.ids
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "IssueDao"

@ExperimentalCoroutinesApi
@Dao
abstract class IssueDao : BaseDao<Issue>("issue") {

    // FLOW FUNCTIONS
    @RawQuery(observedEntities = [FullIssue::class])
    abstract fun getFullIssuesByQuery(query: SupportSQLiteQuery): Flow<List<FullIssue>>

    fun getIssuesByFilter(filter: SearchFilter): Flow<List<FullIssue>> {
        val query = createIssueQuery(filter)
        Log.d(TAG, "this is the query: ${filter.mSortType?.order} ${query.sql}")
        return getFullIssuesByQuery(query)
    }

    // PAGING SOURCE FUNCTIONS
    @RawQuery(observedEntities = [FullIssue::class])
    abstract fun getFullIssuesByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullIssue>

    fun getIssuesByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullIssue> {
        val query = createIssueQuery(filter)

        return getFullIssuesByQueryPagingSource(query)
    }

    // SUSPEND FUNCTIONS
    @RawQuery(observedEntities = [FullIssue::class])
    abstract suspend fun getFullIssuesByQuerySus(query: SupportSQLiteQuery): List<FullIssue>

    suspend fun getIssuesByFilterSus(filter: SearchFilter): List<FullIssue> {
        val query = createIssueQuery(filter)

        return getFullIssuesByQuerySus(query)
    }

    @Query(
        """SELECT ie.* 
            FROM issue ie
            JOIN issue ie2 ON ie2.issueId = ie.issueId
            WHERE ie.issueId=:issueId 
            OR ie.variantOf=:issueId 
            ORDER BY sortCode
            """
    )
    abstract fun getVariants(issueId: Int): Flow<List<Issue>>

    @Transaction
    @Query(
        """SELECT ie.*  
        FROM issue ie 
        WHERE ie.issueId=:issueId"""
    )
    abstract fun getFullIssue(issueId: Int): Flow<FullIssue?>

    @Transaction
    @Query(
        """SELECT ie.*  
        FROM issue ie 
        WHERE ie.issueId=:issueId"""
    )
    abstract suspend fun getIssueSus(issueId: Int): FullIssue?

    @Query(
        """
            DELETE FROM issue
            WHERE 0 = 0
        """
    )
    abstract fun dropAll()

    companion object {
        private fun createIssueQuery(filter: SearchFilter): SimpleSQLiteQuery {
            val tableJoinString = StringBuilder()
            val conditionsString = StringBuilder()
            val args: ArrayList<Any> = arrayListOf()
            fun connectword(): String = if (conditionsString.isEmpty()) "WHERE" else "AND"

            tableJoinString.append(
                """SELECT DISTINCT ie.* 
                        FROM issue ie 
                        """
            )

            filter.mSeries?.let {
                conditionsString.append(
                    """${connectword()} ie.series = ? 
                    """
                )
                args.add(it.series.seriesId)
            }

            if (filter.hasPublisher()) {
                tableJoinString.append(
                    """JOIN series ss ON ie.series = ss.seriesId 
                    """
                )

                if (filter.mPublishers.isNotEmpty()) {
                    val publisherList = modelsToSqlIdString(filter.mPublishers)

                    conditionsString.append(
                        """${connectword()} ss.publisher IN $publisherList 
                        """
                    )
                }
            }

            if (filter.needsStoryTable) {
                tableJoinString.append(
                    """JOIN story sy on sy.issue = ie.issueId
                    """
                )
            }

            if (filter.hasCreator()) {
                for (creatorId in filter.mCreators.ids) {
                    conditionsString.append(
                        """${connectword()} (
                            sy.storyId IN (
                                SELECT ct.story
                                FROM credit ct
                                WHERE namedetail IN (
                                    SELECT nl.nameDetailId
                                    FROM namedetail nl
                                    WHERE nl.creator = $creatorId
                                )
                            )
                            OR sy.storyId IN (
                                SELECT ect.story
                                FROM excredit ect
                                WHERE namedetail IN (
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

            if (filter.hasCharacter()) {
                tableJoinString.append(
                    """JOIN appearance ap ON ap.story = sy.storyId 
                """
                )

                filter.mCharacter?.let {
                    conditionsString.append(
                        """${connectword()} ap.character = ? 
                    """
                    )
                    args.add(it.characterId)
                }
            }

            if (filter.hasDateFilter()) {
                conditionsString.append(
                    """${connectword()} ((ie.releaseDate <= '${filter.mEndDate}' 
                    AND ie.releaseDate >= '${filter.mStartDate}'
                    AND ie.releaseDate IS NOT NULL)
                    OR (ie.coverDate <= '${filter.mEndDate}'
                    AND ie.coverDate >= '${filter.mStartDate}'
                    AND ie.releaseDate IS NULL))
                    """
                )
            }

            if (filter.mMyCollection) {
                conditionsString.append(
                    """${connectword()} ie.issueId IN (
                    SELECT issue
                    FROM collectionItem
                    WHERE userCollection = ?) 
                """
                )
                args.add(BaseCollection.MY_COLL.id)
            }

            if (!filter.mShowVariants) {
                conditionsString.append(
                    "${connectword()} ie.variantOf IS NULL "
                )
            }

            // TODO: what does a text filter mean in an issue list?
            filter.mTextFilter?.let { textFilter ->
                Log.d(TAG, "*************************************************************")
                Log.d(TAG, "*************************************************************")
                Log.d(TAG, "*************************************************************")
                Log.d(TAG, "SERIOUS TODO, BUT STILL WANT IT TO RUN FOR THE MOMENT!")
                Log.d(TAG, "*************************************************************")
                Log.d(TAG, "*************************************************************")
                Log.d(TAG, "*************************************************************")
            }

            val sortClause: String = filter.mSortType?.let {
                val isValid =
                    SortType.Companion.SortTypeOptions.ISSUE.options.containsSortType(it)
                val sortString: String =
                    if (isValid) {
                        it.sortString
                    } else {
                        SortType.Companion.SortTypeOptions.ISSUE.options[0].sortString
                    }
                "ORDER BY $sortString"
            } ?: ""

            return SimpleSQLiteQuery(
                "$tableJoinString$conditionsString$sortClause",
                args.toArray()
            )
        }
    }
}