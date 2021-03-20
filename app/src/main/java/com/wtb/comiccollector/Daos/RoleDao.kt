package com.wtb.comiccollector.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.Role

@Dao
abstract class RoleDao : BaseDao<Role>() {
    @Query("SELECT * FROM role")
    abstract fun getRoleList(): LiveData<List<Role>>

    @Query("SELECT * FROM role WHERE roleName = :roleName")
    abstract fun getRoleByName(roleName: String): Role
}