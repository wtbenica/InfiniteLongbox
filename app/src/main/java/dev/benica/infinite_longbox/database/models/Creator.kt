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

package dev.benica.infinite_longbox.database.models

import androidx.room.*
import dev.benica.infinite_longbox.InfiniteLongboxApplication.Companion.context
import dev.benica.infinite_longbox.R
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Entity(
    indices = [
        Index(value = ["name"]),
        Index(value = ["sortName"])
    ]
)
data class Creator(
    @PrimaryKey(autoGenerate = true) val creatorId: Int = AUTO_ID,
    val name: String,
    val sortName: String,
    val bio: String? = null,
) : DataModel(), FilterModel {

    override val id: Int
        get() = creatorId

    override val tagName: String
        get() = "Creator"

    override val compareValue: String
        get() = name

    override fun toString(): String {
        return name
    }
}

@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Creator::class,
            parentColumns = arrayOf("creatorId"),
            childColumns = arrayOf("creator"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = arrayOf("creator")),
        Index(value = ["name"]),
        Index(value = ["sortName"])
    ]
)
data class NameDetail(
    @PrimaryKey(autoGenerate = true) val nameDetailId: Int = AUTO_ID,
    val creator: Int,
    val name: String,
    val sortName: String? = null,
) : DataModel(), FilterModel {
    override val id: Int
        get() = nameDetailId

    companion object : FilterType {
        override val displayName: String = context!!.getString(R.string.filter_type_creator)

        override fun toString(): String = displayName
    }

    override val tagName: String
        get() = displayName

    override val compareValue: String
        get() = name
}

/**
 * Used in FullCredit to hold the creator linked to the nameDetail
 */
@ExperimentalCoroutinesApi
data class NameDetailAndCreator(
    @Embedded
    val nameDetail: NameDetail,

    @Relation(parentColumn = "creator", entityColumn = "creatorId")
    val creator: Creator,
)

@ExperimentalCoroutinesApi
data class FullCreator(
    @Embedded
    val creator: Creator,

    @Relation(parentColumn = "creatorId", entityColumn = "creator")
    val nameDetail: List<NameDetail>,
) : ListItem