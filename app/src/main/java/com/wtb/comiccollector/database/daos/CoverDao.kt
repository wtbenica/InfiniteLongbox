package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import com.wtb.comiccollector.database.models.Cover
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Dao
abstract class CoverDao : BaseDao<Cover>("cover")
