package dev.benica.infinite_longbox

import dev.benica.infinite_longbox.database.daos.ReleaseRow
import dev.benica.infinite_longbox.database.models.ReleasePrecision
import dev.benica.infinite_longbox.viewmodels.groupReleases
import dev.benica.infinite_longbox.viewmodels.releasesView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import java.time.LocalDate
import org.junit.Test

class ReleaseSectionsTest {
    private fun row(id: Int, date: String, precision: ReleasePrecision) =
        ReleaseRow(id, 1, "S", "s", 1, null, "1", LocalDate.parse(date), precision)

    @Test fun exactDaysGroupByWeekAndTheRestComeLast() {
        val sections = groupReleases(listOf(
            row(1, "1994-03-02", ReleasePrecision.DAY), // Wednesday
            row(2, "1994-03-05", ReleasePrecision.DAY), // Saturday, same Sunday-start week
            row(3, "1994-03-09", ReleasePrecision.DAY),
            row(4, "1994-03-01", ReleasePrecision.ESTIMATED),
            row(5, "1994-03-01", ReleasePrecision.MONTH),
        ))
        assertEquals(listOf(LocalDate.parse("1994-02-27"), LocalDate.parse("1994-03-06"), null), sections.map { it.weekStart })
        assertEquals(listOf(listOf(1, 2), listOf(3), listOf(4, 5)), sections.map { s -> s.rows.map { it.issueId } })
    }

    @Test fun noLooseIssuesMeansNoElsewhereSection() {
        assertNull(groupReleases(listOf(row(1, "1994-03-02", ReleasePrecision.DAY))).last().weekStart?.let { null } ?: null)
        assertEquals(1, groupReleases(listOf(row(1, "1994-03-02", ReleasePrecision.DAY))).size)
    }
}

class ReleaseOrderTest {
    private fun row(id: Int, sortName: String, number: String) =
        ReleaseRow(id, id, sortName, sortName, 1, null, number, LocalDate.parse("1994-03-02"), ReleasePrecision.DAY)

    @Test fun aWeekIsInTitleThenNumberOrder() {
        val rows = listOf(row(1, "Xtreme", "1"), row(2, "Batman", "10"), row(3, "Batman", "9"), row(4, "avengers", "2"))
        assertEquals(listOf(4, 3, 2, 1), groupReleases(rows).single().rows.map { it.issueId })
    }
}

class ReleasesViewTest {
    private fun row(id: Int, publisher: Int) =
        ReleaseRow(id, id, "S$id", "s$id", publisher, "P$publisher", "1", LocalDate.parse("1994-03-02"), ReleasePrecision.DAY)

    private val rows = listOf(row(1, 10), row(2, 10), row(3, 20))

    @Test fun nothingPickedShowsAllWithBiggestPublisherFirst() {
        val view = releasesView(rows, emptySet())
        assertEquals(listOf(1, 2, 3), view.sections.flatMap { s -> s.rows.map { it.issueId } })
        assertEquals(listOf(10 to 2, 20 to 1), view.publishers.map { it.id to it.count })
    }

    @Test fun pickedPublishersNarrowTheIssuesNotTheOptions() {
        val view = releasesView(rows, setOf(20))
        assertEquals(listOf(3), view.sections.flatMap { s -> s.rows.map { it.issueId } })
        assertEquals(2, view.publishers.size)
    }

    @Test fun aPickedPublisherWithNothingThisMonthStaysAnOption() {
        val view = releasesView(rows, setOf(99))
        assertEquals(emptyList<Any>(), view.sections)
        assertEquals(0, view.publishers.first { it.id == 99 }.count)
    }
}
