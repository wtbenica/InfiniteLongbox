package com.wtb.comiccollector.database.Daos

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.MyCollection
import com.wtb.comiccollector.database.models.Series
import java.util.*

private const val TAG = APP + "CollectionDao"

@Dao
abstract class CollectionDao : BaseDao<MyCollection>() {

    @Transaction
    @RawQuery(
        observedEntities = [FullIssue::class]
    )
    abstract fun getFullIssuesByQuery(query: SupportSQLiteQuery): DataSource.Factory<Int, FullIssue>


    fun getIssuesByFilter(filter: Filter): DataSource.Factory<Int, FullIssue> {
        var tableJoinString = String()
        var conditionsString = String()
        val args: ArrayList<Any> = arrayListOf()
        var containsCondition = false

        tableJoinString +=
            "SELECT DISTINCT ie.*, ss.seriesName, pr.publisher " +
                    "FROM issue ie " +
                    "JOIN series ss ON ie.seriesId = ss.seriesId " +
                    "JOIN publisher pr ON ss.publisherId = pr.publisherId " +
                    "JOIN mycollection mn ON ie.issueId = mn.issueId "

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

        return getFullIssuesByQuery(query)
    }

    @Transaction
    @RawQuery(
        observedEntities = [Series::class]
    )
    abstract fun getSeriesByQuery(query: SupportSQLiteQuery): DataSource.Factory<Int, Series>

    fun getSeriesByFilter(filter: Filter): DataSource.Factory<Int, Series> {
        var tableJoinString = String()
        var conditionsString = String()
        val args: ArrayList<Any> = arrayListOf()
        var containsCondition = false

        tableJoinString +=
            "SELECT DISTINCT ss.* " +
                    "FROM series ss " +
                    "JOIN issue ie ON ie.seriesId = ss.seriesId " +
                    "JOIN mycollection mn ON mn.issueId = ie.issueId "

        if (filter.hasPublisher()) {
            tableJoinString += "JOIN publisher pr ON ss.publisherId = pr.publisherId "

            val publisherList = filter.mPublishers.map { it.publisherId }.toString().replace(
                "[",
                "("
            ).replace("]", ")")

            conditionsString += "WHERE pr.publisherId IN $publisherList "
            containsCondition = true
        }

        if (filter.hasCreator()) {
            if (containsCondition) {
                conditionsString += "AND "
            } else {
                conditionsString += "WHERE "
                containsCondition = true
            }

            tableJoinString +=
                "JOIN story sy on sy.issueId = ie.issueId " +
                        "JOIN credit ct on ct.storyId = sy.storyId " +
                        "JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId "

            val creatorsList = filter.mCreators.map { it.creatorId }.toString().replace(
                "[", "" +
                        "("
            ).replace("]", ")")

            conditionsString += "nl.creatorId IN $creatorsList "
        }

        if (filter.hasDateFilter()) {
            conditionsString += if (containsCondition) {
                "AND "
            } else {
                "WHERE "
            }

            conditionsString += "ss.startDate < ? AND ss.endDate > ? "
            args.add(filter.mEndDate)
            args.add(filter.mStartDate)
        }

        val query = SimpleSQLiteQuery(
            tableJoinString + conditionsString + "ORDER BY ${filter.mSortOption.sortColumn}",
            args.toArray()
        )

        return getSeriesByQuery(query)
    }

    @Query(
        """
            SELECT COUNT(*) as count
            FROM mycollection mc
            WHERE mc.issueId = :issueId
        """
    )
    abstract fun inCollection(issueId: Int): LiveData<Count>

    @Query(
        """
            DELETE FROM mycollection
            WHERE issueId = :issueId
        """
    )
    abstract fun deleteById(issueId: Int)
}

data class Count(
    val count: Int
)