package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.repository.DUMMY_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.util.*

@ExperimentalCoroutinesApi
@Dao
abstract class PublisherDao : BaseDao<Publisher>() {
    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    abstract fun getPublisher(publisherId: Int): Flow<Publisher?>

    @Query("SELECT * FROM publisher WHERE publisherId != $DUMMY_ID ORDER BY publisher ASC")
    abstract fun getAll(): Flow<List<Publisher>>

    @Query("SELECT * FROM publisher WHERE publisherId IN (:publisherIds)")
    abstract fun getPublishers(publisherIds: List<Int>?): Flow<List<Publisher>?>

    @RawQuery(
        observedEntities = [Publisher::class]
    )
    abstract fun getPublishersByQuery(query: SupportSQLiteQuery): Flow<List<Publisher>>

    fun getPublishersByFilter(filter: SearchFilter): Flow<List<Publisher>> {
        val mSeries = filter.mSeries
        var tableJoinString = String()
        var conditionsString = String()
        val args: ArrayList<Any> = arrayListOf()

        tableJoinString +=
            "SELECT DISTINCT pr.* " +
                    "FROM publisher pr " +
                    "JOIN series ss ON ss.publisherId = pr.publisherId " +
                    "JOIN issue ie ON ie.seriesId = ss.seriesId "

        conditionsString += "WHERE pr.publisherId != $DUMMY_ID "

        if (mSeries != null) {
            conditionsString += "AND ss.seriesId = ${mSeries.seriesId} "
        }

        if (filter.hasCreator()) {
            tableJoinString +=
                "JOIN story sy ON sy.issueId = ie.issueId " +
                        "JOIN credit ct ON ct.storyId = sy.storyId " +
                        "JOIN nameDetail nd ON ct.nameDetailId = nd.nameDetailId " +
                        "JOIN creator cr ON cr.creatorId = nd.creatorId "

            val creatorIds = Companion.modelsToSqlIdString(filter.mCreators)

            conditionsString +=
                "AND cr.creatorId IN $creatorIds"
        }

        if (filter.hasDateFilter()) {
            conditionsString += "AND ie.releaseDate < ? AND ie.releaseDate > ? "
            args.add(filter.mEndDate)
            args.add(filter.mStartDate)
        }

        if (filter.mMyCollection) {
            tableJoinString += "JOIN issue ie2 on ie2.seriesId = ss.seriesId " +
                    "JOIN mycollection mc ON mc.issueId = ie2.issueId "
        }

        val query = SimpleSQLiteQuery(
            tableJoinString + conditionsString,
            args.toArray()
        )

        return getPublishersByQuery(query)
    }
}