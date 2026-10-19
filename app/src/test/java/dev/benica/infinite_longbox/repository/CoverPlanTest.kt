package dev.benica.infinite_longbox.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CoverPlanTest {
    private val today = LocalDate.of(2026, 10, 19)
    private fun daysAgo(n: Long) = today.minusDays(n)

    @Test
    fun anIssueNobodyHasLookedAtIsDownloaded() {
        assertEquals(CoverPlan.DOWNLOAD, CoverPlan.decide(false, null, today))
    }

    @Test
    fun anIssueKnownToHaveNoCoverIsNotAskedAboutAgainForAWeek() {
        assertEquals(CoverPlan.NOTHING, CoverPlan.decide(false, CoverRow(false, daysAgo(2)), today))
        assertEquals(CoverPlan.NOTHING, CoverPlan.decide(false, CoverRow(false, daysAgo(7)), today))
    }

    @Test
    fun andIsAskedAboutAgainAfterAWeek() {
        assertEquals(CoverPlan.DOWNLOAD, CoverPlan.decide(false, CoverRow(false, daysAgo(8)), today))
    }

    @Test
    fun aCoverWhoseFileWentMissingIsDownloadedAgain() {
        assertEquals(CoverPlan.DOWNLOAD, CoverPlan.decide(false, CoverRow(true, daysAgo(1)), today))
    }

    @Test
    fun aFileWithNoRecordOfItIsLinked() {
        assertEquals(CoverPlan.LINK, CoverPlan.decide(true, null, today))
        assertEquals(CoverPlan.LINK, CoverPlan.decide(true, CoverRow(false, daysAgo(1)), today))
    }

    @Test
    fun aCoverCheckedThisWeekIsLeftAlone() {
        assertEquals(CoverPlan.NOTHING, CoverPlan.decide(true, CoverRow(true, daysAgo(3)), today))
    }

    @Test
    fun aCoverNotCheckedForAWeekIsAskedAboutWithoutDownloadingItAgain() {
        assertEquals(CoverPlan.RECHECK, CoverPlan.decide(true, CoverRow(true, daysAgo(9)), today))
    }

    @Test
    fun theOldestBrowsedCoversBeyondTheLimitAreTheOnesToTrim() {
        val covers = listOf("a" to daysAgo(1), "b" to daysAgo(5), "c" to daysAgo(3), "d" to daysAgo(9))
        assertEquals(listOf("d", "b"), CoverPlan.oldestBeyond(covers, 2) { it.second }.map { it.first })
    }

    @Test
    fun nothingIsTrimmedWhileUnderTheLimit() {
        val covers = listOf("a" to daysAgo(1), "b" to daysAgo(5))
        assertEquals(emptyList<Pair<String, LocalDate>>(), CoverPlan.oldestBeyond(covers, 2) { it.second })
        assertEquals(emptyList<Pair<String, LocalDate>>(), CoverPlan.oldestBeyond(covers, 10) { it.second })
    }
}
