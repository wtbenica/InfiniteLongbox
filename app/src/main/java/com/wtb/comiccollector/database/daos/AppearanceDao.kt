package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.database.models.Appearance
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Dao
abstract class AppearanceDao : BaseDao<Appearance>("appearance") {

    @Query("SELECT * FROM appearance WHERE story IN (:storyIds)")
    abstract fun getAppearancesByStoryIds(storyIds: List<Int>): List<Appearance>

    @Query("SELECT * FROM appearance WHERE series = :seriesId")
    abstract fun getAppearancesBySeriesId(seriesId: Int): List<Appearance>
}