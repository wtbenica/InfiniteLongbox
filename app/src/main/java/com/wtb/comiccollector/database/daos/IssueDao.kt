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
import com.wtb.comiccollector.database.models.*
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
                        LEFT JOIN cover cr ON ie.issueId = cr.issueId 
                        LEFT JOIN story sy on sy.issueId = ie.issueId 
                        """)

            filter.mSeries?.let {
                conditionsString.append("""${connectword()} ie.seriesId = ? 
                """)
                args.add(it.seriesId)
            }

            if (filter.hasPublisher()) {
                tableJoinString.append("""JOIN series ss ON ie.seriesId = ss.seriesId 
                """)

                if (filter.mPublishers.isNotEmpty()) {
                    val publisherList = modelsToSqlIdString(filter.mPublishers)

                    conditionsString.append("""${connectword()} ss.publisherId IN $publisherList 
                    """)
                }
            }

            if (filter.mTextFilter?.type in listOf(All, Publisher)) {
                tableJoinString.append("""JOIN publisher pr ON ss.publisherId = pr.publisherId 
                """)
            }

            if (filter.mCreators.isNotEmpty()) {
                val creatorsList = modelsToSqlIdString(filter.mCreators)

                conditionsString.append("""${connectword()} (sy.storyId IN (
                SELECT storyId
                FROM credit ct
                JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId
                WHERE nl.creatorId IN $creatorsList)
                OR sy.storyId IN (
                SELECT storyId
                FROM excredit ect
                JOIN namedetail nl on nl.nameDetailId = ect.nameDetailId
                WHERE nl.creatorId IN $creatorsList)) 
            """)
            }

            if (filter.mTextFilter?.type in listOf(All, NameDetail)) {
                tableJoinString.append("""LEFT JOIN credit ct on ct.storyId = sy.storyId 
                            LEFT JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId 
                            LEFT JOIN excredit ect on ect.storyId = sy.storyId
                            LEFT JOIN namedetail nl2 on nl2.nameDetailId = ect.nameDetailId """)
            }

            if (filter.hasDateFilter()) {
                conditionsString.append("""${connectword()} ss.startDate < ? 
                    ${connectword()} ss.endDate > ? """)
                args.add(filter.mEndDate)
                args.add(filter.mStartDate)
            }

            if (filter.mMyCollection) {
                conditionsString.append("""${connectword()} ie.issueId IN (
                    SELECT issueId
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

                if (filter.mTextFilter?.type in listOf(All, Character)) {
                    tableJoinString.append("""LEFT JOIN character ch ON ch.characterId = ap.character 
                    """)
                }
            }

            if (!filter.mShowVariants) {
                conditionsString.append("${connectword()} ie.variantOf IS NULL "
                )
            }

            val sortClause: String = filter.mSortType?.let {
                val isValid =
                    SortType.Companion.SortTypeOptions.ISSUE.options.containsSortType(it)
//                it !in SortType.Companion.SortTypeOptions.ISSUE.options
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