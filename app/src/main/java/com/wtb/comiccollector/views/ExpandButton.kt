package com.wtb.comiccollector.views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.animation.DecelerateInterpolator
import com.wtb.comiccollector.APP

class ExpandButton(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageButton(context, attributeSet) {

    private var isExpanded = false

    fun initExpanded(isExpanded: Boolean) {
        this.isExpanded = isExpanded
        rotation = if (isExpanded) {
            180f
        } else {
            0f
        }
    }

    fun toggleExpand() {
        val currRotation = rotation
        val destRotation = if (isExpanded) 0f else 180f
        val rotateAnimation =
            ObjectAnimator.ofFloat(this, "rotation", currRotation, destRotation)
                .apply {
                    interpolator = DecelerateInterpolator()
                }

        AnimatorSet().apply {
            play(rotateAnimation)
            start()
        }

        isExpanded = !isExpanded
        Log.d(TAG, "The box is expanded: $isExpanded")
    }

    companion object {
        const val TAG = APP + "ImageButton"
    }
}