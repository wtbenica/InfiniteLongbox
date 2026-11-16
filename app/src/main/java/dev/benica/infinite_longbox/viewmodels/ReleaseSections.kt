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

/** A publisher with issues in the month on show; [count] is 0 for one picked that has none this month. */
data class PublisherOption(val id: Int, val name: String, val count: Int)

/** A month as the screen shows it: [sections] of the picked publishers (all when none are), and the publishers to pick from. */
data class ReleasesView(val sections: List<ReleaseSection>, val publishers: List<PublisherOption>)

fun releasesView(rows: List<ReleaseRow>, picked: Set<Int>): ReleasesView {
    val names = rows.associate { it.publisherId to (it.publisher ?: "") }
    val counts = rows.groupingBy { it.publisherId }.eachCount()
    val options = (counts.keys + picked).map { PublisherOption(it, names[it].orEmpty(), counts[it] ?: 0) }
        .sortedWith(compareByDescending<PublisherOption> { it.count }.thenBy { it.name.lowercase() })
    val shown = if (picked.isEmpty()) rows else rows.filter { it.publisherId in picked }
    return ReleasesView(groupReleases(shown), options)
}
