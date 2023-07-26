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
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.comiccollector.database.models.NameDetail
import com.wtb.comiccollector.database.models.NameDetailAndCreator
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Dao
abstract class NameDetailDao : BaseDao<NameDetail>("namedetail") {

    @RawQuery(observedEntities = [NameDetailAndCreator::class])
    abstract suspend fun getNameDetailsRaw(query: SupportSQLiteQuery): List<NameDetail>

    suspend fun getNameDetailsByCreatorId(creatorId: Int): List<NameDetail> {
        val query = SimpleSQLiteQuery(
            """SELECT *
                FROM namedetail nd
                WHERE nd.creator = $creatorId """
        )

        return getNameDetailsRaw(query)
    }

    suspend fun getNameDetailsByCreatorIds(creatorIds: List<Int>): List<NameDetail> {
        val query = SimpleSQLiteQuery(
            """SELECT *
                FROM namedetail nd
                WHERE nd.creator in ${
                creatorIds.toString()
                    .replace('[', '(')
                    .replace(']', ')')
            }""",
        )

        return getNameDetailsRaw(query)
    }
}