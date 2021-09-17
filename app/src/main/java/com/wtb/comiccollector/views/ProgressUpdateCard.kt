package com.wtb.comiccollector.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.cardview.widget.CardView
import com.wtb.comiccollector.R

class ProgressUpdateCard(context: Context, attributeSet: AttributeSet) :
    CardView(context, attributeSet) {

    private val publisherStatus: ImageView
    private val publisherProgress: ProgressBar
    private val seriesStatus: ImageView
    private val seriesProgress: ProgressBar
    private val creatorStatus: ImageView
    private val creatorProgress: ProgressBar
    private val characterStatus: ImageView
    private val characterProgress: ProgressBar

    init {
        val view = inflate(context, R.layout.fragment_static_update, this)
        publisherStatus = view.findViewById(R.id.status_publisher)
        publisherProgress = view.findViewById(R.id.progress_bar_publishers)
        seriesStatus = view.findViewById(R.id.status_series)
        seriesProgress = view.findViewById(R.id.progress_bar_series)
        creatorStatus = view.findViewById(R.id.status_creators)
        creatorProgress = view.findViewById(R.id.progress_bar_creators)
        characterStatus = view.findViewById(R.id.status_characters)
        characterProgress = view.findViewById(R.id.progress_bar_characters)
    }

    fun updatePublisherProgress(progressPct: Int) =
        updateProgress(progressPct, publisherStatus, seriesStatus, publisherProgress)
    fun updateSeriesProgress(progressPct: Int) =
        updateProgress(progressPct, seriesStatus, creatorStatus, seriesProgress)
    fun updateCreatorProgress(progressPct: Int) =
        updateProgress(progressPct, creatorStatus, characterStatus, creatorProgress)
    fun updateCharacterProgress(progressPct: Int) =
        updateProgress(progressPct, characterStatus, null, characterProgress)

    private fun updateProgress(
        progressPct: Int,
        currItemStatus: ImageView,
        nextItemStatus: ImageView? = null,
        currItemProgressBar: ProgressBar,
    ) {
        if (progressPct == 100) {
            currItemStatus.setImageResource(R.drawable.status_done)
            nextItemStatus?.setImageResource(R.drawable.status_in_progress)
        }
        currItemProgressBar.progress = progressPct
    }

    fun hide() {
        val shrinkAnimation = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0f)

        val fadeAnimation = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    this@ProgressUpdateCard.visibility = View.GONE
                }
            })
        }

        AnimatorSet().apply {
            play(fadeAnimation).with(shrinkAnimation)
            start()
        }
    }
}