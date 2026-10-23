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

import kotlinx.serialization.Serializable
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.database.models.CollectionItem
import dev.benica.infinite_longbox.database.models.CopySigner
import dev.benica.infinite_longbox.database.models.Creator
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

    /** What is kept about a copy: where it is stored, its condition, notes. */
    @Query(
        """
            UPDATE collectionItem
            SET storageLocation = :storageLocation, condition = :condition, notes = :notes
            WHERE issue = :issueId AND userCollection = :collectionId
        """
    )
    abstract fun setCopyInfo(
        issueId: Int,
        collectionId: Int,
        storageLocation: String?,
        condition: String?,
        notes: String?,
    )

    /** Where the copy is stored, and nothing else about it. */
    @Query(
        """
            UPDATE collectionItem SET storageLocation = :storageLocation
            WHERE issue = :issueId AND userCollection = :collectionId
        """
    )
    abstract fun setStorage(issueId: Int, collectionId: Int, storageLocation: String)

    @Query("SELECT collectionItemId FROM collectionItem WHERE issue = :issueId AND userCollection = :collectionId")
    abstract fun itemId(issueId: Int, collectionId: Int): Int?

    @Query("DELETE FROM CopySigner WHERE collectionItem = :itemId")
    abstract fun clearSigners(itemId: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun addSigners(signers: List<CopySigner>)

    /** Makes [creatorIds] the creators who signed the copy of the issue in the collection. */
    @Transaction
    open fun setSigners(issueId: Int, collectionId: Int, creatorIds: Collection<Int>) {
        val item = itemId(issueId, collectionId) ?: return
        clearSigners(item)
        addSigners(creatorIds.distinct().map { CopySigner(item, it) })
    }

    /** The creators who signed the copy of the issue in the collection. */
    @Query(
        """
            SELECT cr.* FROM CopySigner cs
            JOIN CollectionItem ci ON ci.collectionItemId = cs.collectionItem
            JOIN Creator cr ON cr.creatorId = cs.creator
            WHERE ci.issue = :issueId AND ci.userCollection = :collectionId
            ORDER BY cr.sortName COLLATE NOCASE
        """
    )
    abstract fun signers(issueId: Int, collectionId: Int): Flow<List<Creator>>

    /** The copies in the collection that [creatorId] signed. */
    @Query(
        """
            SELECT ci.issue AS issueId, ci.series AS seriesId, ss.seriesName AS series,
                   IFNULL(ie.issueNumRaw, '') AS number, ie.variantName AS variantName
            FROM CopySigner cs
            JOIN CollectionItem ci ON ci.collectionItemId = cs.collectionItem
            JOIN Issue ie ON ie.issueId = ci.issue
            JOIN Series ss ON ss.seriesId = ci.series
            WHERE cs.creator = :creatorId AND ci.userCollection = :collectionId
            ORDER BY ss.sortName COLLATE NOCASE, ss.startDate, ie.sortCode
        """
    )
    abstract fun signedBy(creatorId: Int, collectionId: Int): Flow<List<SignedIssue>>

    /** Every signature, for the backup file. */
    @Query(
        """
            SELECT ci.userCollection AS collectionId, ci.issue AS issueId,
                   cr.creatorId AS creatorId, cr.name AS name
            FROM CopySigner cs
            JOIN CollectionItem ci ON ci.collectionItemId = cs.collectionItem
            JOIN Creator cr ON cr.creatorId = cs.creator
        """
    )
    abstract suspend fun exportSigners(): List<SignatureRow>

    /** The creators among [ids] the phone has a record of. */
    @Query("SELECT creatorId FROM Creator WHERE creatorId IN (:ids)")
    abstract suspend fun knownCreators(ids: List<Int>): List<Int>

    /** The places copies have been put, for suggesting one when entering the next. */
    @Query(
        """
            SELECT DISTINCT storageLocation FROM collectionItem
            WHERE storageLocation IS NOT NULL AND storageLocation != ''
            ORDER BY storageLocation COLLATE NOCASE
        """
    )
    abstract suspend fun storageLocations(): List<String>

    /** How many issues are in the collection. */
    @Query("SELECT COUNT(DISTINCT issue) FROM collectionItem WHERE userCollection = :collectionId")
    abstract fun countIssues(collectionId: Int): Flow<Int>

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
                   IFNULL(ie.issueNumRaw, '') AS number,
                   ci.storageLocation AS storageLocation,
                   ci.condition AS condition,
                   ci.notes AS notes
            FROM collectionItem ci
            JOIN issue ie ON ie.issueId = ci.issue
            JOIN series ss ON ss.seriesId = ci.series
            ORDER BY ci.userCollection, ss.sortName COLLATE NOCASE, ss.startDate, ie.sortCode
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
    val storageLocation: String? = null,
    val condition: String? = null,
    val notes: String? = null,
)

/** A copy a creator signed, as the creator's page lists it. */
data class SignedIssue(
    val issueId: Int,
    val seriesId: Int,
    val series: String,
    val number: String,
    val variantName: String,
)

data class SignatureRow(val collectionId: Int, val issueId: Int, val creatorId: Int, val name: String)

data class CollectionName(val userCollectionId: Int, val name: String)

@Serializable
data class Count(
    val count: Int
)