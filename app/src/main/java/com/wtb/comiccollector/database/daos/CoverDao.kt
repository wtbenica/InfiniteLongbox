package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.database.models.Cover
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Dao
abstract class CoverDao : BaseDao<Cover>("cover") {
    @Query("""SELECT * FROM cover""")
    abstract suspend fun getAll(): List<Cover>
}
