package dev.benica.infinite_longbox.ui.issue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppearanceTextTest {
    @Test
    fun aRoleAndADetailAreSaidTogetherAfterTheBar() {
        assertEquals("supporting; first appearance", appearanceText("supporting", "first appearance"))
    }

    @Test
    fun eitherOneAloneIsEnough() {
        assertEquals("supporting", appearanceText("supporting", ""))
        assertEquals("first appearance", appearanceText(null, "first appearance"))
    }

    @Test
    fun nothingSaidMeansNoBarAtAll() {
        assertNull(appearanceText(null, null))
        assertNull(appearanceText("", ""))
        assertNull(appearanceText("  ", null))
    }
}
