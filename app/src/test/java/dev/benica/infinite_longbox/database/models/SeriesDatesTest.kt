package dev.benica.infinite_longbox.database.models

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SeriesDatesTest {
    private fun year(y: Int) = LocalDate.of(y, 1, 1)

    @Test
    fun aFinishedRunIsItsTwoYears() =
        assertEquals("1999 - 2009", SeriesDates.range(year(1999), year(2009), "was ongoing series"))

    @Test
    fun aSingleYearRunRepeatsTheYear() =
        assertEquals("2011 - 2011", SeriesDates.range(year(2011), year(2011), "one-shot"))

    @Test
    fun aRunWithNoEndYearIsPresent() {
        assertEquals("2026 - present", SeriesDates.range(year(2026), null, "limited series"))
        assertEquals("1990 - present", SeriesDates.range(year(1990), null, "ongoing series"))
        assertEquals("1990 - present", SeriesDates.range(year(1990), null, null))
    }

    @Test
    fun aRunThatStoppedInAnUnknownYearIsJustItsStart() {
        assertEquals("1999", SeriesDates.range(year(1999), null, "was ongoing series"))
        assertEquals("1999", SeriesDates.range(year(1999), null, "Was ongoing"))
    }

    @Test
    fun noStartNoRange() = assertEquals("", SeriesDates.range(null, year(2000), "ongoing series"))
}
