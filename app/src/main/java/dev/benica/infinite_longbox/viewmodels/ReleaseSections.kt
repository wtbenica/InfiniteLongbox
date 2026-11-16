package dev.benica.infinite_longbox.viewmodels

import dev.benica.infinite_longbox.database.daos.ReleaseRow
import dev.benica.infinite_longbox.database.models.ReleasePrecision
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Issues of a month under one heading: a week (starting [weekStart]), or null for those with no exact day. */
data class ReleaseSection(val weekStart: LocalDate?, val rows: List<ReleaseRow>)

private val byTitle = compareBy<ReleaseRow>({ it.sortName.lowercase() }, { it.number.takeWhile { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }, { it.number }, { it.issueId })

/** Issues with an exact day go under their week; the rest follow together, as they can't be placed more closely. Each group is in title order. */
fun groupReleases(rows: List<ReleaseRow>): List<ReleaseSection> {
    val (exact, loose) = rows.partition { it.precision == ReleasePrecision.DAY }
    val weeks = exact.groupBy { it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) }
        .toSortedMap()
        .map { (start, inWeek) -> ReleaseSection(start, inWeek.sortedWith(byTitle)) }
    return if (loose.isEmpty()) weeks else weeks + ReleaseSection(null, loose.sortedWith(byTitle))
}
