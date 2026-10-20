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

import java.time.LocalDate

/** How a series' run is written: "1999 - 2009", "2026 - present", or just "1999" when it ended in a year GCD doesn't know. */
object SeriesDates {
    fun range(start: LocalDate?, end: LocalDate?, publishingFormat: String?): String {
        val from = start?.year ?: return ""
        return when {
            end != null -> "$from - ${end.year}"
            // "was ongoing series": it stopped, but GCD has no year for it
            publishingFormat?.trim()?.startsWith("was", ignoreCase = true) == true -> "$from"
            else -> "$from - present"
        }
    }
}
