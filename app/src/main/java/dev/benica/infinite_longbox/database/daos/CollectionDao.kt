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
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.database.models.CollectionItem
import dev.benica.infinite_longbox.database.models.UserCollection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "CollectionDao"

@ExperimentalCoroutinesApi
@Dao
abstract class UserCollectionDao : BaseDao<UserCollection>("userCollection") {

    @Query("SELECT userCollectionId, name FROM userCollection")
    abstract suspend fun names(): List<CollectionName>

    @Query(
        """
            DELETE FROM collectionitem
            WHERE collectionItemId > 2;
        """
    )
    abstract fun dropAll()
}

@ExperimentalCoroutinesApi
@Dao
abstract class CollectionItemDao : BaseDao<CollectionItem>("collectionItem") {
    @Query(
        """
            SELECT COUNT(*) as count
            FROM collectionItem ci
            WHERE ci.issue = :issueId
            AND ci.userCollection = :collectionId
        """
    )
    abstract fun inCollection(issueId: Int, collectionId: Int): Flow<Count>

    @Query(
        """
            DELETE FROM collectionItem
            WHERE issue = :issueId
            AND userCollection = :collectionId
        """
    )
    abstract fun deleteById(issueId: Int, collectionId: Int)

    @Query(
        """
            DELETE FROM collectionitem
            WHERE 0 = 0
        """
    )
    abstract fun dropAll()

    /** Everything in the collections, in the order a person would read it. */
    @Query(
        """
            SELECT ci.userCollection AS collectionId,
                   ci.issue AS issueId,
                   ci.series AS seriesId,
                   ss.seriesName AS series,
                   IFNULL(ie.issueNumRaw, '') AS number
            FROM collectionItem ci
            JOIN issue ie ON ie.issueId = ci.issue
            JOIN series ss ON ss.seriesId = ci.series
            ORDER BY ci.userCollection, ss.sortName COLLATE NOCASE, ss.startDate, ie.issueNumRaw
        """
    )
    abstract suspend fun exportRows(): List<CollectionExportRow>
}

data class CollectionExportRow(
    val collectionId: Int,
    val issueId: Int,
    val seriesId: Int,
    val series: String,
    val number: String,
)

data class CollectionName(val userCollectionId: Int, val name: String)

data class Count(
    @SerializedName("count")
    @Expose
    val count: Int
)