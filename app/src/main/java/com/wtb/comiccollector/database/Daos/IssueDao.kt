package com.wtb.comiccollector.database.Daos

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.models.Cover
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.Issue

private const val TAG = APP + "IssueDao"

@Dao
abstract class IssueDao : BaseDao<Issue>() {
    @Query("SELECT * FROM issue WHERE issueId=:issueId")
    abstract fun getIssue(issueId: Int): LiveData<Issue?>

    @Transaction
    @Query(
        """SELECT ie.*  
        FROM issue ie 
        JOIN series ss ON ie.seriesId = ss.seriesId 
        JOIN publisher pr ON ss.publisherId = pr.publisherId
        LEFT JOIN mycollection mn ON ie.issueId = mn.issueId
        LEFT JOIN cover cr ON ie.issueId = cr.coverId
        WHERE ie.issueId=:issueId"""
    )
    abstract suspend fun getIssueSus(issueId: Int): FullIssue?

    @Query("SELECT * FROM issue WHERE issueId=:issueId OR variantOf=:issueId ORDER BY sortCode")
    abstract suspend fun getVariants(issueId: Int): List<Issue>

    @Transaction
    @Query(
        "SELECT ie.* " +
                "FROM issue ie " +
                "JOIN series ss ON ie.seriesId = ss.seriesId " +
                "JOIN publisher pr ON ss.publisherId = pr.publisherId " +
                "WHERE issueId = :issueId"
    )
    abstract fun getFullIssue(issueId: Int): LiveData<FullIssue?>

    @RawQuery(
        observedEntities = [FullIssue::class]
    )
    abstract fun getFullIssuesByQuery(query: SupportSQLiteQuery): PagingSource<Int, FullIssue>

    fun getIssuesByFilter(filter: Filter): PagingSource<Int, FullIssue> {

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
        } else {
            conditionsString += "AND ie.variantOf IS NULL "
        }

        val query = SimpleSQLiteQuery(
            tableJoinString + conditionsString + "ORDER BY ${filter.mSortOption.sortColumn}",
            args.toArray()
        )

        Log.d(TAG, tableJoinString + conditionsString)
        Log.d(TAG, query.sql)
        return getFullIssuesByQuery(query)
    }

    @RawQuery(
        observedEntities = [FullIssue::class]
    )
    abstract fun getFullIssuesByQuery2(query: SupportSQLiteQuery): LiveData<List<FullIssue>>

    fun getIssuesByFilter2(filter: Filter): LiveData<List<FullIssue>> {

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
        } else {
            conditionsString += "AND ie.variantOf IS NULL "
        }

        val query = SimpleSQLiteQuery(
            tableJoinString + conditionsString + "ORDER BY ${filter.mSortOption.sortColumn}",
            args.toArray()
        )

        Log.d(TAG, tableJoinString + conditionsString)
        Log.d(TAG, query.sql)
        return getFullIssuesByQuery2(query)
    }

    @Transaction
    @Query(
        """
            SELECT ie.*
            FROM issue ie
            JOIN series ss ON ss.seriesId = ie.seriesId
            JOIN publisher pr ON pr.publisherId = ss.publisherId
            WHERE ss.seriesId=:seriesId
            AND ie.variantOf IS NULL
            """
    )
    abstract fun getIssuesBySeries(seriesId: Int): LiveData<List<FullIssue>>
}

@Dao
abstract class CoverDao : BaseDao<Cover>() {

    @Query("SELECT cr.* FROM cover cr WHERE cr.issueId = :issueId")
    abstract fun getCoverByIssueId(issueId: Int): LiveData<Cover?>
}