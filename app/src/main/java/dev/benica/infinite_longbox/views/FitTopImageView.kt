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
import android.net.Uri
import java.io.File
import android.util.AttributeSet


class FitTopImageView(
    context: Context,
    attrs: AttributeSet
) : androidx.appcompat.widget.AppCompatImageView(context, attrs) {

    init {
        scaleType = ScaleType.MATRIX
    }
    private var shownUri: Uri? = null
    private var shownFileStamp = 0L

    override fun setImageURI(uri: Uri?) {
        // See CoverImageView: a replaced cover keeps its file name, so force a reload
        val stamp = uri?.path?.let { File(it).lastModified() } ?: 0L
        if (uri != null && uri == shownUri && stamp != shownFileStamp) super.setImageURI(null)
        shownUri = uri
        shownFileStamp = stamp

        super.setImageURI(uri)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        calculateMatrix()
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        calculateMatrix()
        return super.setFrame(l, t, r, b)
    }

    private fun calculateMatrix() {
        val drawable = drawable ?: return
        val matrix = imageMatrix
        val scale: Float
        val viewWidth = width - paddingLeft - paddingRight
        val viewHeight = height - paddingTop - paddingBottom
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        scale = if (drawableWidth * viewHeight > drawableHeight * viewWidth) {
            viewHeight.toFloat() / drawableHeight.toFloat()
        } else {
            viewWidth.toFloat() / drawableWidth.toFloat()
        }
        matrix.setScale(scale, scale)
        imageMatrix = matrix
    }
}