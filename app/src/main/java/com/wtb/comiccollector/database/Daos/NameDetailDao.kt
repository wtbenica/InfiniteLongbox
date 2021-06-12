package com.wtb.comiccollector.database.Daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.wtb.comiccollector.database.models.NameDetail
import com.wtb.comiccollector.database.models.NameDetailAndCreator

@Dao
abstract class NameDetailDao : BaseDao<NameDetail>() {

    @Transaction
    @Query("""
        SELECT *
        FROM namedetail nd
        WHERE nd.creatorId = :creatorId
    """)
    abstract suspend fun getNameDetailsByCreatorId(creatorId: Int): List<NameDetailAndCreator>
}