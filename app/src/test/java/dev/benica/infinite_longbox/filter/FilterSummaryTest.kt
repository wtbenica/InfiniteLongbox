package dev.benica.infinite_longbox.filter

import org.junit.Assert.assertEquals
import org.junit.Test

class FilterSummaryTest {
    private fun summary(
        myCollection: Boolean = false,
        series: String? = null,
        character: String? = null,
        creators: List<String> = emptyList(),
        publishers: List<String> = emptyList(),
        startYear: Int? = null,
        endYear: Int? = null,
        text: String? = null,
    ) = FilterSummary.of(
        myCollection, "My Collection", series, character, creators, publishers, startYear, endYear, text,
    )

    private fun missing(cover: Boolean, storage: Boolean, myCollection: Boolean = true) = FilterSummary.of(
        myCollection, "My Collection", missingCover = cover, missingStorage = storage,
    )

    @Test
    fun whatIsMissingFollowsTheCollection() {
        assertEquals("My Collection · No cover", missing(cover = true, storage = false))
        assertEquals("My Collection · No cover · Not stored", missing(cover = true, storage = true))
    }

    @Test
    fun whatIsMissingIsLeftOutOfALisWithoutTheCollection() =
        assertEquals("", missing(cover = true, storage = true, myCollection = false))

    @Test
    fun noFiltersIsBlank() = assertEquals("", summary())

    @Test
    fun theCollectionComesFirst() =
        assertEquals("My Collection · Oracle", summary(myCollection = true, character = "Oracle"))

    @Test
    fun everythingTogether() = assertEquals(
        "My Collection · Doom Patrol · Negative Man · Rachel Pollack · DC · 1990–1999 · \"robot\"",
        summary(
            myCollection = true, series = "Doom Patrol", character = "Negative Man",
            creators = listOf("Rachel Pollack"), publishers = listOf("DC"),
            startYear = 1990, endYear = 1999, text = "robot",
        ),
    )

    @Test
    fun twoCreatorsAreNamedAndMoreAreCounted() {
        assertEquals("Jack Kirby, Stan Lee", summary(creators = listOf("Stan Lee", "Jack Kirby")))
        assertEquals("3 creators", summary(creators = listOf("A", "B", "C")))
    }

    @Test
    fun publishersAreCountedToo() =
        assertEquals("3 publishers", summary(publishers = listOf("A", "B", "C")))

    @Test
    fun oneSidedAndSingleYearRanges() {
        assertEquals("from 1990", summary(startYear = 1990))
        assertEquals("until 1999", summary(endYear = 1999))
        assertEquals("1990", summary(startYear = 1990, endYear = 1990))
    }

    @Test
    fun blankNamesAreIgnored() =
        assertEquals("", summary(series = " ", character = "", creators = listOf(""), text = " "))
}
