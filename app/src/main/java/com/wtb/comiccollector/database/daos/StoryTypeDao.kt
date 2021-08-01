package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import com.wtb.comiccollector.database.models.StoryType
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Dao
abstract class StoryTypeDao : BaseDao<StoryType>("storytype")