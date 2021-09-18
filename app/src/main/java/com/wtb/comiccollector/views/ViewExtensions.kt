package com.wtb.comiccollector.views

import android.animation.*
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.DecelerateInterpolator
import androidx.core.view.marginTop
import androidx.core.view.updateMargins
import com.wtb.comiccollector.R

internal fun View.hide() {
    this.measure(WRAP_CONTENT, MATCH_PARENT)

    val shrinkYAnimation = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0f)
    val shrinkXAnimation = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0f)
    val fadeoutAnimation = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                val duration = 100L
                animateHeight(measuredHeight, 0, duration)
                animateWidth(measuredWidth, 0, duration)
                animateMargins(this@hide.marginTop, 0, duration)
            }
        })
    }

    AnimatorSet().apply {
        duration = 500L
        play(fadeoutAnimation).with(shrinkYAnimation).with(shrinkXAnimation).apply {
            interpolator = DecelerateInterpolator()
        }
        start()
    }
}

internal fun View.show() {
    this.measure(WRAP_CONTENT, WRAP_CONTENT)

    val growYAnimation = ObjectAnimator.ofFloat(this, "scaleY", 0f, 1f)
    val growXAnimation = ObjectAnimator.ofFloat(this, "scaleX", 0f, 1f)
    val fadeAnimation = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f)

    AnimatorSet().apply {
        play(fadeAnimation).with(growYAnimation).with(growXAnimation).apply {
            interpolator = DecelerateInterpolator()
            addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    this@show.visibility = View.VISIBLE
                    val duration = 100L
                    animateMargins(0, MARGIN_DEFAULT, duration)
                    animateHeight(0, measuredHeight, duration)
                    animateWidth(0, measuredWidth, duration)
                }
            })
        }
        start()
    }
}

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
        layoutHeightAnimator.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                this@animateHeight.visibility = View.GONE
            }
        })
    }

    layoutHeightAnimator.duration = duration
    layoutHeightAnimator.interpolator = DecelerateInterpolator()
    layoutHeightAnimator.start()
}

internal fun View.toggleVisibility(): Int {
    this.measure(WRAP_CONTENT, WRAP_CONTENT)
    val isExpanded = this.visibility == View.VISIBLE
    if (isExpanded) {
        this.hide()
    } else {
        this.show()
    }
    return this.visibility
}

