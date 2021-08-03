package com.wtb.comiccollector.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.SortType
import com.wtb.comiccollector.SortType.Companion.containsSortType
import com.wtb.comiccollector.database.models.Character
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FullCreator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class CreatorDao : BaseDao<Creator>("creator") {

    @Query("SELECT * FROM creator ORDER BY sortName ASC")
    abstract fun getAll(): Flow<List<Creator>>

    // FLOW FUNCTIONS
    @RawQuery(observedEntities = [Creator::class])
    abstract fun getCreatorsByQuery(query: SupportSQLiteQuery): Flow<List<Creator>>

    fun getCreatorsByFilter(filter: SearchFilter): Flow<List<Creator>> {

        val query = getCreatorQuery(filter)

        return getCreatorsByQuery(query)
    }

    // PAGING SOURCE FUNCTIONS
    @RawQuery(observedEntities = [Character::class])
    abstract fun getCreatorsByQueryPagingSource(query: SupportSQLiteQuery): PagingSource<Int, FullCreator>

    fun getCreatorsByFilterPagingSource(filter: SearchFilter): PagingSource<Int, FullCreator> {
        val query = getCreatorQuery(filter)

        return getCreatorsByQueryPagingSource(query)
    }

    companion object {
        private fun getCreatorQuery(filter: SearchFilter): SimpleSQLiteQuery {
            val tableJoinString = StringBuilder()
            val tableJoinString2 = StringBuilder()
            val conditionsString = StringBuilder()
            val args: ArrayList<Any> = arrayListOf()
            fun connectword(): String = if (conditionsString.isEmpty()) "WHERE" else "AND"

            //language=RoomSql
            tableJoinString.append(
                """SELECT DISTINCT cr.* 
                        FROM creator cr 
                        JOIN nameDetail nd ON cr.creatorId = nd.creatorId 
                        JOIN credit ct ON ct.nameDetailId = nd.nameDetailId 
                        JOIN story sy ON ct.storyId = sy.storyId 
                        JOIN issue ie ON ie.issueId = sy.issueId 
                        JOIN series ss ON ie.seriesId = ss.seriesId 
                        """)

            //language=RoomSql
            tableJoinString2.append(
                """SELECT DISTINCT cr.* 
                        FROM creator cr 
                        JOIN nameDetail nd ON cr.creatorId = nd.creatorId 
                        JOIN excredit ect ON ect.nameDetailId = nd.nameDetailId 
                        JOIN story sy ON ect.storyId = sy.storyId 
                        JOIN issue ie ON ie.issueId = sy.issueId 
                        JOIN series ss ON ie.seriesId = ss.seriesId  
                        """)

            filter.mSeries?.let {
                conditionsString.append("""${connectword()} ss.seriesId = ${it.seriesId} 
                """)
            }

            if (filter.hasPublisher()) {
                val publisherList = modelsToSqlIdString(filter.mPublishers)

                conditionsString.append("""${connectword()} ss.publisherId IN $publisherList  
                """)
            }

            if (filter.hasDateFilter()) {
                //language=RoomSql
                conditionsString.append("""${connectword()} ie.releaseDate < ? 
                    AND ie.releaseDate > ? 
                    """)
                args.add(filter.mEndDate)
                args.add(filter.mStartDate)
                args.add(filter.mEndDate)
                args.add(filter.mStartDate)
            }

            if (filter.mMyCollection) {
                conditionsString.append("""${connectword()} ie.issueId IN (
                    SELECT issueId
                    FROM mycollection) 
                """)
            }

            val allArgs: ArrayList<Any> = arrayListOf()
            args.forEach { allArgs.add(it) }
            args.forEach { allArgs.add(it) }

            val sortClause: String = filter.mSortType?.let {
                val isValid =
                    SortType.Companion.SortTypeOptions.CREATOR.options.containsSortType(it)
//                it !in SortType.Companion.SortTypeOptions.CREATOR.options
                val sortString: String =
                    if (isValid) {
                        it.sortString
                    } else {
                        SortType.Companion.SortTypeOptions.CREATOR.options[0].sortString
                    }
                "ORDER BY ${sortString}"
            } ?: ""


            return SimpleSQLiteQuery(
                """
                    SELECT *
                    FROM 
                    ( 
                        $tableJoinString$conditionsString 
                        UNION 
                        $tableJoinString2$conditionsString 
                    ) 
                    $sortClause
                """,
                allArgs.toArray()
            )
        }
    }
}