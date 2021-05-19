package com.wtb.comiccollector.database.Daos

import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.database.models.NameDetail

@Dao
abstract class NameDetailDao : BaseDao<NameDetail>() {
    // TODO: This should return a list
    @Query("SELECT * FROM NameDetail ND WHERE ND.creatorId = :creatorId")
    abstract suspend fun getNameDetailByCreatorIdSus(creatorId: Int): List<NameDetail>?

    @Query("SELECT * FROM NameDetail ND WHERE ND.name like :name")
    abstract suspend fun getNameDetailByName(name: String): List<NameDetail>?
}