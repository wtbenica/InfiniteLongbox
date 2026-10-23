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

package dev.benica.infinite_longbox.ui.scan

/**
 * The frame on the scanner that the barcode (with its 5-digit add-on) is lined up in. The scanner
 * draws it and ignores a barcode that isn't inside it, so the wait for the add-on only starts once
 * the comic is in place.
 *
 * Everything is in fractions of the camera picture (0..1, left to right and top to bottom).
 */
object ScanWindow {
    /** A UPC with its add-on is about this much wider than it is tall. */
    private const val ASPECT = 2.4f
    private const val WIDTH = 0.84f
    private const val CENTER_Y = 0.42f

    /** How far outside the frame a barcode's centre may be, as a fraction of the frame's size. */
    private const val TOLERANCE = 0.25f

    class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        fun contains(x: Float, y: Float) = x in left..right && y in top..bottom
    }

    /** The frame, for a picture [viewAspect] (width / height) wide. */
    fun rect(viewAspect: Float): Rect {
        val height = WIDTH * viewAspect / ASPECT
        return Rect(
            left = (1f - WIDTH) / 2f,
            top = CENTER_Y - height / 2f,
            right = (1f + WIDTH) / 2f,
            bottom = CENTER_Y + height / 2f,
        )
    }

    /** Whether a barcode centred at ([x], [y]) is in the frame, give or take the tolerance. */
    fun accepts(x: Float, y: Float, viewAspect: Float): Boolean {
        val r = rect(viewAspect)
        val dx = (r.right - r.left) * TOLERANCE
        val dy = (r.bottom - r.top) * TOLERANCE
        return Rect(r.left - dx, r.top - dy, r.right + dx, r.bottom + dy).contains(x, y)
    }
}
