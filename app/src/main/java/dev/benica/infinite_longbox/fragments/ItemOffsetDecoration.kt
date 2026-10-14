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

package dev.benica.infinite_longbox.fragments

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ItemOffsetDecoration(itemOffset: Int, itemOffsetHorizontal: Int? = null, numCols: Int = 1) :
    RecyclerView.ItemDecoration() {
    private var mItemOffset = itemOffset
    private var mItemOffsetHorizontal = itemOffsetHorizontal
    private var mNumCols = numCols

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        super.getItemOffsets(outRect, view, parent, state)

//        outRect.top = mItemOffset
//        outRect.bottom = mItemOffset
//        outRect.left = mItemOffsetHorizontal ?: mItemOffset
//        outRect.right = mItemOffsetHorizontal ?: mItemOffset

        val childAdapterPosition = parent.getChildAdapterPosition(view)
        val itemCount = parent.adapter?.itemCount ?: 1

        val topDivisor = if (childAdapterPosition < mNumCols) 1 else 2
        val bottomDivisor = if (childAdapterPosition >= itemCount / mNumCols * mNumCols) 1 else 2

        // Every item in a row must end up exactly the same width, or (covers keep their aspect
        // ratio) the same row ends up with cards of different heights.
        //
        // GridLayoutManager splits the width into spans, giving the leftover pixels to some of
        // them (see calculateItemBorders), and the offsets below are subtracted from each span.
        // So: use the same total offset for every column (outer margin h, gaps ~h), then give the
        // wider spans their extra pixel as extra offset.
        val h = mItemOffsetHorizontal ?: mItemOffset
        val column = if (childAdapterPosition >= 0) childAdapterPosition % mNumCols else 0
        val total = parent.width - parent.paddingLeft - parent.paddingRight
        val step = h / mNumCols
        val left = h - column * step

        val right = if (total > 0) {
            val spans = spanWidths(total, mNumCols)
            val itemWidth = spans.min() - (h + step)
            spans[column] - itemWidth - left
        } else {
            h + step - left
        }

        outRect.top = mItemOffset / topDivisor
        outRect.bottom = mItemOffset / bottomDivisor
        outRect.left = left
        outRect.right = right
    }

    /** Span widths exactly as GridLayoutManager.calculateItemBorders computes them. */
    private fun spanWidths(total: Int, spanCount: Int): IntArray {
        val widths = IntArray(spanCount)
        val perSpan = total / spanCount
        val remainder = total % spanCount
        var additional = 0
        for (i in 0 until spanCount) {
            var size = perSpan
            additional += remainder
            if (additional > 0 && spanCount - additional < remainder) {
                size += 1
                additional -= spanCount
            }
            widths[i] = size
        }
        return widths
    }
}