package dev.benica.infinite_longbox.ui.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanWindowTest {
    private val aspect = 1280f / 2856f

    @Test
    fun theFrameIsCenteredAcrossAndWiderThanTall() {
        val r = ScanWindow.rect(aspect)
        assertEquals(0.5f, (r.left + r.right) / 2f, 0.001f)
        // in the picture's own proportions, not the fractions'
        assertEquals(2.4f, ((r.right - r.left) * aspect) / (r.bottom - r.top), 0.01f)
    }

    @Test
    fun aBarcodeInTheFrameIsAccepted() {
        val r = ScanWindow.rect(aspect)
        assertTrue(ScanWindow.accepts((r.left + r.right) / 2, (r.top + r.bottom) / 2, aspect))
    }

    @Test
    fun aBarcodeJustOutsideIsStillAcceptedAndOneFarAwayIsNot() {
        val r = ScanWindow.rect(aspect)
        val nudge = (r.bottom - r.top) * 0.1f
        assertTrue(ScanWindow.accepts(0.5f, r.bottom + nudge, aspect))
        assertFalse(ScanWindow.accepts(0.5f, 0.9f, aspect))
        assertFalse(ScanWindow.accepts(0.5f, 0.05f, aspect))
    }
}
