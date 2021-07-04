package com.wtb.comiccollector.database.Daos

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
abstract class IssueDao : BaseDao<Issue>() {
    // FLOW FUNCTIONS
    @RawQuery(observedEntities = [FullIssue::class])
    abstract fun getFullIssuesByQuery(query: SupportSQLiteQuery): Flow<List<FullIssue>>

    fun getIssuesByFilter(filter: SearchFilter): Flow<List<FullIssue>> {
        var tableJoinString = String()
        var conditionsString = String()
        val args: ArrayList<Any> = arrayListOf()
        var containsCondition = false

        tableJoinString +=
            """SELECT DISTINCT ie.*, ss.seriesName, pr.publisher 
                FROM issue ie 
                JOIN series ss ON ie.seriesId = ss.seriesId 
                JOIN publisher pr ON ss.publisherId = pr.publisherId 
                LEFT JOIN cover cr ON ie.issueId = cr.issueId """

        conditionsString +=
            "WHERE ss.seriesId = ${filter.mSeries?.seriesId} "

        if (filter.hasPublisher()) {
            val publisherList = modelsToSqlIdString(filter.mPublishers)

            conditionsString += "AND pr.publisherId IN $publisherList "
        }

        if (filter.hasCreator()) {
            tableJoinString +=
                "JOIN story sy on sy.issueId = ie.issueId " +
                        "JOIN credit ct on ct.storyId = sy.storyId " +
                        "JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId "

            val creatorsList = modelsToSqlIdString(filter.mCreators)

            conditionsString += "AND nl.creatorId IN $creatorsList "
        }

        if (filter.hasDateFilter()) {
            conditionsString += "AND ss.startDate < ? AND ss.endDate > ? "
            args.add(filter.mEndDate)
            args.add(filter.mStartDate)
        }

        if (filter.mMyCollection) {
            tableJoinString += "JOIN mycollection mc ON mc.issueId = ie.issueId "
        }

        if (!filter.mShowVariants) {
            conditionsString += "AND ie.variantOf IS NULL "
        }

        val query = SimpleSQLiteQuery(
            tableJoinString + conditionsString + "ORDER BY ${filter.mSortType.sortString}",
            args.toArray()
        )

        return getFullIssuesByQuery(query)
    }

    @Query(
        """SELECT ie.* FROM issue ie
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
        JOIN series ss ON ie.seriesId = ss.seriesId 
        JOIN publisher pr ON ss.publisherId = pr.publisherId
        LEFT JOIN mycollection mn ON ie.issueId = mn.issueId
        LEFT JOIN cover cr ON ie.issueId = cr.issueId
        WHERE ie.issueId=:issueId"""
    )
    abstract fun getFullIssue(issueId: Int): Flow<FullIssue?>

    // PAGING SOURCE FUNCTIONS
    @RawQuery(observedEntities = [FullIssue::class])
    abstract fun getFullIssuesByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullIssue>

    fun getIssuesByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullIssue> {
        val mSeries = filter.mSeries

        var tableJoinString = String()
        var conditionsString = String()
        val args: ArrayList<Any> = arrayListOf()
        var containsCondition = false

        tableJoinString +=
            "SELECT DISTINCT ie.*, ss.seriesName, pr.publisher " +
                    "FROM issue ie " +
                    "JOIN series ss ON ie.seriesId = ss.seriesId " +
                    "JOIN publisher pr ON ss.publisherId = pr.publisherId "

        conditionsString +=
            "WHERE ss.seriesId = ${mSeries?.seriesId} "

        if (filter.hasPublisher()) {
            val publisherList = modelsToSqlIdString(filter.mPublishers)

            conditionsString += "AND pr.publisherId IN $publisherList "
        }

        if (filter.hasCreator()) {
            tableJoinString +=
                "JOIN story sy on sy.issueId = ie.issueId " +
                        "LEFT JOIN credit ct on ct.storyId = sy.storyId " +
                        "LEFT JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId " +
                        "LEFT JOIN excredit ect on ect.storyId = sy.storyId " +
                        "LEFT JOIN namedetail nl2 on nl2.nameDetailId = ect.nameDetailId "

            val creatorsList = modelsToSqlIdString(filter.mCreators)

            conditionsString += "AND (nl.creatorId IN $creatorsList " +
                    "OR nl2.creatorId IN $creatorsList) "
        }

        if (filter.hasDateFilter()) {
            conditionsString += "AND ss.startDate < ? AND ss.endDate > ? "
            args.add(filter.mEndDate)
            args.add(filter.mStartDate)
        }

        if (filter.mMyCollection) {
            tableJoinString += "JOIN mycollection mc ON mc.issueId = ie.issueId "
        }

        if (!filter.mShowVariants) {
            conditionsString += "AND ie.variantOf IS NULL "
        }

        val query = SimpleSQLiteQuery(
            tableJoinString + conditionsString + "ORDER BY ${filter.mSortType.sortString} ",
            args.toArray()
        )

        return getFullIssuesByQueryPagingSource(query)
    }

    // SUSPEND FUNCTIONS
    @Transaction
    @Query(
        """SELECT ie.*  
        FROM issue ie 
        JOIN series ss ON ie.seriesId = ss.seriesId 
        JOIN publisher pr ON ss.publisherId = pr.publisherId
        LEFT JOIN mycollection mn ON ie.issueId = mn.issueId
        LEFT JOIN cover cr ON ie.issueId = cr.issueId
        WHERE ie.issueId=:issueId"""
    )
    abstract suspend fun getIssueSus(issueId: Int): FullIssue?
}