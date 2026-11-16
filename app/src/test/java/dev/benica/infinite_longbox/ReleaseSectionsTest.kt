package dev.benica.infinite_longbox

import dev.benica.infinite_longbox.database.daos.ReleaseRow
import dev.benica.infinite_longbox.database.models.ReleasePrecision
import dev.benica.infinite_longbox.viewmodels.groupReleases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import java.time.LocalDate
import org.junit.Test

class ReleaseSectionsTest {
    private fun row(id: Int, date: String, precision: ReleasePrecision) =
        ReleaseRow(id, 1, "S", "s", null, "1", LocalDate.parse(date), precision)

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
        ReleaseRow(id, id, sortName, sortName, null, number, LocalDate.parse("1994-03-02"), ReleasePrecision.DAY)

    @Test fun aWeekIsInTitleThenNumberOrder() {
        val rows = listOf(row(1, "Xtreme", "1"), row(2, "Batman", "10"), row(3, "Batman", "9"), row(4, "avengers", "2"))
        assertEquals(listOf(4, 3, 2, 1), groupReleases(rows).single().rows.map { it.issueId })
    }
}
