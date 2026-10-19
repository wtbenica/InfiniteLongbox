package dev.benica.infinite_longbox.fragments

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesCoverTest {
    @Test
    fun aSeriesWhoseFirstIssueHasNoCoverOnTheServerLooksAtItsOtherIssues() {
        assertTrue(shouldFetchSeriesCover(hasCover = false, firstIssueKnown = true, firstIssueCoverChecked = true))
    }

    @Test
    fun aSeriesWithNoFirstIssueAtAllLooksAtItsOtherIssues() {
        assertTrue(shouldFetchSeriesCover(hasCover = false, firstIssueKnown = false, firstIssueCoverChecked = false))
    }

    @Test
    fun whileTheFirstIssuesCoverIsStillComingItWaits() {
        assertFalse(shouldFetchSeriesCover(hasCover = false, firstIssueKnown = true, firstIssueCoverChecked = false))
    }

    @Test
    fun aCardThatHasACoverNeedsNothing() {
        assertFalse(shouldFetchSeriesCover(hasCover = true, firstIssueKnown = true, firstIssueCoverChecked = true))
        assertFalse(shouldFetchSeriesCover(hasCover = true, firstIssueKnown = false, firstIssueCoverChecked = false))
    }
}
