package com.wtb.comiccollector.fragments

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