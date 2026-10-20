package dev.benica.infinite_longbox.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class IssueNumberTextTest {
    private fun main(raw: String?) = IssueNumberText.of(raw).main

    @Test
    fun aPlainNumberGetsAHash() = assertEquals("#27", main("27"))

    @Test
    fun numbersWithSuffixesFractionsAndSignsGetAHash() {
        assertEquals("#1A", main("1A"))
        assertEquals("#1/2", main("1/2"))
        assertEquals("#-1", main("-1"))
        assertEquals("#1.MU", main("1.MU"))
        assertEquals("#1 Pre-Order Edition", main("1 Pre-Order Edition"))
        assertEquals("#3 [Section 2]", main("3 [Section 2]"))
    }

    @Test
    fun aNumberThatIsNotPrintedKeepsItsBrackets() = assertEquals("#[13]", main("[13]"))

    @Test
    fun wordsAreShownAsTheyAre() {
        assertEquals("Preview", main("Preview"))
        assertEquals("Annual 3", main("Annual 3"))
        assertEquals("∞ [Infinity]", main("∞ [Infinity]"))
    }

    @Test
    fun noNumberIsUnnumbered() {
        assertEquals(IssueNumberText.Label("Unnumbered"), IssueNumberText.of("[nn]"))
        assertEquals(IssueNumberText.Label("Unnumbered"), IssueNumberText.of("[NN]"))
    }

    @Test
    fun theTextAfterNnBecomesTheDetail() {
        assertEquals(
            IssueNumberText.Label("Unnumbered", "Nox'Em All Shoes"),
            IssueNumberText.of("[nn - Nox'Em All Shoes]"),
        )
        assertEquals(
            IssueNumberText.Label("Unnumbered", "T. Hersom / \"Best\" Soap"),
            IssueNumberText.of("[nn - T. Hersom / \"Best\" Soap]"),
        )
    }

    @Test
    fun missingOrBlankIsNothing() {
        assertEquals(IssueNumberText.Label(""), IssueNumberText.of(null))
        assertEquals(IssueNumberText.Label(""), IssueNumberText.of("  "))
    }

    @Test
    fun surroundingSpacesAreIgnored() = assertEquals("#27", main(" 27 "))

    private fun subtitle(raw: String?, format: String? = null, count: Int = 12, ended: Boolean = false) =
        IssueNumberText.subtitle(raw, format, count, ended)

    @Test
    fun anOrdinaryIssueIsItsNumber() = assertEquals("#27", subtitle("27", "ongoing series", 300))

    @Test
    fun aSeriesOfOneIssueSaysWhatItIs() {
        assertEquals("One-shot", subtitle("1", "one-shot", 1, true))
        assertEquals("Graphic novel", subtitle("1", "graphic novel", 1, true))
        assertEquals("One-shot; giveaway", subtitle("1", "one-shot; giveaway", 1, true))
    }

    @Test
    fun aSeriesOfOneIssueWithNoFormatIsJustItsNumber() = assertEquals("#1", subtitle("1", "", 1, true))

    @Test
    fun aFinishedLimitedSeriesSaysHowLongItRan() {
        assertEquals("#2 (of 4)", subtitle("2", "limited series", 4, true))
        assertEquals("#1 (of 4)", subtitle("1", "miniseries", 4, true))
        assertEquals("#3 (of 6)", subtitle("3", "mini-series", 6, true))
    }

    @Test
    fun aLimitedSeriesStillRunningHasNoOf() = assertEquals("#2", subtitle("2", "limited series", 4, false))

    @Test
    fun noOfForNumbersThatAreNotPlainOrAreBeyondTheCount() {
        assertEquals("#1A", subtitle("1A", "limited series", 4, true))
        assertEquals("#0", subtitle("0", "limited series", 4, true))
        assertEquals("#7", subtitle("7", "limited series", 4, true))
        assertEquals("Preview", subtitle("Preview", "limited series", 4, true))
    }

    @Test
    fun unnumberedKeepsItsDetailInTheSubtitle() =
        assertEquals("Unnumbered · Nox'Em All Shoes", subtitle("[nn - Nox'Em All Shoes]", "ongoing series", 3))

    @Test
    fun noNumberAndNotAOneShotIsNothing() = assertEquals(null, subtitle(null, "ongoing series", 3))
}
