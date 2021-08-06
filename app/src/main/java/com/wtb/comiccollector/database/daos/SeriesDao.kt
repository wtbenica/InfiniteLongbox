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
import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.R
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
        Log.d(TAG, query.sql)
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

            if (filter.mPublishers.isNotEmpty()) {
                val publisherList = modelsToSqlIdString(filter.mPublishers)

                conditions.append("""${connectword()} ss.publisherId IN $publisherList """)
            }

            if (filter.mTextFilter?.type in listOf(All, Publisher)) {
                table.append("""JOIN publisher pr ON ss.publisherId = pr.publisherId 
                """)
            }

            if (filter.hasCreator()) {
                var needsOr = false
                conditions.append("""${connectword()} ss.seriesId IN (
                    SELECT ie.seriesId
                    FROM issue ie
                    WHERE ie.issueId IN (
                        SELECT sy.issueId
                        FROM story sy
                        WHERE (
                """)

                if (filter.mCreators.isNotEmpty()) {
                    needsOr = true
                    val creatorsList = modelsToSqlIdString(filter.mCreators)

                    conditions.append("""
                            sy.storyId IN (
                                SELECT storyId
                                FROM credit ct
                                JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId
                                WHERE nl.creatorId IN $creatorsList
                            )
                            OR sy.storyId IN (
                                SELECT storyId
                                FROM excredit ect
                                JOIN namedetail nl on nl.nameDetailId = ect.nameDetailId
                                WHERE nl.creatorId IN $creatorsList
                            )
                        )
                    )
                ) 
                """)
                }

                if (filter.mTextFilter?.type in listOf(All, NameDetail)) {
                    if (needsOr) conditions.append("""OR """)
                    conditions.append("""
                            sy.storyId IN (
                                SELECT ct.storyId
                                FROM credit ct
                                WHERE (
                                    ct.nameDetailId IN (
                                        SELECT nl.nameDetailId
                                        FROM nameDetail nl
                                        WHERE (
                                            nl.name LIKE '%${filter.mTextFilter?.text}%' 
                                            OR nl.alterego LIKE '%${filter.mTextFilter?.text}%'
                                        )
                                    )                
                                )
                            )
                            OR sy.storyId IN (
                                SELECT ect.storyId
                                FROM excredit ect
                                WHERE (
                                    ect.nameDetailId IN (
                                        SELECT nl.nameDetailId
                                        FROM nameDetail nl
                                        WHERE (
                                            nl.name LIKE '%${filter.mTextFilter?.text}%' 
                                            OR nl.alterego LIKE '%${filter.mTextFilter?.text}%'
                                        )
                                    )                
                                )
                            )
                        )
                    )
                )
                """)
                }
            }

            if (filter.hasDateFilter()) {
                conditions.append("""${connectword()} ss.startDate < ? 
                    ${connectword()} ss.endDate > ? 
                    """)
                args.add(filter.mEndDate)
                args.add(filter.mStartDate)
            }

            if (filter.hasCharacter()) {
                filter.mCharacter?.characterId?.let {
                    conditions.append("""${connectword()} ss.seriesId IN (
                    SELECT ie.seriesId
                            FROM issue ie
                            WHERE ie.issueId IN (
                            SELECT sy.issueId
                                    FROM story sy
                                    WHERE sy.storyId IN (
                                    SELECT ap.story
                                            FROM appearance ap
                                            WHERE ap.character = ?
                                    )
                            )
                    )
                    """)

                    args.add(it)
                }

                if (filter.mTextFilter?.type in listOf(All, Character)) {
                    table.append("""LEFT JOIN character ch ON ch.characterId = ap.character 
                    """)
                }
            }

            if (filter.mMyCollection) {
                conditions.append("""${connectword()} ie.issueId IN (
                    SELECT issueId
                    FROM mycollection) 
                """)
            }

            filter.mTextFilter?.let { textFilter ->
                val lookup: Map<FilterTypeSpinnerOption, List<String?>> =
                    mapOf(Pair(Series, listOf(context?.getString(R.string.table_col_series_name))),
                          Pair(Publisher, listOf(context?.getString(R.string.table_col_publisher))),
                          Pair(NameDetail, listOf(context?.getString(R.string.table_col_namedetail),
                                                  context?.getString(R.string.table_col_namedetail2))),
                          Pair(Character,
                               listOf(context?.getString(R.string.table_col_character_name),
                                      context?.getString(R.string.table_col_character_alterego))))

                conditions.append("${connectword()} (")

                when (textFilter.type) {
                    All.Companion::class -> {
                        lookup.forEach {
                            addTypeFilterElse(it.value,
                                              textFilter,
                                              conditions)
                        }
                    }
                    else                 -> {
                        addTypeFilterElse(lookup[textFilter.type],
                                          textFilter,
                                          conditions)
                    }
                }

                conditions.append(""") """)
            }

            val sortClause: String = filter.mSortType?.let {
                val isValid =
                    SortType.Companion.SortTypeOptions.SERIES.options.containsSortType(it)
//                it !in SortType.Companion.SortTypeOptions.SERIES.options
                Log.d(TAG, "isVALid? $it $isValid")
                val sortString: String =
                    if (isValid) {
                        it.sortString
                    } else {
                        SortType.Companion.SortTypeOptions.SERIES.options[0].sortString
                    }
                "ORDER BY ${sortString}"
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