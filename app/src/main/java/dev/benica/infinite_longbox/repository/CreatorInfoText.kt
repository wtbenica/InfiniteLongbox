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

import java.util.Locale

/** The words the creator page puts around its data. */
object CreatorInfoText {
    /** "Aug 28, 1917 · New York"; whichever part there is. */
    fun lifeEvent(event: LifeEvent): String =
        listOf(event.date, event.place).filter { it.isNotBlank() }.joinToString(" · ")

    /** "1936 – 1993" from the years of the first and latest credit; one year if they are the same. */
    fun active(first: IssueRef?, latest: IssueRef?): String {
        val from = first?.year
        val to = latest?.year ?: from
        return when {
            from == null && to == null -> ""
            from == null -> "$to"
            to == null || from == to -> "$from"
            else -> "$from – $to"
        }
    }

    /** "Penciler 1,200 · Inker 900". */
    fun roles(roles: List<RoleCount>, locale: Locale = Locale.getDefault()): String =
        roles.joinToString(" · ") { "${it.role} ${String.format(locale, "%,d", it.count)}" }

    /** "Teddy (pen name)" or just the name. */
    fun otherName(name: OtherName): String =
        if (name.kind.isBlank()) name.name else "${name.name} (${name.kind})"

    /** "Fantastic Four · 102 issues". */
    fun series(series: SeriesCount, locale: Locale = Locale.getDefault()): String =
        "${series.seriesName} · ${String.format(locale, "%,d", series.issues)} issue${if (series.issues == 1) "" else "s"}"
}
