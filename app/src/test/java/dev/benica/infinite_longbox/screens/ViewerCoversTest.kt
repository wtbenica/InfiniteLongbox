package dev.benica.infinite_longbox.screens

import dev.benica.infinite_longbox.ui.covers.ViewerCover
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerCoversTest {
    private fun cover(id: Int, name: String = "") = ViewerCover(id, "#$id", name, null, "key$id")

    // Issue 10 and its variant 11 are in the collection, then issue 20
    private val stops = listOf(cover(10), cover(11, "B"), cover(20))
    private val variantOf = listOf<Int?>(null, 10, null)

    @Test
    fun aCoverThatIsAStopOpensWhereItIs() {
        val (covers, start) = viewerCovers(stops, variantOf, here = 1, shown = cover(11, "B"), shownIssue = 10)

        assertEquals(stops, covers)
        assertEquals(1, start)
    }

    @Test
    fun aVariantOutsideTheCollectionIsPutInAfterItsIssueAndOpensFirst() {
        val outside = cover(12, "C")
        val (covers, start) = viewerCovers(stops, variantOf, here = 0, shown = outside, shownIssue = 10)

        assertEquals(listOf(10, 11, 12, 20), covers.map { it.issueId })
        assertEquals(2, start)
        assertEquals(outside, covers[start])
    }

    @Test
    fun theCoverOnThePageReplacesItsStopSoItIsTheFreshestOne() {
        val fresher = ViewerCover(20, "#20", "", java.io.File("c.jpg"), "key20")
        val (covers, start) = viewerCovers(stops, variantOf, here = 2, shown = fresher, shownIssue = 20)

        assertEquals(fresher, covers[2])
        assertEquals(2, start)
    }

    @Test
    fun aVariantOfAnIssueWithNoStopOpensNextToThePlaceOnThePage() {
        val (covers, start) = viewerCovers(stops, variantOf, here = 2, shown = cover(31, "B"), shownIssue = 30)

        assertEquals(listOf(10, 11, 20, 31), covers.map { it.issueId })
        assertEquals(3, start)
    }
}
