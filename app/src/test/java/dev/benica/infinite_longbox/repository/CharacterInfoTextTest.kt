package dev.benica.infinite_longbox.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CharacterInfoTextTest {
    private val us = Locale.US
    private fun issue(number: String = "23", keyDate: String = "1989-01-00") =
        IssueRef(1, 2, "Suicide Squad", number, keyDate)

    @Test
    fun anIssueIsNamedWithItsNumberAndDate() =
        assertEquals("Suicide Squad #23 (Jan 1989)", CharacterInfoText.issue(issue(), us))

    @Test
    fun anUnnumberedIssueHasNoNumberAndAWordIsKept() {
        assertEquals("Suicide Squad (Jan 1989)", CharacterInfoText.issue(issue(number = "[nn]"), us))
        assertEquals("Suicide Squad Preview (Jan 1989)", CharacterInfoText.issue(issue(number = "Preview"), us))
    }

    @Test
    fun anIssueWithoutADateOrNumberIsJustItsSeries() =
        assertEquals("Suicide Squad", CharacterInfoText.issue(issue(number = "", keyDate = ""), us))

    @Test
    fun aMonthOfZeroMeansOnlyTheYearIsKnown() {
        assertEquals("1989", CharacterInfoText.date("1989-00-00", us))
        assertEquals("1989", CharacterInfoText.date("1989", us))
    }

    @Test
    fun nonsenseDatesAreBlank() {
        assertEquals("", CharacterInfoText.date("", us))
        assertEquals("", CharacterInfoText.date("n/a", us))
    }

    @Test
    fun yearsOfMembership() {
        assertEquals("1996 – present", CharacterInfoText.years(1996, null))
        assertEquals("1960 – 1970", CharacterInfoText.years(1960, 1970))
        assertEquals("1996", CharacterInfoText.years(1996, 1996))
        assertEquals("until 1970", CharacterInfoText.years(null, 1970))
        assertEquals("", CharacterInfoText.years(null, null))
    }

    @Test
    fun namesWithAndWithoutAnAlterEgo() {
        assertEquals("Batgirl", CharacterInfoText.name("Batgirl", ""))
        assertEquals("Batgirl (Barbara Gordon)", CharacterInfoText.name("Batgirl", "Barbara Gordon"))
    }

    @Test
    fun appearanceCountsAreGroupedAndPluralised() {
        assertEquals("2,270 appearances in 1,320 issues", CharacterInfoText.appearances(2270, 1320, us))
        assertEquals("1 appearance in 1 issue", CharacterInfoText.appearances(1, 1, us))
    }

    @Test
    fun creditCountsAreGroupedAndPluralised() {
        assertEquals("18,400 credits in 9,200 issues", CharacterInfoText.credits(18400, 9200, us))
        assertEquals("1 credit in 1 issue", CharacterInfoText.credits(1, 1, us))
    }
}
