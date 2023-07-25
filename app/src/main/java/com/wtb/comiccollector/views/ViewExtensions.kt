package com.wtb.comiccollector.views

import android.animation.*
import android.view.View
import android.view.View.GONE
import android.view.View.MeasureSpec.*
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.DecelerateInterpolator
import androidx.core.view.marginTop
import androidx.core.view.updateMargins
import com.wtb.comiccollector.MainActivity
import com.wtb.comiccollector.R
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val DURATION = 200L

internal fun View.hide() {
    val shrinkYAnimation = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0f)
    val shrinkXAnimation = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0f)
    val fadeoutAnimation = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f)

    AnimatorSet().apply {
        duration = DURATION
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator, isReverse: Boolean) {
                animateHeight(height, 0, DURATION)
                animateMargins(this@hide.marginTop, 0, DURATION)
            }

            override fun onAnimationEnd(animation: Animator) {
                this@hide.visibility = GONE
            }
        })
        play(fadeoutAnimation).with(shrinkYAnimation).with(shrinkXAnimation).apply {
            interpolator = DecelerateInterpolator()
        }
        start()
    }
}

@ExperimentalCoroutinesApi
internal fun View.show() {
    this@show.visibility = View.VISIBLE
    val tt = measuredHeight
    scaleY = 1f
    val lp = layoutParams
    lp.height = WRAP_CONTENT
    layoutParams = lp
    measure(
        makeMeasureSpec((context as MainActivity).screenSize.x, AT_MOST), UNSPECIFIED
    )
    val hh = measuredHeight

    this@show.scaleY = 0f
    val lp2 = layoutParams
    lp2.height = 0
    layoutParams = lp2

    val growYAnimation = ObjectAnimator.ofFloat(this, "scaleY", 0f, 1f)
    val growXAnimation = ObjectAnimator.ofFloat(this, "scaleX", 0f, 1f)
    val fadeAnimation = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f)

    AnimatorSet().apply {
        duration = DURATION
        interpolator = DecelerateInterpolator()
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                animateMargins(0, MARGIN_DEFAULT, DURATION)
                animateHeight(0, hh, DURATION)
            }
        })
        play(fadeAnimation).with(growYAnimation).with(growXAnimation)
        start()
    }
}
//internal fun View.hide() {
//    val shrinkYAnimation = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0f)
//    val shrinkXAnimation = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0f)
//    val fadeoutAnimation = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
//        addListener(object : AnimatorListenerAdapter() {
//            override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
//                val duration = 100L
//                animateHeight(height, 0, duration)
//                animateWidth(width, 0, duration)
//                animateMargins(this@hide.marginTop, 0, duration)
//            }
//        })
//    }
//
//    AnimatorSet().apply {
//        duration = 500L
//        play(fadeoutAnimation).with(shrinkYAnimation).with(shrinkXAnimation).apply {
//            interpolator = DecelerateInterpolator()
//        }
//        start()
//    }
//}
//
//internal fun View.show() {
//    val growYAnimation = ObjectAnimator.ofFloat(this, "scaleY", 0f, 1f)
//    val growXAnimation = ObjectAnimator.ofFloat(this, "scaleX", 0f, 1f)
//    val fadeAnimation = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f)
//
//    AnimatorSet().apply {
//        play(fadeAnimation).with(growYAnimation).with(growXAnimation).apply {
//            interpolator = DecelerateInterpolator()
//            addListener(object : AnimatorListenerAdapter() {
//                override fun onAnimationStart(animation: Animator?) {
//                    this@show.visibility = View.VISIBLE
//                    val duration = 100L
//                    animateMargins(0, MARGIN_DEFAULT, duration)
//                    animateHeight(0, measuredHeight, duration)
//                    animateWidth(0, measuredWidth, duration)
//                }
//            })
//        }
//        start()
//    }
//}

val View.MARGIN_DEFAULT
    get() = resources.getDimension(R.dimen.margin_default).toInt()


private fun View.animateMargins(startValue: Int, endValue: Int, duration: Long) {
    val layoutMarginAnimator = ValueAnimator.ofInt(startValue, endValue)
    layoutMarginAnimator.addUpdateListener {
        val value: Int = it.animatedValue as Int
        val layoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.updateMargins(top = value, bottom = value)
    }

    layoutMarginAnimator.duration = duration
    layoutMarginAnimator.interpolator = DecelerateInterpolator()
    layoutMarginAnimator.start()
}

private fun View.animateWidth(startValue: Int, endValue: Int, duration: Long) {
    val layoutWidthAnimator = ValueAnimator.ofInt(startValue, endValue)

    layoutWidthAnimator.addUpdateListener {
        val value: Int = it.animatedValue as Int
        val layoutParams = this.layoutParams
        layoutParams.width = value
        this.layoutParams = layoutParams
    }

    layoutWidthAnimator.duration = duration
    layoutWidthAnimator.interpolator = DecelerateInterpolator()
    layoutWidthAnimator.start()
}

private fun View.animateHeight(startValue: Int, endValue: Int, duration: Long) {
    val layoutHeightAnimator = ValueAnimator.ofInt(startValue, endValue)
    layoutHeightAnimator.addUpdateListener {
        val value: Int = it.animatedValue as Int
        val layoutParams = this.layoutParams
        layoutParams.height = value
        this.layoutParams = layoutParams
    }

    if (endValue == 0) {
        layoutHeightAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                this@animateHeight.visibility = GONE
            }
        })
    }

    layoutHeightAnimator.duration = duration
    layoutHeightAnimator.interpolator = DecelerateInterpolator()
    layoutHeightAnimator.start()
}

@ExperimentalCoroutinesApi
internal fun View.toggleVisibility(): Boolean {
    val isExpanded = this.visibility == View.VISIBLE
    if (isExpanded) {
        this.hide()
    } else {
        this.show()
    }
    return !isExpanded
}

