package com.wtb.comiccollector.database.Daos

import androidx.room.Dao
import com.wtb.comiccollector.database.models.MyCollection

@Dao
abstract class CollectionDao : BaseDao<MyCollection>()