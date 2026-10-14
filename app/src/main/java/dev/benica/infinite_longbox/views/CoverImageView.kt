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


package dev.benica.infinite_longbox.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import java.io.File
import kotlin.math.max

/**
 * Shows a comic cover in a fixed-size box: scaled to fill the box and aligned to the top, so there
 * are never gaps. Portrait covers fill the width (the bottom is cropped; the label that overlaps
 * the card's bottom hides it).
 *
 * Wider-than-tall covers (wraparound and gatefold) are filled by height instead and aligned to the
 * right, which shows the front cover: on a wraparound the front is the right half.
 *
 * The matrix is recalculated whenever the image or the size changes. (FitTopImageView only did it
 * on layout, so an image set after layout was drawn with a stale matrix.)
 */
class CoverImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {
    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateMatrix()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        updateMatrix()
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        updateMatrix()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        updateMatrix()
    }

    private var shownUri: Uri? = null
    private var shownFileStamp = 0L

    override fun setImageURI(uri: Uri?) {
        // A replaced cover is saved to the same file, so the URI doesn't change, and ImageView
        // ignores a request to show the URI it already has. Clear it first when the file changed.
        val stamp = uri?.path?.let { File(it).lastModified() } ?: 0L
        if (uri != null && uri == shownUri && stamp != shownFileStamp) super.setImageURI(null)
        shownUri = uri
        shownFileStamp = stamp

        super.setImageURI(uri)
        updateMatrix()
    }

    private fun updateMatrix() {
        val drawable = drawable ?: return
        val viewWidth = (width - paddingLeft - paddingRight).toFloat()
        val viewHeight = (height - paddingTop - paddingBottom).toFloat()
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()
        if (viewWidth <= 0 || viewHeight <= 0 || drawableWidth <= 0 || drawableHeight <= 0) return

        val scale = max(viewWidth / drawableWidth, viewHeight / drawableHeight)
        val extraWidth = viewWidth - drawableWidth * scale // <= 0: how much is cropped sideways
        val isWide = drawableWidth / drawableHeight > WIDE_ASPECT_RATIO

        imageMatrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(if (isWide) extraWidth else extraWidth / 2f, 0f)
        }
    }

    companion object {
        /** Width/height above which a cover is treated as a wraparound or gatefold spread. */
        private const val WIDE_ASPECT_RATIO = 0.95f
    }
}
