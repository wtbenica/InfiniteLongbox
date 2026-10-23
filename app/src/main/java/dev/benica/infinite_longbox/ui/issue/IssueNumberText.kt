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

package dev.benica.infinite_longbox.ui.issue

/**
 * How an issue's number is shown under its cover. GCD's numbers are text, not numbers: most are
 * digits ("27"), but there are also "1A", "1/2", "-1", "[13]" (a number that isn't printed),
 * "[nn]" (no number at all), "Preview" and "3 [Section 2]".
 */
object IssueNumberText {
    /** [main] is the line to show; [detail] is a smaller line under it, or blank. */
    data class Label(val main: String, val detail: String = "")

    private val UNNUMBERED = Regex("""^\[\s*nn\s*(?:[-–:]\s*(.*?))?\s*]$""", RegexOption.IGNORE_CASE)
    private val NUMBERED = Regex("""^(?:-\s*)?(?:\[\s*)?\d""")

    fun of(raw: String?): Label {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return Label("")

        UNNUMBERED.matchEntire(text)?.let { return Label("Unnumbered", it.groupValues[1]) }

        // A real number gets a "#"; a word ("Preview", "Annual 3") is shown as it is
        return if (NUMBERED.containsMatchIn(text)) Label("#$text") else Label(text)
    }

    /** An issue number with a second number in brackets after it, as Marvel's dual numbering has it. */
    data class Split(val main: String, val alt: String? = null)

    private val DUAL = Regex("""^(\S+)\s*\((\d+)\)$""")

    /** "32 (726)" is two numbers: the line shows 32 and a smaller 726 under it, not both squeezed together. */
    fun split(raw: String?): Split {
        val text = raw?.trim().orEmpty()
        val match = DUAL.matchEntire(text) ?: return Split(text)
        return Split(match.groupValues[1], match.groupValues[2])
    }

    private val PLAIN_NUMBER = Regex("""^\d+$""")

    /**
     * The line under an issue's title in the toolbar. A series of one issue says what it is
     * ("One-shot", "Graphic novel"), because "#1" tells a reader nothing; a limited series that has
     * finished says how long it ran ("#2 (of 4)"); anything else is its number.
     *
     * @param format the series' publishing format as GCD has it ("limited series", "one-shot")
     * @param issueCount how many issues the series has, once it has ended
     * @param ended whether the series has an end date; a running series has no "of"
     */
    fun subtitle(raw: String?, format: String?, issueCount: Int, ended: Boolean): String? {
        val kind = format?.trim().orEmpty()
        if (issueCount == 1 && kind.isNotEmpty()) {
            return kind.replaceFirstChar { it.uppercase() }
        }

        val label = of(raw)
        if (label.main.isEmpty()) return null

        val number = raw?.trim().orEmpty()
        val limited = kind.contains("limited", ignoreCase = true) || kind.contains("mini", ignoreCase = true)
        val main = if (limited && ended && issueCount >= 2 && PLAIN_NUMBER.matches(number) &&
            number.toInt() in 1..issueCount
        ) "${label.main} (of $issueCount)" else label.main

        return listOf(main, label.detail).filter { it.isNotEmpty() }.joinToString(" · ")
    }
}
