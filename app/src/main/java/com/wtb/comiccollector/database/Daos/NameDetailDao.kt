package com.wtb.comiccollector.database.Daos

import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.database.models.NameDetail

@Dao
abstract class NameDetailDao : BaseDao<NameDetail>() {

    @Query("SELECT * FROM NameDetail ND WHERE ND.creatorId = :creatorId")
    abstract suspend fun getNameDetailByCreatorId(creatorId: Int): List<NameDetail>?
}