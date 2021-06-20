package com.wtb.comiccollector

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

class ScrollingViewWithBottomSheetBehavior(context: Context, attributes: AttributeSet) :
    AppBarLayout.ScrollingViewBehavior(context, attributes) {

    private var bottomMargin = 0

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        return super.layoutDependsOn(parent, child, dependency) || dependency is
                BottomSheetBehavior<*>
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        val result = super.onDependentViewChanged(parent, child, dependency)

        if (dependency is BottomSheetBehavior<*> && dependency.height != bottomMargin) {
            val childLayoutParams = child.layoutParams as CoordinatorLayout.LayoutParams
            childLayoutParams.bottomMargin = bottomMargin
            child.requestLayout()
            return true
        } else {
            return result
        }
    }
}