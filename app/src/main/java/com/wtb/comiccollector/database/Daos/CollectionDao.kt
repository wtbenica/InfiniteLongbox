package com.wtb.comiccollector.database.Daos

import android.util.Log
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
    @Query(
        """
            SELECT DISTINCT ss.* 
            FROM series ss
            JOIN issue ie ON ie.seriesId = ss.seriesId
            JOIN mycollection mc ON mc.issueId = ie.issueId
            ORDER BY ss.sortName ASC
            """
    )
    abstract fun getAllSeries(): LiveData<List<Series>>

    @RawQuery(
        observedEntities = arrayOf(
            Series::class
        )
    )
    abstract fun getSeriesByQuery(query: SupportSQLiteQuery): DataSource.Factory<Int, Series>

    fun getSeriesByFilter(filter: Filter): DataSource.Factory<Int, Series> {
        Log.d(
            TAG,
            "Series By Filter: ${filter.hasDateFilter()} ${filter.hasPublisher()} ${filter.hasCreator()}"
        )
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
            if (containsCondition) {
                conditionsString += "AND "
            } else {
                conditionsString += "WHERE "
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
            SELECT DISTINCT ie.*, ss.seriesName, pr.publisher
            FROM issue ie 
            JOIN mycollection mc ON mc.issueId = ie.issueId
            JOIN story sy ON sy.issueId = ie.issueId
            JOIN credit ct ON ct.storyId = sy.storyId
            JOIN series ss ON ss.seriesId = ie.seriesId
            JOIN publisher pr ON pr.publisherId = ss.publisherId
            JOIN namedetail nl ON nl.nameDetailId = ct.nameDetailId
            WHERE nl.creatorId in (:creatorIds) 
            AND ss.seriesId = :seriesId
            ORDER BY ie.sortCode"""
    )
    abstract fun getIssuesBySeriesCreator(
        seriesId: Int,
        creatorIds: List<Int>
    ): LiveData<List<FullIssue>>

    @Query(
        """
            SELECT ie.*, ss.seriesName, pr.publisher 
            FROM issue ie
            JOIN mycollection mc ON mc.issueId = ie.issueId
            JOIN series ss ON ss.seriesId = ie.seriesId
            JOIN publisher pr ON pr.publisherId = ss.publisherId
            WHERE ss.seriesId=:seriesId
            """
    )

    abstract fun getIssuesBySeries(seriesId: Int): LiveData<List<FullIssue>>

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