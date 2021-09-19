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

    fun toggleExpand() {
        val currRotation = rotation
        val rotateAnimation =
            ObjectAnimator.ofFloat(this, "rotation", currRotation, currRotation + 180f)
                .apply {
                    interpolator = DecelerateInterpolator()
                }

        AnimatorSet().apply {
            play(rotateAnimation)
            start()
        }


//        this.setImageResource(if (isExpanded) {
//            Log.d(TAG, "The arrow is up, so setting arrow down.")
//            R.drawable.arrow_down_24
//        } else {
//            Log.d(TAG, "The arrow is down, so setting arrow up.")
//            R.drawable.arrow_up_24
//        }
//        )
        isExpanded = !isExpanded
        Log.d(TAG, "The box is expanded: $isExpanded")
    }

    companion object {
        const val TAG = APP + "ImageButton"
    }
}