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
import com.wtb.comiccollector.database.models.Story
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import org.intellij.lang.annotations.Language

@ExperimentalCoroutinesApi
@Dao
abstract class StoryDao : BaseDao<Story>("Story") {

    @Query(query)
    abstract fun getStoriesFlow(issueId: Int): Flow<List<Story>>

    @Query(query)
    abstract suspend fun getStories(issueId: Int): List<Story>

    @Query(
        """
            DELETE FROM story
            WHERE 0 = 0
        """
    )
    abstract fun dropAll()

    companion object {
        @Language("RoomSql")
        const val query = """
            SELECT sy.*
            FROM story sy
            JOIN storytype se ON se.storyTypeId = sy.storyType
            WHERE sy.issue = :issueId
            AND (sy.storyType = 19 OR sy.storyType= 6)
            ORDER BY sy.sequenceNumber
        """
    }
}