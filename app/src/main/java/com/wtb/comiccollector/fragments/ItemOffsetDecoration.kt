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

        outRect.top = mItemOffset
        outRect.bottom = mItemOffset
        outRect.left = mItemOffsetHorizontal ?: mItemOffset
        outRect.right = mItemOffsetHorizontal ?: mItemOffset
    }
}