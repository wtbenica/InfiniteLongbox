package com.wtb.comiccollector.database.Daos

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.models.Series
import java.util.*

private const val TAG = APP + "SeriesDao"

@Dao
abstract class SeriesDao : BaseDao<Series>() {

    @Query("SELECT * FROM series WHERE seriesId=:seriesId")
    abstract fun getSeries(seriesId: Int): LiveData<Series?>

    @RawQuery(
        observedEntities = arrayOf(
            Series::class
        )
    )
    abstract fun getSeriesByQuery(query: SupportSQLiteQuery): DataSource.Factory<Int, Series>

    @Query("SELECT * FROM series")
    abstract fun getAllOfThem(): LiveData<List<Series>>

    fun getSeriesByFilter(filter: Filter): DataSource.Factory<Int, Series> {
        Log.d(
            TAG,
            "Series By Filter: ${filter.hasDateFilter()} ${filter.hasPublisher()} $filter.hasCreator()}"
        )
        var tableJoinString = String()
        var conditionsString = String()
        val args: ArrayList<Any> = arrayListOf()
        var containsCondition = false

        tableJoinString +=
            "SELECT DISTINCT ss.* " +
                    "FROM series ss "

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
                "JOIN issue ie on ie.seriesId = ss.seriesId " +
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
}