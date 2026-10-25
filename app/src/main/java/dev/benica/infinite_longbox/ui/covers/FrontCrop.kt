/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.benica.infinite_longbox.ui.covers

import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.max
import kotlin.math.roundToInt

/** Width over height above which a cover is a wraparound or gatefold spread, whose front is the right half. */
const val WIDE_COVER = 0.95f

/** Fills the card like [ContentScale.Crop]; a wraparound is scaled so its front half (the right) fills the width. */
object FrontCrop : ContentScale {
    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
        if (srcSize.width / srcSize.height <= WIDE_COVER) return ContentScale.Crop.computeScaleFactor(srcSize, dstSize)
        val scale = max(dstSize.width * 2 / srcSize.width, dstSize.height / srcSize.height)
        return ScaleFactor(scale, scale)
    }
}

/**
 * Where the picture sits in the card: against the end when it is wider than the card (the front of
 * a wraparound), at the start otherwise; [top] puts the top edge of the picture at the card's, else it is centred.
 */
class FrontAlignment(private val top: Boolean) : Alignment {
    override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset {
        val x = if (size.width > space.width) space.width - size.width else 0
        val y = if (top) 0 else ((space.height - size.height) / 2f).roundToInt()
        return IntOffset(x, y)
    }
}
