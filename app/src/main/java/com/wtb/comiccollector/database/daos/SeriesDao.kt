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
    abstract fun getSeriesByQuery(query: SupportSQLiteQuery): Flow<List<FullSeries>>

    fun getSeriesByFilter(filter: SearchFilter): Flow<List<FullSeries>> {
        val query = getSeriesQuery(filter)

        return getSeriesByQuery(query)
    }

    // PAGING SOURCE FUNCTIONS
    @RawQuery(observedEntities = [FullSeries::class])
    abstract fun getSeriesByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullSeries>

    fun getSeriesByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullSeries> {

        val query = getSeriesQuery(filter)
        Log.d(TAG, "Paging: ${query.sql} $query")
        return getSeriesByQueryPagingSource(query)
    }

    // SUSPEND FUNCTIONS
    @RawQuery(observedEntities = [FullSeries::class])
    abstract suspend fun getSeriesByQuerySus(query: SupportSQLiteQuery): List<FullSeries>

    suspend fun getSeriesByFilterSus(filter: SearchFilter): List<FullSeries> {
        val query = getSeriesQuery(filter)

        return getSeriesByQuerySus(query)
    }

    companion object {
        private fun getSeriesQuery(filter: SearchFilter): SimpleSQLiteQuery {
            val table = StringBuilder()
            val conditions = StringBuilder()
            val args: ArrayList<Any> = arrayListOf()
            fun connectword(): String = if (conditions.isEmpty()) "WHERE" else "AND"

            table.append("""SELECT DISTINCT ss.* 
                FROM series ss """)

            conditions.append("${connectword()} ss.seriesId != $DUMMY_ID ")

            if (filter.mPublishers.isNotEmpty()) {
                val publisherList = modelsToSqlIdString(filter.mPublishers)

                conditions.append("""${connectword()} ss.publisher IN $publisherList """)
            }

            if (filter.hasCreator()) {
                val creatorsList = modelsToSqlIdString(filter.mCreators)
                conditions.append(
                    """${connectword()} ss.seriesId IN (
                        SELECT ct.series
                        FROM credit ct
                        WHERE ct.nameDetail IN (
                            SELECT nl.nameDetailId
                            FROM namedetail nl
                            WHERE nl.creator IN $creatorsList))
                    OR ss.seriesId IN (
                        SELECT ect.series
                        FROM excredit ect
                        WHERE ect.nameDetail IN (
                            SELECT nl.nameDetailId
                            FROM namedetail nl
                            WHERE nl.creator IN $creatorsList))
                    """)
            }

            if (filter.hasDateFilter()) {
                conditions.append("""${connectword()} ss.startDate < ? 
                    ${connectword()} ss.endDate > ? 
                    """)
                args.add(filter.mEndDate)
                args.add(filter.mStartDate)
            }

            filter.mCharacter?.characterId?.let {
                conditions.append(
                    """${connectword()} ss.seriesId IN (
                        SELECT ap.series
                        FROM appearance ap
                        WHERE ap.character = $it)
                         """)
            }

            if (filter.mMyCollection) {
                conditions.append("""${connectword()} ss.seriesId IN (
                    SELECT series
                    FROM mycollection) 
                """)
            }

            filter.mTextFilter?.let { textFilter ->
                val text = textFilterToString(textFilter.text)

                conditions.append("""${connectword()} ss.seriesName like '$text' 
                """)
            }

            val sortClause: String = filter.mSortType?.let {
                val isValid = SortType.Companion.SortTypeOptions.SERIES.options.containsSortType(it)

                val sortString: String = if (isValid) {
                    it.sortString
                } else {
                    SortType.Companion.SortTypeOptions.SERIES.options[0].sortString
                }

                "ORDER BY $sortString"
            } ?: ""

            return SimpleSQLiteQuery(
                "$table$conditions$sortClause",
                args.toArray()
            )
        }

        private fun addTypeFilterElse(
            lookup: List<String?>?,
            textFilter: TextFilter,
            conditions: StringBuilder,
        ) {
            var first = true
            lookup?.forEach {
                addTextFilterCondition(it,
                                       first,
                                       conditions,
                                       textFilter)
                    .also { first = false }
            }
        }

        private fun addTextFilterCondition(
            column: String?,
            first: Boolean,
            conditions: StringBuilder,
            textFilter: TextFilter,
        ) {
            if (!first) {
                conditions.append("""OR """)
            }

            conditions.append("""$column like '%${textFilter.text}%' """)
        }
    }
}

@ExperimentalCoroutinesApi
@Dao
abstract class BondTypeDao : BaseDao<BondType>("bondtype")

@ExperimentalCoroutinesApi
@Dao
abstract class SeriesBondDao : BaseDao<SeriesBond>("seriesbond")