package com.wtb.comiccollector.database.Daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.repository.DUMMY_ID
import kotlinx.coroutines.flow.Flow
import java.util.*

private const val TAG = APP + "SeriesDao"

@Dao
abstract class SeriesDao : BaseDao<Series>() {
    // FLOW FUNCTIONS
    @RawQuery(
        observedEntities = [Series::class]
    )
    abstract fun getSeriesByQuery(query: SupportSQLiteQuery): Flow<List<Series>>

    fun getSeriesByFilter(filter: Filter): Flow<List<Series>> {

        var tableJoinString = String()
        var conditionsString = String()
        val args: ArrayList<Any> = arrayListOf()

        tableJoinString +=
            "SELECT DISTINCT ss.* " +
                    "FROM series ss "

        conditionsString += "WHERE ss.seriesId != $DUMMY_ID "

        if (filter.hasPublisher()) {
            tableJoinString += "JOIN publisher pr ON ss.publisherId = pr.publisherId "

            val publisherList = modelsToSqlIdString(filter.mPublishers)

            conditionsString += "AND pr.publisherId IN $publisherList "
        }

        if (filter.hasCreator()) {
            tableJoinString +=
                "JOIN issue ie on ie.seriesId = ss.seriesId " +
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
            tableJoinString += "JOIN issue ie2 on ie2.seriesId = ss.seriesId " +
                    "JOIN mycollection mc ON mc.issueId = ie2.issueId "
        }

        val query = SimpleSQLiteQuery(
            tableJoinString + conditionsString + "ORDER BY ${filter.mSortOption.sortColumn}",
            args.toArray()
        )

        return getSeriesByQuery(query)
    }

    @Query("SELECT * FROM series WHERE seriesId=:seriesId")
    abstract fun getSeries(seriesId: Int): Flow<Series?>

    @Query("SELECT * FROM series")
    abstract fun getAllOfThem(): Flow<List<Series>>

    // PAGING SOURCE FUNCITONS
    @RawQuery(observedEntities = [Series::class])
    abstract fun getSeriesByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullSeries>

    fun getSeriesByFilterPagingSource(filter: Filter): PagingSource<Int, FullSeries> {

        var tableJoinString = String()
        var conditionsString = String()
        val args: ArrayList<Any> = arrayListOf()

        tableJoinString +=
            "SELECT DISTINCT ss.* " +
                    "FROM series ss "

        conditionsString += "WHERE ss.seriesId != $DUMMY_ID "

        if (filter.hasPublisher()) {
            tableJoinString += "JOIN publisher pr ON ss.publisherId = pr.publisherId "

            val publisherList = modelsToSqlIdString(filter.mPublishers)

            conditionsString += "AND pr.publisherId IN $publisherList "
        }

        if (filter.hasCreator()) {
            tableJoinString +=
                """JOIN issue ie on ie.seriesId = ss.seriesId 
                    JOIN story sy on sy.issueId = ie.issueId 
                    LEFT JOIN credit ct on ct.storyId = sy.storyId 
                    LEFT JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId 
                    LEFT JOIN excredit ect on ect.storyId = sy.storyId
                    LEFT JOIN namedetail nl2 on nl2.nameDetailId = ect.nameDetailId """

            val creatorsList = modelsToSqlIdString(filter.mCreators)

            conditionsString +=
                """AND (nl.creatorId IN $creatorsList 
                OR nl2.creatorId in $creatorsList)"""
        }

        if (filter.hasDateFilter()) {
            conditionsString += "AND ss.startDate < ? AND ss.endDate > ? "
            args.add(filter.mEndDate)
            args.add(filter.mStartDate)
        }

        if (filter.mMyCollection) {
            tableJoinString += "JOIN issue ie2 on ie2.seriesId = ss.seriesId " +
                    "JOIN mycollection mc ON mc.issueId = ie2.issueId "
        }

        val query = SimpleSQLiteQuery(
            tableJoinString + conditionsString + "ORDER BY ${filter.mSortOption.sortColumn}",
            args.toArray()
        )

        return getSeriesByQueryPagingSource(query)
    }

}