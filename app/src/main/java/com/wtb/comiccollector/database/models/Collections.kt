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

package com.wtb.comiccollector.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Issue::class,
            parentColumns = arrayOf("issueId"),
            childColumns = arrayOf("issue"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Series::class,
            parentColumns = arrayOf("seriesId"),
            childColumns = arrayOf("series"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserCollection::class,
            parentColumns = arrayOf("userCollectionId"),
            childColumns = arrayOf("userCollection"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("issue", "userCollection", unique = true),
        Index("userCollection"),
        Index("series"),
    ]
)
data class CollectionItem(
    @PrimaryKey(autoGenerate = true) val collectionItemId: Int = AUTO_ID,
    var issue: Int,
    var series: Int,
    var userCollection: Int
) : DataModel() {
    override val id: Int
        get() = collectionItemId
}

@Entity
data class UserCollection(
    @PrimaryKey(autoGenerate = true) val userCollectionId: Int = AUTO_ID,
    var name: String,
    var permanent: Boolean = false
) : DataModel() {
    override val id: Int
        get() = userCollectionId
}

enum class BaseCollection(val id: Int) {
    MY_COLL(1), WISH_LIST(2)
}

