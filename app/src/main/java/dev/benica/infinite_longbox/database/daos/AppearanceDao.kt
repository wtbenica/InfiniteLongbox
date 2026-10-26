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

package dev.benica.infinite_longbox.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.benica.infinite_longbox.database.models.Appearance
import dev.benica.infinite_longbox.database.models.FullAppearance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class AppearanceDao : BaseDao<Appearance>("appearance") {

    @Query("SELECT * FROM appearance WHERE story IN (:storyIds)")
    abstract fun getAppearancesByStoryIds(storyIds: List<Int>): List<Appearance>

    @Transaction
    @Query(
        """SELECT ap.*
        FROM appearance ap
        WHERE ap.issue = :issueId
    """
    )
    abstract fun getAppearancesByIssueId(issueId: Int): Flow<List<FullAppearance>>

    @Query("DELETE FROM appearance WHERE story = :story AND character = :character AND appearanceId NOT IN (:keep)")
    abstract suspend fun deleteOthers(story: Int, character: Int, keep: List<Int>)

    /** A rebuilt server renumbers appearances, so a copy saved under an old id would show twice. */
    @Transaction
    override suspend fun upsertSus(objList: List<Appearance>): Boolean {
        objList.groupBy { it.story to it.character }.forEach { (key, rows) ->
            deleteOthers(key.first, key.second, rows.map { it.appearanceId })
        }
        return super.upsertSus(objList)
    }

    @Query(
        """
            DELETE FROM appearance
            WHERE 0 = 0
        """
    )
    abstract fun dropAll()
}