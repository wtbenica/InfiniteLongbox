/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    @Query(
        """SELECT ap.*
        FROM appearance ap
        WHERE ap.issue = :issueId
    """
    )
    abstract fun getAppearancesByIssueId(issueId: Int): Flow<List<FullAppearance>>

    @Query(
        """
            DELETE FROM appearance
            WHERE 0 = 0
        """
    )
    abstract fun dropAll()
}