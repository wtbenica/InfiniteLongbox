package com.wtb.comiccollector.database.daos

import android.util.Log
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.Issue
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
        Log.d(TAG, "getIssuesByFilter")
        Log.d(TAG, "ISSUE QUERY----------------------------------------------------------")
        Log.d(TAG, query.sql)
        return getFullIssuesByQuery(query)
    }

    // PAGING SOURCE FUNCTIONS
    @RawQuery(observedEntities = [FullIssue::class])
    abstract fun getFullIssuesByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullIssue>

    fun getIssuesByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullIssue> {
        val query = createIssueQuery(filter)
        Log.d(TAG, "getIssuesByFilterPagingSource")
        return getFullIssuesByQueryPagingSource(query)
    }

    // SUSPEND FUNCTIONS
    @RawQuery(observedEntities = [FullIssue::class])
    abstract suspend fun getFullIssuesByQuerySus(query: SupportSQLiteQuery): List<FullIssue>

    suspend fun getIssuesByFilterSus(filter: SearchFilter): List<FullIssue> {
        val query = createIssueQuery(filter)
        Log.d(TAG, "getIssuesByFilterSus")
        return getFullIssuesByQuerySus(query)
    }

    @Query(
        """SELECT ie.* 
            FROM issue ie
            JOIN issue ie2 ON ie2.issueId = ie.issueId
            WHERE ie.issueId=:issueId 
            OR ie.variantOf=:issueId 
            OR (ie.issueId=:issueId
            AND (ie2.issueId = ie.variantOf
            OR ie2.variantOf = ie.variantOf ))
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

    companion object {
        private fun createIssueQuery(filter: SearchFilter): SimpleSQLiteQuery {
            var tableJoinString = String()
            var conditionsString = String()
            val args: ArrayList<Any> = arrayListOf()
            fun connectword(): String = if (conditionsString.isEmpty()) "WHERE" else "AND"

            tableJoinString +=
                """SELECT DISTINCT ie.* 
                        FROM issue ie 
                        JOIN series ss ON ie.seriesId = ss.seriesId 
                        JOIN publisher pr ON ss.publisherId = pr.publisherId 
                        LEFT JOIN cover cr ON ie.issueId = cr.issueId 
                        LEFT JOIN story sy on sy.issueId = ie.issueId """

            filter.mSeries?.let {
                conditionsString +=
                    "${connectword()} ss.seriesId = ${it.seriesId} "
            }

            if (filter.hasPublisher()) {
                val publisherList = modelsToSqlIdString(filter.mPublishers)

                conditionsString += "${connectword()} pr.publisherId IN $publisherList "
            }

            if (filter.hasCreator()) {
                tableJoinString +=
                    """LEFT JOIN credit ct on ct.storyId = sy.storyId 
                                LEFT JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId 
                                LEFT JOIN excredit ect on ect.storyId = sy.storyId 
                                LEFT JOIN namedetail nl2 on nl2.nameDetailId = ect.nameDetailId """

                val creatorsList = modelsToSqlIdString(filter.mCreators)

                conditionsString += "${connectword()} (nl.creatorId IN $creatorsList " +
                        "OR nl2.creatorId IN $creatorsList) "
            }

            if (filter.hasDateFilter()) {
                conditionsString += "${connectword()} ss.startDate < ? AND ss.endDate > ? "
                args.add(filter.mEndDate)
                args.add(filter.mStartDate)
            }

            if (filter.mMyCollection) {
                tableJoinString += "JOIN mycollection mc ON mc.issueId = ie.issueId "
            }

            filter.mCharacter?.let {
                tableJoinString += """JOIN appearance ap ON ap.story = sy.storyId """

                conditionsString += "${connectword()} ap.character = ? "

                args.add(it.characterId)
            }

            if (!filter.mShowVariants) {
                conditionsString += "${connectword()} ie.variantOf IS NULL "
            }

            val sortClause: String = filter.mSortType?.let { "ORDER BY ${it.sortString}" } ?: ""

            return SimpleSQLiteQuery(
                tableJoinString + conditionsString + sortClause,
                args.toArray()
            )
        }
    }
}