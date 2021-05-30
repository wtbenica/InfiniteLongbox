package com.wtb.comiccollector.database.Daos

import androidx.room.Dao
import com.wtb.comiccollector.database.models.Cover

@Dao
abstract class CoverDao : BaseDao<Cover>();
