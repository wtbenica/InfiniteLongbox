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
import dev.benica.infinite_longbox.database.models.ExCredit
import dev.benica.infinite_longbox.database.models.FullCredit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class ExCreditDao : BaseDao<ExCredit>("excredit") {

    @Query("SELECT * FROM excredit WHERE story IN (:storyIds)")
    abstract fun getExCreditsByStoryIds(storyIds: List<Int>): List<ExCredit>

    @Transaction
    @Query(
        """
            SELECT exc.*, st.sortCode
            FROM excredit exc
            JOIN story sr on exc.story = sr.storyId
            JOIN storytype st on st.storyTypeId = sr.storyType
            JOIN role ON exc.role = role.roleId
            WHERE sr.issue = :issueId
            ORDER BY st.sortCode, sr.sequenceNumber, role.sortOrder
        """
    )
    abstract fun getIssueExtractedCredits(issueId: Int): Flow<List<FullCredit>>

    @Query(
        """
            DELETE FROM excredit
            WHERE 0 = 0
        """
    )
    abstract fun dropAll()
}