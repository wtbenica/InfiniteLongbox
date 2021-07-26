package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.Creator
import com.wtb.comiccollector.repository.DUMMY_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.util.*

@ExperimentalCoroutinesApi
@Dao
abstract class CreatorDao : BaseDao<Creator>() {
    fun getCreatorsByFilter(filter: SearchFilter): Flow<List<Creator>> {
        val mSeries = filter.mSeries

        var tableJoinString = String()
        var conditionsString = String()
        val args: ArrayList<Any> = arrayListOf()

        tableJoinString +=
            "SELECT DISTINCT cr.* " +
                    "FROM creator cr " +
                    "LEFT JOIN nameDetail nd ON cr.creatorId = nd.creatorId " +
                    "LEFT JOIN credit ct ON ct.nameDetailId = nd.nameDetailId " +
                    "LEFT JOIN story sy ON ct.storyId = sy.storyId " +
                    "LEFT JOIN issue ie ON ie.issueId = sy.issueId " +
                    "LEFT JOIN series ss ON ie.seriesId = ss.seriesId "

        conditionsString += "WHERE cr.creatorId != $DUMMY_ID "

        if (mSeries != null) {
            conditionsString += "AND ss.seriesId = ${mSeries.seriesId} "
        }

        if (filter.hasPublisher()) {
            tableJoinString +=
                "LEFT JOIN publisher pr ON ss.publisherId = pr.publisherId "

            val publisherList = Companion.modelsToSqlIdString(filter.mPublishers)

            conditionsString += "AND pr.publisherId IN $publisherList"
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

        return getCreatorsByQuery(query)
    }

    @RawQuery(
        observedEntities = [Creator::class]
    )
    abstract fun getCreatorsByQuery(query: SupportSQLiteQuery): Flow<List<Creator>>

    @Query("SELECT * FROM creator ORDER BY sortName ASC")
    abstract fun getAll(): Flow<List<Creator>>

//    @Query(
//        """
//            SELECT DISTINCT creator.*
//            FROM creator
//            NATURAL JOIN issue
//            NATURAL JOIN series
//            NATURAL JOIN credit
//            WHERE seriesId = :seriesId
//            AND issue.releaseDate < :endDate AND issue.releaseDate > :startDate
//        """
//    )
//    abstract fun getCreatorList(
//        seriesId: Int,
//        startDate: LocalDate = LocalDate.MIN,
//        endDate: LocalDate = LocalDate.MAX
//    ): LiveData<List<Creator>>
//

//    @Query(
//        """
//            SELECT c.*
//            FROM creator c
//            NATURAL JOIN namedetail nl
//            NATURAL JOIN credit ct
//            NATURAL JOIN story sy
//            NATURAL JOIN issue ie
//            NATURAL JOIN series s
//            WHERE s.seriesId = :seriesId
//            AND c.name LIKE :text
//        """
//    )
//    abstract fun getCreatorBySeriesAndPartial(seriesId: Int, text: String): Flow<List<Creator>>
//
//    @Query(
//        """
//            SELECT c.*
//            FROM creator c
//            NATURAL JOIN namedetail nl
//            NATURAL JOIN credit ct
//            NATURAL JOIN story sy
//            NATURAL JOIN issue ie
//            NATURAL JOIN series s
//            WHERE s.seriesId = :seriesId
//        """
//    )
//    abstract fun getCreatorBySeries(seriesId: Int): Flow<List<Creator>>
//
//    @Query(
//        """
//            SELECT *
//            FROM creator
//            WHERE name LIKE :text
//        """
//    )
//    abstract fun getCreatorByPartial(text: String): Flow<List<Creator>>
//
//    @Query(
//        """
//            SELECT *
//            FROM creator cr
//            WHERE cr.name = :creator
//        """
//    )
//    abstract fun getCreatorByName(creator: String): Flow<Creator?>
//
//    @Query(
//        """
//            SELECT *
//            FROM creator cr
//            WHERE cr.name = :creator
//        """
//    )
//    abstract suspend fun getCreatorByNameSus(creator: String): List<Creator>?
//
//    @Query("SELECT * FROM creator WHERE creatorId = :creatorId")
//    abstract fun getCreator(vararg creatorId: Int): Flow<Creator?>
//
//    @Query("SELECT * FROM creator WHERE creatorId = :creatorId")
//    abstract suspend fun getCreatorSus(vararg creatorId: Int): Creator?
//
//    @Query("SELECT * FROM creator WHERE creatorId IN (:creatorIds)")
//    abstract fun getCreators(creatorIds: List<Int>?): Flow<List<Creator>?>
}