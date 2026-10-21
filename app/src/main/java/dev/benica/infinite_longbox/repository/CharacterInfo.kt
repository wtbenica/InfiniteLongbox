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

/** What the server knows about a character (see the backend's character_info.py). */
data class CharacterInfo(
    val characterId: Int,
    val name: String,
    val alterEgo: String = "",
    val publisher: String = "",
    val appearances: Int = 0,
    val issues: Int = 0,
    val firstAppearance: IssueRef? = null,
    val origin: IssueRef? = null,
    val death: IssueRef? = null,
    val linked: Boolean = false,
    /** The GCD's own id for the character, when there is one; the page on comics.org uses it. */
    val gcdId: Int? = null,
    val description: String = "",
    val firstYear: Int? = null,
    val alternateNames: List<String> = emptyList(),
    val teams: List<TeamRef> = emptyList(),
    val related: List<RelatedCharacter> = emptyList(),
)

data class IssueRef(
    val issueId: Int,
    val seriesId: Int,
    val seriesName: String,
    val number: String = "",
    val keyDate: String = "",
    val year: Int? = null,
)

data class TeamRef(
    val groupId: Int,
    val name: String,
    val yearJoined: Int? = null,
    val yearLeft: Int? = null,
    val membership: String = "",
    val notes: String = "",
)

/** [characterId] is null for a character the phone has no row for, which can't be opened. */
data class RelatedCharacter(
    val relation: String,
    val characterId: Int?,
    val name: String,
    val alterEgo: String = "",
    val detail: String = "",
)
