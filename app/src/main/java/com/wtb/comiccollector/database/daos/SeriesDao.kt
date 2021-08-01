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
import com.wtb.comiccollector.database.models.BondType
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.database.models.SeriesBond
import com.wtb.comiccollector.repository.DUMMY_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.util.*

private const val TAG = APP + "SeriesDao"

@ExperimentalCoroutinesApi
@Dao
abstract class SeriesDao : BaseDao<Series>("series") {

    @Query("SELECT * FROM series")
    abstract fun getAll(): Flow<List<Series>>

    @Transaction
    @Query("SELECT * FROM series WHERE seriesId=:seriesId")
    abstract fun getSeries(seriesId: Int): Flow<FullSeries?>

    // FLOW FUNCTIONS
    @RawQuery(observedEntities = [Series::class])
    abstract fun getSeriesByQuery(query: SupportSQLiteQuery): Flow<List<Series>>

    fun getSeriesByFilter(filter: SearchFilter): Flow<List<Series>> {

        val query = getSeriesQuery(filter)
        Log.d(TAG, "getSeriesByQuery")
        return getSeriesByQuery(query)
    }

    // PAGING SOURCE FUNCTIONS
    @RawQuery(observedEntities = [FullSeries::class])
    abstract fun getSeriesByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullSeries>

    fun getSeriesByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullSeries> {

        val query = getSeriesQuery(filter)
        Log.d(TAG, "getSeriesByQueryPagingSource")
        return getSeriesByQueryPagingSource(query)
    }

    // SUSPEND FUNCTIONS
    @RawQuery(observedEntities = [FullSeries::class])
    abstract suspend fun getSeriesByQuerySus(query: SupportSQLiteQuery): List<FullSeries>

    suspend fun getSeriesByFilterSus(filter: SearchFilter): List<FullSeries> {
        val query = getSeriesQuery(filter)
        Log.d(TAG, "getSeriesByQuerySus")
        return getSeriesByQuerySus(query)
    }

    companion object {
        private fun getSeriesQuery(filter: SearchFilter): SimpleSQLiteQuery {
            val table = StringBuilder()
            val conditions = StringBuilder()
            val args: ArrayList<Any> = arrayListOf()
            fun connectword(): String = if (conditions.isEmpty()) "WHERE" else "AND"

            table.append("""SELECT DISTINCT ss.* 
                FROM series ss 
                LEFT JOIN issue ie on ie.seriesId = ss.seriesId 
                LEFT JOIN story sy on sy.issueId = ie.issueId """)

            conditions.append("${connectword()} ss.seriesId != $DUMMY_ID ")

            if (filter.hasPublisher()) {
                table.append("JOIN publisher pr ON ss.publisherId = pr.publisherId ")

                val publisherList = modelsToSqlIdString(filter.mPublishers)

                conditions.append("${connectword()} pr.publisherId IN $publisherList ")
            }

            if (filter.hasCreator()) {
                table.append(
                    """LEFT JOIN credit ct on ct.storyId = sy.storyId 
                            LEFT JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId 
                            LEFT JOIN excredit ect on ect.storyId = sy.storyId
                            LEFT JOIN namedetail nl2 on nl2.nameDetailId = ect.nameDetailId """)

                val creatorsList = modelsToSqlIdString(filter.mCreators)

                conditions.append(
                    """${connectword()} (nl.creatorId IN $creatorsList 
                        OR nl2.creatorId in $creatorsList)""")
            }

            if (filter.hasDateFilter()) {
                conditions.append("${connectword()} ss.startDate < ? ${connectword()} ss.endDate > ? ")
                args.add(filter.mEndDate)
                args.add(filter.mStartDate)
            }

            filter.mSeries?.let {
                conditions.append("${connectword()} ss.seriesId = ? ")
                args.add(it.seriesId)
            }

            filter.mCharacter?.let {
                table.append("""JOIN appearance ap ON ap.story = sy.storyId """)

                conditions.append("${connectword()} ap.character = ? ")
                args.add(it.characterId)
            }

            if (filter.mMyCollection) {
                table.append("JOIN mycollection mc ON mc.issueId = ie.issueId ")
            }

            filter.mTextFilter?.let {
                conditions.append("${connectword()} ss.seriesName like '%${it.text}%' ")
            }

            val sortClause: String = filter.mSortType?.let { "ORDER BY ${it.sortString}" } ?: ""
            return SimpleSQLiteQuery(
                "$table$conditions$sortClause",
                args.toArray()
            )
        }
    }
}

@ExperimentalCoroutinesApi
@Dao
abstract class BondTypeDao : BaseDao<BondType>("bondtype")

@ExperimentalCoroutinesApi
@Dao
abstract class SeriesBondDao : BaseDao<SeriesBond>("seriesbond")