package dev.benica.infinite_longbox.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CreatorInfoTextTest {
    private fun issue(year: Int?) = IssueRef(1, 2, "X", "1", "", year)

    @Test
    fun aLifeEventIsItsDateAndPlace() {
        assertEquals("Aug 28, 1917 · New York", CreatorInfoText.lifeEvent(LifeEvent("Aug 28, 1917", "New York")))
        assertEquals("1917", CreatorInfoText.lifeEvent(LifeEvent("1917", "")))
        assertEquals("New York", CreatorInfoText.lifeEvent(LifeEvent("", "New York")))
        assertEquals("", CreatorInfoText.lifeEvent(LifeEvent()))
    }

    @Test
    fun activeYearsRunFromTheFirstCreditToTheLatest() {
        assertEquals("1936 – 1993", CreatorInfoText.active(issue(1936), issue(1993)))
        assertEquals("1936", CreatorInfoText.active(issue(1936), issue(1936)))
        assertEquals("1936", CreatorInfoText.active(issue(1936), null))
        assertEquals("1993", CreatorInfoText.active(null, issue(1993)))
        assertEquals("", CreatorInfoText.active(null, null))
        assertEquals("", CreatorInfoText.active(issue(null), issue(null)))
    }

    @Test
    fun rolesAreListedWithGroupedCounts() =
        assertEquals(
            "Penciler 1,200 · Inker 900",
            CreatorInfoText.roles(listOf(RoleCount("Penciler", 1200), RoleCount("Inker", 900)), Locale.US),
        )

    @Test
    fun otherNamesShowTheirKind() {
        assertEquals("Teddy (pen name)", CreatorInfoText.otherName(OtherName("Teddy", "pen name")))
        assertEquals("J. Kirby", CreatorInfoText.otherName(OtherName("J. Kirby")))
    }

    @Test
    fun seriesShowTheirIssueCount() {
        assertEquals("Thor · 1,102 issues", CreatorInfoText.series(SeriesCount(1, "Thor", 1102), Locale.US))
        assertEquals("Thor · 1 issue", CreatorInfoText.series(SeriesCount(1, "Thor", 1), Locale.US))
    }
}
