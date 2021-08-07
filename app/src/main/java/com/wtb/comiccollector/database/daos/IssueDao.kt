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
import com.wtb.comiccollector.SortType
import com.wtb.comiccollector.SortType.Companion.containsSortType
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
        Log.d(TAG, "${query.sql} ${query.toString()}")
        return getFullIssuesByQuery(query)
    }

    // PAGING SOURCE FUNCTIONS
    @RawQuery(observedEntities = [FullIssue::class])
    abstract fun getFullIssuesByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullIssue>

    fun getIssuesByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullIssue> {
        val query = createIssueQuery(filter)
        Log.d(TAG, "getIssuesByFilterPagingSource")
        Log.d(TAG, "${query.sql} ${query.toString()}")
        return getFullIssuesByQueryPagingSource(query)
    }

    // SUSPEND FUNCTIONS
    @RawQuery(observedEntities = [FullIssue::class])
    abstract suspend fun getFullIssuesByQuerySus(query: SupportSQLiteQuery): List<FullIssue>

    suspend fun getIssuesByFilterSus(filter: SearchFilter): List<FullIssue> {
        val query = createIssueQuery(filter)
        Log.d(TAG, "getIssuesByFilterSus")
        Log.d(TAG, "${query.sql} ${query.toString()}")
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
            val tableJoinString = StringBuilder()
            val conditionsString = StringBuilder()
            val args: ArrayList<Any> = arrayListOf()
            fun connectword(): String = if (conditionsString.isEmpty()) "WHERE" else "AND"

            tableJoinString.append("""SELECT DISTINCT ie.* 
                        FROM issue ie 
                        LEFT JOIN cover cr ON ie.issueId = cr.issue 
                        LEFT JOIN story sy on sy.issue = ie.issueId 
                        """)

            filter.mSeries?.let {
                conditionsString.append("""${connectword()} ie.series = ? 
                """)
                args.add(it.seriesId)
            }

            if (filter.hasPublisher()) {
                tableJoinString.append("""JOIN series ss ON ie.series = ss.seriesId 
                """)

                if (filter.mPublishers.isNotEmpty()) {
                    val publisherList = modelsToSqlIdString(filter.mPublishers)

                    conditionsString.append("""${connectword()} ss.publisher IN $publisherList 
                    """)
                }
            }

            if (filter.mCreators.isNotEmpty()) {
                val creatorsList = modelsToSqlIdString(filter.mCreators)

                conditionsString.append("""${connectword()} (sy.storyId IN (
                SELECT story
                FROM credit ct
                JOIN namedetail nl on nl.nameDetailId = ct.nameDetail
                WHERE nl.creator IN $creatorsList)
                OR sy.storyId IN (
                SELECT story
                FROM excredit ect
                JOIN namedetail nl on nl.nameDetailId = ect.nameDetail
                WHERE nl.creator IN $creatorsList)) 
            """)
            }

            if (filter.hasDateFilter()) {
                conditionsString.append("""${connectword()} ss.startDate < ? 
                    ${connectword()} ss.endDate > ? """)
                args.add(filter.mEndDate)
                args.add(filter.mStartDate)
            }

            if (filter.mMyCollection) {
                conditionsString.append("""${connectword()} ie.issueId IN (
                    SELECT issue
                    FROM mycollection) 
                """)
            }

            if (filter.hasCharacter()) {
                tableJoinString.append("""LEFT JOIN appearance ap ON ap.story = sy.storyId 
                """)

                filter.mCharacter?.let {
                    conditionsString.append("""${connectword()} ap.character = ? 
                    """)
                    args.add(it.characterId)
                }
            }

            if (!filter.mShowVariants) {
                conditionsString.append("${connectword()} ie.variantOf IS NULL "
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
                "ORDER BY ${sortString}"
            } ?: ""

            return SimpleSQLiteQuery(
                "$tableJoinString$conditionsString$sortClause",
                args.toArray()
            )
        }
    }
}