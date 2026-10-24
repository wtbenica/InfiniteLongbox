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

import java.time.LocalDate

/** What the app knows about an issue's cover row in its database. */
data class CoverRow(val hasImage: Boolean, val checked: LocalDate)

/** What to do about an issue's cover, from the file on the phone and the row in the database. */
enum class CoverPlan {
    /** Ask the server for the image. */
    DOWNLOAD,

    /** The image is on the phone: record where. */
    LINK,

    /** The image is on the phone; ask whether the server's has changed. */
    RECHECK,

    /** Nothing to do. */
    NOTHING;

    companion object {
        const val CHECK_DAYS = 7L

        /** Covers of issues outside the collection that are kept; the oldest beyond that go. */
        const val KEEP_BROWSED = 400

        /** The oldest of [items] beyond the [keep] newest. */
        fun <T> oldestBeyond(items: List<T>, keep: Int, checked: (T) -> LocalDate): List<T> =
            items.sortedBy(checked).dropLast(keep.coerceAtLeast(0))

        fun decide(fileExists: Boolean, row: CoverRow?, today: LocalDate): CoverPlan {
            val due = row == null || today > row.checked.plusDays(CHECK_DAYS)
            return when {
                // no image here: a recent "none on the server" stands; anything else asks
                !fileExists -> if (row != null && !row.hasImage && !due) NOTHING else DOWNLOAD
                row == null || !row.hasImage -> LINK
                due -> RECHECK
                else -> NOTHING
            }
        }
    }
}
