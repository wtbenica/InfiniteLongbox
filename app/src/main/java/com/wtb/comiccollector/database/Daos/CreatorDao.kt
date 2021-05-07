package com.wtb.comiccollector.database.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.DUMMY_ID
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.models.Creator
import java.time.LocalDate
import java.util.*

@Dao
abstract class CreatorDao : BaseDao<Creator>() {
    @Query(
        """
            SELECT DISTINCT creator.*
            FROM creator
            NATURAL JOIN issue
            NATURAL JOIN series
            NATURAL JOIN credit
            WHERE seriesId = :seriesId
            AND issue.releaseDate < :endDate AND issue.releaseDate > :startDate
        """
    )
    abstract fun getCreatorList(
        seriesId: Int,
        startDate: LocalDate = LocalDate.MIN,
        endDate: LocalDate = LocalDate.MAX
    ): LiveData<List<Creator>>

    @Query("SELECT * FROM creator ORDER BY sortName ASC")
    abstract fun getCreatorsList(): LiveData<List<Creator>>

    @Query(
        """
            SELECT c.*
            FROM creator c
            NATURAL JOIN namedetail nl
            NATURAL JOIN credit ct
            NATURAL JOIN story sy
            NATURAL JOIN issue ie
            NATURAL JOIN series s
            WHERE s.seriesId = :seriesId
            AND c.name LIKE :text
        """
    )
    abstract fun getCreatorBySeriesAndPartial(seriesId: Int, text: String): LiveData<List<Creator>>

    @Query(
        """
            SELECT c.*
            FROM creator c
            NATURAL JOIN namedetail nl
            NATURAL JOIN credit ct
            NATURAL JOIN story sy
            NATURAL JOIN issue ie
            NATURAL JOIN series s
            WHERE s.seriesId = :seriesId
        """
    )
    abstract fun getCreatorBySeries(seriesId: Int): LiveData<List<Creator>>

    @Query(
        """
            SELECT *
            FROM creator
            WHERE name LIKE :text
        """
    )
    abstract fun getCreatorByPartial(text: String): LiveData<List<Creator>>

    @Query(
        """
            SELECT *
            FROM creator cr
            WHERE cr.name = :creator
        """
    )
    abstract fun getCreatorByName(creator: String): LiveData<Creator?>

    @Query(
        """
            SELECT *
            FROM creator cr
            WHERE cr.name = :creator
        """
    )
    abstract suspend fun getCreatorByNameSus(creator: String): List<Creator>?

    @Query("SELECT * FROM creator WHERE creatorId = :creatorId")
    abstract fun getCreator(vararg creatorId: Int): LiveData<Creator?>

    @Query("SELECT * FROM creator WHERE creatorId = :creatorId")
    abstract suspend fun getCreatorSus(vararg creatorId: Int): Creator?

    @Query("SELECT * FROM creator WHERE creatorId IN (:creatorIds)")
    abstract fun getCreators(creatorIds: List<Int>?): LiveData<List<Creator>?>

    @RawQuery(
        observedEntities = [Creator::class]
    )
    abstract fun getCreatorsByQuery(query: SupportSQLiteQuery): LiveData<List<Creator>>

    fun getCreatorsByFilter(filter: Filter): LiveData<List<Creator>> {
        val mSeries = filter.mSeries

        var tableJoinString = String()
        var conditionsString = String()
        val args: ArrayList<Any> = arrayListOf()

        tableJoinString +=
            "SELECT DISTINCT cr.* " +
                    "FROM creator cr "

        conditionsString += "WHERE cr.creatorId != $DUMMY_ID "

        if (filter.hasPublisher() || filter.hasDateFilter() || mSeries != null) {
            tableJoinString +=
                "JOIN nameDetail nd ON cr.creatorId = nd.creatorId " +
                        "JOIN credit ct ON ct.nameDetailId = nd.nameDetailId " +
                        "JOIN story sy ON ct.storyId = sy.storyId " +
                        "JOIN issue ie ON ie.issueId = sy.issueId " +
                        "JOIN series ss ON ie.seriesId = ss.seriesId "

            if (mSeries != null) {
                conditionsString += "AND ss.seriesId = ${mSeries.seriesId} "
            }

            if (filter.hasPublisher()) {
                tableJoinString +=
                    "JOIN series ss ON ie.seriesId = ss.seriesId " +
                            "JOIN publisher pr ON ss.publisherId = pr.publisherId "

                val publisherList = modelsToSqlIdString(filter.mPublishers)

                conditionsString += "AND pr.publisherId IN $publisherList"
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

        return getCreatorsByQuery(query)
    }
}