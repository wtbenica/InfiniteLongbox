package com.wtb.comiccollector.database.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.DUMMY_ID
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.models.Publisher
import java.util.*

@Dao
abstract class PublisherDao : BaseDao<Publisher>() {
    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    abstract fun getPublisher(publisherId: Int): LiveData<Publisher?>

    @Query("SELECT * FROM publisher WHERE publisherId != $DUMMY_ID ORDER BY publisher ASC")
    abstract fun getPublishersList(): LiveData<List<Publisher>>

    @Query("SELECT * FROM publisher WHERE publisherId IN (:publisherIds)")
    abstract fun getPublishers(publisherIds: List<Int>?): LiveData<List<Publisher>?>

    @RawQuery(
        observedEntities = arrayOf(
            Publisher::class
        )
    )
    abstract fun getPublishersByQuery(query: SupportSQLiteQuery): LiveData<List<Publisher>>

    fun getPublishersByFilter(filter: Filter): LiveData<List<Publisher>> {
        val mSeries = filter.mSeries
        var tableJoinString = String()
        var conditionsString = String()
        val args: ArrayList<Any> = arrayListOf()

        tableJoinString +=
            "SELECT DISTINCT pr.* " +
                    "FROM publisher pr "

        conditionsString += "WHERE pr.publisherId != $DUMMY_ID "

        if (filter.hasCreator() || filter.hasDateFilter() || mSeries != null) {
            tableJoinString +=
                "JOIN series ss ON ss.publisherId = pr.publisherId " +
                        "JOIN issue ie ON ie.seriesId = ss.seriesId "

            if (mSeries != null) {
                conditionsString += "AND ss.seriesId = ${mSeries.seriesId} "
            }

            if (filter.hasCreator()) {
                tableJoinString +=
                    "JOIN story sy ON sy.issueId = ie.issueId " +
                            "JOIN credit ct ON ct.storyId = sy.storyId " +
                            "JOIN nameDetail nd ON ct.nameDetailId = nd.nameDetailId " +
                            "JOIN creator cr ON cr.creatorId = nd.creatorId"

                conditionsString +=
                    "WHERE cr.creatorId IN ${filter.mCreators}"
            }

            if (filter.hasDateFilter()) {
                conditionsString += "AND ie.releaseDate < ? AND ie.releaseDate > ? "
                args.add(filter.mEndDate)
                args.add(filter.mStartDate)
            }
        }

        val query = SimpleSQLiteQuery(
            tableJoinString + conditionsString,
            args.toArray()
        )

        return getPublishersByQuery(query)
    }
}