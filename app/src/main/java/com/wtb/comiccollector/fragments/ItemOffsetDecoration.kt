package com.wtb.comiccollector.fragments

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ItemOffsetDecoration(itemOffset: Int, private val isHorizontal: Boolean = false) : RecyclerView
.ItemDecoration() {
    private var mItemOffset = itemOffset
    private var spanCount = 2

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val ratio = if (isHorizontal) 2 else 1

        outRect.top = mItemOffset / ratio
        outRect.bottom = mItemOffset / ratio
        outRect.left = mItemOffset
        outRect.right = mItemOffset
    }
}