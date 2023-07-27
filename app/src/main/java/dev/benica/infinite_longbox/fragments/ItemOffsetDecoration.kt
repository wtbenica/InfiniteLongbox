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
        val leftDivisor = if (childAdapterPosition % mNumCols == 0) 1 else 2
        val rightDivisor = if (childAdapterPosition % mNumCols == mNumCols - 1) 1 else 2

        outRect.top = mItemOffset / topDivisor
        outRect.bottom = mItemOffset / bottomDivisor
        outRect.left = (mItemOffsetHorizontal ?: mItemOffset) / leftDivisor
        outRect.right = (mItemOffsetHorizontal ?: mItemOffset) / rightDivisor
    }
}