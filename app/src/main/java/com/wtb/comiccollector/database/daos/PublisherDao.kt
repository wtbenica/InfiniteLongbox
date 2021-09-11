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
abstract class PublisherDao : BaseDao<Publisher>("publisher") {
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
        val tableJoinString = StringBuilder()
        val conditionsString = StringBuilder()
        val args: ArrayList<Any> = arrayListOf()

        tableJoinString.append(
            """SELECT DISTINCT pr.* 
                FROM publisher pr 
                JOIN series ss ON ss.publisher = pr.publisherId 
                """)

        conditionsString.append("""WHERE pr.publisherId != $DUMMY_ID """)

        if (mSeries != null) {
            conditionsString.append("""AND ss.seriesId = ${mSeries.series.seriesId} """)
        }

        if (filter.hasCreator()) {
            tableJoinString.append(
                """JOIN credit ct ON ct.series = ss.seriesId 
                    JOIN nameDetail nd ON ct.nameDetail = nd.nameDetailId 
                    JOIN creator cr ON cr.creatorId = nd.creator """)

            val creatorIds = Companion.modelsToSqlIdString(filter.mCreators)

            conditionsString.append(
                """AND cr.creatorId IN $creatorIds """)
        }

        if (filter.hasDateFilter()) {
            tableJoinString.append("""JOIN issue ie ON ie.series = ss.seriesId 
            """)
            conditionsString.append("""AND ie.releaseDate <= '${filter.mEndDate}'
                AND ie.releaseDate > '${filter.mStartDate}'
            """)
        }

        if (filter.mMyCollection) {
            tableJoinString.append("""JOIN mycollection mc ON mc.series = ss.seriesId 
            """)
        }
        
        filter.mTextFilter?.let { textFilter -> 
            val text = textFilterToString(textFilter.text)
            
            conditionsString.append("""AND pr.publisher like ? 
            """)

            args.add(text)
        }

        val query = SimpleSQLiteQuery(
            "$tableJoinString$conditionsString",
            args.toArray()
        )

        return getPublishersByQuery(query)
    }
}