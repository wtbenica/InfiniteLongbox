package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.database.models.Role
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class RoleDao : BaseDao<Role>() {
    @Query("SELECT * FROM role")
    abstract fun getRoleList(): Flow<List<Role>>
}