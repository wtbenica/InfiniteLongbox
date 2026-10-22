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

package dev.benica.infinite_longbox.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

/**
 * Picks which part of a cover the series card shows: the top, where logos usually are, unless
 * the top is nearly blank (a white background with the art lower down), then the middle.
 */
object CoverFocus {
    /** The top band checked for content, as a fraction of the cover's height. */
    private const val TOP_BAND = 0.30f

    /** Fraction of the band's pixels on a sharp edge, below which it counts as blank. */
    private const val BLANK_EDGE_DENSITY = 0.02f

    private const val EDGE_STEP = 40
    private const val SAMPLE_WIDTH = 200

    private val cache = LruCache<String, Alignment>(512)

    /** The alignment if it's already been worked out, so a card can start with the right one. */
    fun peek(file: File): Alignment? = cache.get("${file.path}@${file.lastModified()}")

    suspend fun alignmentFor(file: File): Alignment = withContext(Dispatchers.Default) {
        val key = "${file.path}@${file.lastModified()}"
        cache.get(key) ?: compute(file).also { cache.put(key, it) }
    }

    private fun compute(file: File): Alignment {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return Alignment.TopStart

        val options = BitmapFactory.Options().apply {
            inSampleSize = (bounds.outWidth / SAMPLE_WIDTH).coerceAtLeast(1)
        }
        val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return Alignment.TopStart
        val blank = topIsBlank(bitmap)
        bitmap.recycle()
        return if (blank) Alignment.CenterStart else Alignment.TopStart
    }

    private fun topIsBlank(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = (bitmap.height * TOP_BAND).toInt()
        if (w < 2 || h < 1) return false

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val luma = IntArray(pixels.size) {
            val p = pixels[it]
            (((p shr 16) and 0xFF) * 299 + ((p shr 8) and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
        }

        var edges = 0
        for (y in 0 until h) {
            for (x in 1 until w) {
                if (abs(luma[y * w + x] - luma[y * w + x - 1]) > EDGE_STEP) edges++
            }
        }
        return edges.toFloat() / (w * h) < BLANK_EDGE_DENSITY
    }
}
