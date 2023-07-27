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
import dev.benica.infinite_longbox.database.models.Credit
import dev.benica.infinite_longbox.database.models.FullCredit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class CreditDao : BaseDao<Credit>("credit") {

    @Query("SELECT * FROM credit WHERE story IN (:storyIds)")
    abstract fun getCreditsByStoryIds(storyIds: List<Int>): List<Credit>

    @Transaction
    @Query(
        """
            SELECT cr.*, st.sortCode
            FROM credit cr
            JOIN story sy on cr.story = sy.storyId
            JOIN storytype st on st.storyTypeId = sy.storyType
            JOIN role ON cr.role = role.roleId
            WHERE sy.issue = :issueId
            ORDER BY st.sortCode, sy.sequenceNumber, role.sortOrder
        """
    )
    abstract fun getIssueCredits(issueId: Int): Flow<List<FullCredit>>

    @Query(
        """
            DELETE FROM Credit
            WHERE 0 = 0
        """
    )
    abstract fun dropAll()
}