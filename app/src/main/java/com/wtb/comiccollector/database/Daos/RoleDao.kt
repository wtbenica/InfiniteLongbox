package com.wtb.comiccollector.database.Daos

import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.database.models.Role
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RoleDao : BaseDao<Role>() {
    @Query("SELECT * FROM role")
    abstract fun getRoleList(): Flow<List<Role>>

    @Query("SELECT * FROM role WHERE roleName = :roleName")
    abstract fun getRoleByName(roleName: String): Role
}