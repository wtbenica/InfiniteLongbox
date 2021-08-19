package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.wtb.comiccollector.database.models.Appearance
import com.wtb.comiccollector.database.models.FullAppearance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class AppearanceDao : BaseDao<Appearance>("appearance") {

    @Query("SELECT * FROM appearance WHERE story IN (:storyIds)")
    abstract fun getAppearancesByStoryIds(storyIds: List<Int>): List<Appearance>

    @Query("SELECT * FROM appearance WHERE series = :seriesId")
    abstract fun getAppearancesBySeriesId(seriesId: Int): List<Appearance>

    @Transaction
    @Query("""SELECT ap.*
        FROM appearance ap
        WHERE ap.issue = :issueId
    """)
    abstract fun getAppearancesByIssueId(issueId: Int): Flow<List<FullAppearance>>
}