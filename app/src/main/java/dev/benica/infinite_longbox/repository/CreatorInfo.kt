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

package dev.benica.infinite_longbox.repository

/** What the server knows about a creator (see the backend's creator_info.py). */
data class CreatorInfo(
    val creatorId: Int,
    val name: String,
    val disambiguation: String = "",
    val born: LifeEvent = LifeEvent(),
    val died: LifeEvent = LifeEvent(),
    val bio: String = "",
    val otherNames: List<OtherName> = emptyList(),
    val credits: Int = 0,
    val issues: Int = 0,
    val roles: List<RoleCount> = emptyList(),
    val firstCredit: IssueRef? = null,
    val latestCredit: IssueRef? = null,
    val topSeries: List<SeriesCount> = emptyList(),
)

/** A birth or a death: either part may be blank. */
data class LifeEvent(val date: String = "", val place: String = "")

/** [kind] is "pen name", "birth name" and so on, or blank. */
data class OtherName(val name: String, val kind: String = "")

data class RoleCount(val role: String, val count: Int)

data class SeriesCount(val seriesId: Int, val seriesName: String, val issues: Int)
