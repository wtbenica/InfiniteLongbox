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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.benica.infinite_longbox.database.models.Cover
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Dao
abstract class CoverDao : BaseDao<Cover>("cover") {
    @Query("""SELECT * FROM cover""")
    abstract suspend fun getAll(): List<Cover>

    /**
     * Saves a cover, replacing any existing cover for the same issue. The generic upsert can't
     * do this: covers are unique by issue, but a new [Cover] has a fresh coverId, so
     * insert-then-update never touched the existing row and a "not found" row stuck forever.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun setCover(cover: Cover)

    @Query("DELETE FROM cover WHERE issue = :issueId")
    abstract suspend fun deleteForIssue(issueId: Int)

    @Query(
        """
       DELETE FROM cover
        WHERE 0 = 0
    """
    )
    abstract fun dropAll()
}
