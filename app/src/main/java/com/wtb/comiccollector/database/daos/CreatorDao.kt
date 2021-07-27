package com.wtb.comiccollector.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.Character
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.database.models.FullCreator
import com.wtb.comiccollector.repository.DUMMY_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.util.*

@ExperimentalCoroutinesApi
@Dao
abstract class CreatorDao : BaseDao<Creator>() {

    @Query("SELECT * FROM creator ORDER BY sortName ASC")
    abstract fun getAll(): Flow<List<Creator>>

    // FLOW FUNCTIONS
    @RawQuery(observedEntities = [Creator::class])
    abstract fun getCreatorsByQuery(query: SupportSQLiteQuery): Flow<List<Creator>>

    fun getCreatorsByFilter(filter: SearchFilter): Flow<List<Creator>> {

        val query = Companion.getCreatorQuery(filter)

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
            var tableJoinString = String()
            var conditionsString = String()
            val args: ArrayList<Any> = arrayListOf()
            fun connectword(): String = if (conditionsString.isEmpty()) "WHERE" else "AND"

            tableJoinString +=
                """SELECT DISTINCT cr.* 
                        FROM creator cr 
                        JOIN nameDetail nd ON cr.creatorId = nd.creatorId 
                        JOIN credit ct ON ct.nameDetailId = nd.nameDetailId 
                         JOIN story sy ON ct.storyId = sy.storyId 
                         JOIN issue ie ON ie.issueId = sy.issueId 
                         JOIN series ss ON ie.seriesId = ss.seriesId """

            conditionsString += "${connectword()} cr.creatorId != $DUMMY_ID "

            filter.mSeries?.let {
                conditionsString += """${connectword()} ss.seriesId = ${it.seriesId} """
            }

            if (filter.hasPublisher()) {
                tableJoinString +=
                    """LEFT JOIN publisher pr ON ss.publisherId = pr.publisherId """

                val publisherList = modelsToSqlIdString(filter.mPublishers)

                conditionsString += """${connectword()} pr.publisherId IN $publisherList """
            }

            if (filter.hasDateFilter()) {
                conditionsString += "AND ie.releaseDate < ? AND ie.releaseDate > ? "
                args.add(filter.mEndDate)
                args.add(filter.mStartDate)

            }

            if (filter.mMyCollection) {
                tableJoinString += """JOIN mycollection mc ON mc.issueId = ie.issueId """
            }

            val query = SimpleSQLiteQuery(
                tableJoinString + conditionsString,
                args.toArray()
            )
            return query
        }
    }
}