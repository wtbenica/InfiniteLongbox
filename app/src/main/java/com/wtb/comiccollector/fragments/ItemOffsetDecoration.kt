package com.wtb.comiccollector.fragments

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ItemOffsetDecoration(itemOffset: Int, itemOffsetHorizontal: Int? = null) :
    RecyclerView.ItemDecoration() {
    private var mItemOffset = itemOffset
    private var mItemOffsetHorizontal = itemOffsetHorizontal

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val childAdapterPosition = parent.getChildAdapterPosition(view)
        val itemCount = parent.adapter?.itemCount ?: 1

        val topDivisor = if (childAdapterPosition == 0) 1 else 2
        val bottomDivisor = if (childAdapterPosition == itemCount - 1) 1         else 2

        outRect.top = mItemOffset / topDivisor
        outRect.bottom = mItemOffset / bottomDivisor
        outRect.left = mItemOffsetHorizontal ?: mItemOffset
        outRect.right = mItemOffsetHorizontal ?: mItemOffset
    }
}