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

package dev.benica.infinite_longbox

/**
 * A line saying what a list is filtered by, for the collapsed filter sheet, where the filters
 * themselves are out of sight: "My Collection · Oracle · DC · 1990–1999".
 */
object FilterSummary {
    /** How many names of one kind are listed before they are counted instead. */
    private const val NAMES_SHOWN = 2

    fun of(
        myCollection: Boolean,
        myCollectionLabel: String,
        series: String? = null,
        character: String? = null,
        creators: Collection<String> = emptyList(),
        publishers: Collection<String> = emptyList(),
        startYear: Int? = null,
        endYear: Int? = null,
        text: String? = null,
        creatorsLabel: (Int) -> String = { "$it creators" },
        publishersLabel: (Int) -> String = { "$it publishers" },
        missingCover: Boolean = false,
        missingStorage: Boolean = false,
        missingCoverLabel: String = "No cover",
        missingStorageLabel: String = "Not stored",
    ): String = listOfNotNull(
        myCollectionLabel.takeIf { myCollection },
        missingCoverLabel.takeIf { myCollection && missingCover },
        missingStorageLabel.takeIf { myCollection && missingStorage },
        series?.takeIf { it.isNotBlank() },
        character?.takeIf { it.isNotBlank() },
        names(creators, creatorsLabel),
        names(publishers, publishersLabel),
        years(startYear, endYear),
        text?.takeIf { it.isNotBlank() }?.let { "\"$it\"" },
    ).joinToString(" · ")

    private fun names(names: Collection<String>, counted: (Int) -> String): String? {
        val sorted = names.filter { it.isNotBlank() }.sortedBy { it.lowercase() }
        return when {
            sorted.isEmpty() -> null
            sorted.size <= NAMES_SHOWN -> sorted.joinToString(", ")
            else -> counted(sorted.size)
        }
    }

    /** "1990–1999", "from 1990" or "until 1999"; null when neither is set. */
    private fun years(start: Int?, end: Int?): String? = when {
        start != null && end != null -> if (start == end) "$start" else "$start–$end"
        start != null -> "from $start"
        end != null -> "until $end"
        else -> null
    }
}
