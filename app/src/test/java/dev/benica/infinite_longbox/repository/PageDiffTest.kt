package dev.benica.infinite_longbox.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class PageDiffTest {
    @Test
    fun pagesAtTheServersVersionAreKeptAndOthersAreFetchedAgain() {
        val stale = PageDiff.stalePages("tttt", listOf("a", "b", "c", "d"), listOf("a", "X", "c", "d"))
        assertEquals("tftt", stale)
    }

    @Test
    fun nothingChangedMeansNothingToFetch() {
        assertEquals("ttt", PageDiff.stalePages("ttt", listOf("a", "b", "c"), listOf("a", "b", "c")))
    }

    @Test
    fun pagesTheServerHasThatThePhoneDoesNotAreFetched() {
        assertEquals("ttff", PageDiff.stalePages("tt", listOf("a", "b"), listOf("a", "b", "c", "d")))
    }

    @Test
    fun pagesTheServerNoLongerHasAreDropped() {
        assertEquals("tt", PageDiff.stalePages("tttt", listOf("a", "b", "c", "d"), listOf("a", "b")))
    }

    @Test
    fun aPageThePhoneDidNotFinishFetchingIsFetchedEvenIfItsVersionIsKnown() {
        assertEquals("tft", PageDiff.stalePages("tft", listOf("a", "b", "c"), listOf("a", "b", "c")))
    }

    @Test
    fun aPageWhoseVersionThePhoneDoesNotKnowIsFetched() {
        // e.g. a database from before versions were kept: everything is fetched once
        assertEquals("ff", PageDiff.stalePages("tt", emptyList(), listOf("a", "b")))
        assertEquals("tf", PageDiff.stalePages("tt", listOf("a", ""), listOf("a", "b")))
    }

    @Test
    fun whatWasFetchedIsRememberedAtTheVersionItHad() {
        val saved = listOf("a", "b", "c")
        assertEquals(listOf("a", "X", "c", "d"), PageDiff.remember(saved, 4, mapOf(1 to "X", 3 to "d")))
    }

    @Test
    fun remembering_forFewerPagesDropsTheRest() {
        assertEquals(listOf("a", "b"), PageDiff.remember(listOf("a", "b", "c"), 2, emptyMap()))
    }

    @Test
    fun theVersionsGoInAndOutOfThePhonesPreferences() {
        val hashes = listOf("abc123", "def456")
        assertEquals(hashes, PageDiff.parse(PageDiff.format(hashes)))
        assertEquals(emptyList<String>(), PageDiff.parse(null))
        assertEquals(emptyList<String>(), PageDiff.parse(""))
    }
}
