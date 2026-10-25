package dev.benica.infinite_longbox.ui.covers

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class FrontCropTest {
    private val card = Size(1000f, 300f)

    @Test
    fun aFrontCoverIsCroppedLikeAnyOther() {
        val front = Size(650f, 1000f)

        assertEquals(ContentScale.Crop.computeScaleFactor(front, card), FrontCrop.computeScaleFactor(front, card))
    }

    @Test
    fun aWraparoundIsScaledSoItsFrontHalfFillsTheWidth() {
        val wrap = Size(2000f, 1000f)
        val scale = FrontCrop.computeScaleFactor(wrap, card)

        // Half of 2000 pixels across 1000 pixels of card
        assertEquals(1f, scale.scaleX, 0.001f)
    }

    @Test
    fun aWraparoundStillCoversTheHeight() {
        val flat = Size(3000f, 400f)
        val scale = FrontCrop.computeScaleFactor(flat, card)

        assertEquals(card.height / flat.height, scale.scaleY, 0.001f)
    }

    @Test
    fun aPictureWiderThanTheCardSitsAgainstTheEnd() {
        val offset = FrontAlignment(top = true).align(IntSize(2000, 1000), IntSize(1000, 300), LayoutDirection.Ltr)

        assertEquals(IntOffset(-1000, 0), offset)
    }

    @Test
    fun aPictureTheWidthOfTheCardSitsAtTheStart() {
        val offset = FrontAlignment(top = false).align(IntSize(1000, 1500), IntSize(1000, 300), LayoutDirection.Ltr)

        assertEquals(IntOffset(0, -600), offset)
    }
}
