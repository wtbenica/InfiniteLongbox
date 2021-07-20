package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import com.wtb.comiccollector.database.models.Appearance
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Dao
abstract class AppearanceDao: BaseDao<Appearance>()