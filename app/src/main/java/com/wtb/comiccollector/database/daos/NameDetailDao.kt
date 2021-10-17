package com.wtb.comiccollector.database.daos

import android.database.sqlite.SQLiteQueryBuilder
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.database.models.NameDetail
import com.wtb.comiccollector.database.models.NameDetailAndCreator
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Dao
abstract class NameDetailDao : BaseDao<NameDetail>("namedetail") {

    @RawQuery(observedEntities = [NameDetailAndCreator::class])
    abstract suspend fun getNameDetailsRaw(query: SupportSQLiteQuery): List<NameDetail>

    suspend fun getNameDetailsByCreatorId(creatorId: Int): List<NameDetail> {
        val query = SimpleSQLiteQuery(
            """SELECT *
                FROM namedetail nd
                WHERE nd.creator = $creatorId """
        )

        return getNameDetailsRaw(query)
    }

    suspend fun getNameDetailsByCreatorIds(creatorIds: List<Int>): List<NameDetail> {
        val query = SimpleSQLiteQuery(
            """SELECT *
                FROM namedetail nd
                WHERE nd.creator in ${
                creatorIds.toString()
                    .replace('[', '(')
                    .replace(']', ')')
            }""",
        )

        return getNameDetailsRaw(query)
    }
}