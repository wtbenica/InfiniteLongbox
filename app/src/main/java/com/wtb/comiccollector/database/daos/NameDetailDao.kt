package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.database.models.NameDetail
import com.wtb.comiccollector.database.models.NameDetailAndCreator
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Dao
abstract class NameDetailDao : BaseDao<NameDetail>() {

    @Query("SELECT * FROM namedetail WHERE nameDetailId=:id")
    abstract suspend fun get(id: Int): NameDetail?

    @RawQuery(observedEntities = [NameDetailAndCreator::class])
    abstract suspend fun getNameDetailsRaw(query: SupportSQLiteQuery): List<NameDetailAndCreator>

    suspend fun getNameDetailsByCreatorIds(creatorIds: List<Int>): List<NameDetailAndCreator> {
        val ids = idsToSqlIdString(creatorIds)
        val args: ArrayList<Any> = arrayListOf()

        val query = SimpleSQLiteQuery(
            """
        SELECT *
        FROM namedetail nd
        WHERE nd.creatorId IN $ids """
        )

        return getNameDetailsRaw(query)
    }
}