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

import dev.benica.infinite_longbox.ui.IssueNumberText
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/** The words the character page puts around its data. */
object CharacterInfoText {
    /** "Suicide Squad #23 (Jan 1989)"; the date as far as the issue has one. */
    fun issue(issue: IssueRef, locale: Locale = Locale.getDefault()): String {
        // "#27" for a number, the word for "Preview", nothing for an issue with no number
        val number = IssueNumberText.of(issue.number)
            .takeIf { it.main.isNotEmpty() && it.main != "Unnumbered" }
            ?.let { " ${it.main}" }.orEmpty()
        val date = date(issue.keyDate, locale)
        return issue.seriesName + number + if (date.isEmpty()) "" else " ($date)"
    }

    /** "Jan 1989" from a key date like 1989-01-00 (a month of 00 means GCD doesn't know it). */
    fun date(keyDate: String, locale: Locale = Locale.getDefault()): String {
        val year = keyDate.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) } ?: return ""
        val month = keyDate.drop(5).take(2).toIntOrNull()?.takeIf { it in 1..12 } ?: return year
        return Month.of(month).getDisplayName(TextStyle.SHORT, locale) + " " + year
    }

    /** "1996 – present", "1960 – 1970", "1996" or "until 1970"; blank when neither year is known. */
    fun years(joined: Int?, left: Int?): String = when {
        joined == null && left == null -> ""
        left == null -> "$joined – present"
        joined == null -> "until $left"
        joined == left -> "$joined"
        else -> "$joined – $left"
    }

    /** "Barbara Gordon", or "Barbara Gordon (Batgirl)" when it has an alter ego. */
    fun name(name: String, alterEgo: String): String =
        if (alterEgo.isBlank()) name else "$name ($alterEgo)"

    /** "2,270 appearances in 1,320 issues". */
    fun appearances(appearances: Int, issues: Int, locale: Locale = Locale.getDefault()): String {
        val a = String.format(locale, "%,d", appearances)
        val i = String.format(locale, "%,d", issues)
        return "$a appearance${if (appearances == 1) "" else "s"} in $i issue${if (issues == 1) "" else "s"}"
    }

    /** "18,400 credits in 9,200 issues". */
    fun credits(credits: Int, issues: Int, locale: Locale = Locale.getDefault()): String {
        val c = String.format(locale, "%,d", credits)
        val i = String.format(locale, "%,d", issues)
        return "$c credit${if (credits == 1) "" else "s"} in $i issue${if (issues == 1) "" else "s"}"
    }
}
