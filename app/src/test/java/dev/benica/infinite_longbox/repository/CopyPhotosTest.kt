package dev.benica.infinite_longbox.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyPhotosTest {
    @Test
    fun aPhotoIsNamedForItsIssueAndCollection() {
        assertEquals("12_1_1700000000000.jpg", copyPhotoName(12, 1, 1_700_000_000_000))
    }

    @Test
    fun aPhotoBelongsOnlyToItsOwnIssue() {
        val name = copyPhotoName(12, 1, 5)

        assertTrue(isCopyPhoto(name, 12, 1))
        // 120 starts with 12, but is another issue
        assertFalse(isCopyPhoto(copyPhotoName(120, 1, 5), 12, 1))
        assertFalse(isCopyPhoto(name, 12, 2))
        assertFalse(isCopyPhoto("12_1_5.png", 12, 1))
    }

    @Test
    fun aPictureIsShrunkInWholeStepsToTheLimit() {
        assertEquals(1, photoSampleSize(1200, 1600))
        assertEquals(1, photoSampleSize(4000, 2399))
        assertEquals(2, photoSampleSize(4800, 3600))
        assertEquals(4, photoSampleSize(9600, 4000))
    }
}
