package com.wtb.comiccollector.database.Daos

import androidx.room.Dao
import com.wtb.comiccollector.database.models.StoryType

@Dao
abstract class StoryTypeDao : BaseDao<StoryType>()