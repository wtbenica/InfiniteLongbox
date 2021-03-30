package com.wtb.comiccollector.Daos

import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.NameDetail

@Dao
abstract class NameDetailDao : BaseDao<NameDetail>() {
    @Query("SELECT * FROM NameDetail ND WHERE ND.creatorId = :creatorId")
    abstract suspend fun getNameDetailByCreatorIdSus(creatorId: Int): NameDetail?

    @Query("SELECT * FROM NameDetail ND WHERE ND.name like :name")
    abstract suspend fun getNameDetailByName(name: String): List<NameDetail>?
}