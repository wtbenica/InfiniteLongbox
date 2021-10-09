package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
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
        (view as CardView).cardElevation = 0f
        publisherStatus = view.findViewById(R.id.status_publisher)
        publisherProgress = view.findViewById<ProgressBar>(R.id.progress_bar_publishers)

        seriesStatus = view.findViewById(R.id.status_series)
        seriesProgress = view.findViewById(R.id.progress_bar_series)

        creatorStatus = view.findViewById(R.id.status_creators)
        creatorProgress = view.findViewById(R.id.progress_bar_creators)

        characterStatus = view.findViewById(R.id.status_characters)
        characterProgress = view.findViewById<ProgressBar>(R.id.progress_bar_characters)
    }

    val publisherWrapper = ProgressWrapper(publisherProgress, publisherStatus, seriesStatus)
    val seriesWrapper = ProgressWrapper(seriesProgress, seriesStatus, creatorStatus)
    val creatorWrapper = ProgressWrapper(creatorProgress, creatorStatus, characterStatus)
    val characterWrapper = ProgressWrapper(characterProgress, characterStatus)

    class ProgressWrapper(
        val bar: ProgressBar, val status: ImageView, val nextStatus: ImageView? = null,
    ) {
        fun setMax(max: Int) {
            bar.max = max
        }

        fun setProgress(progress: Int) {
            bar.progress = progress
        }

        fun incrementProgress() {
            if (bar.progress == bar.max) {
                status.setImageResource(R.drawable.status_done)
                nextStatus?.setImageResource(R.drawable.status_in_progress)
            }

            bar.setProgress(bar.progress + 1, true)
        }

        fun isComplete(bool: Boolean) {
            bar.progress = if (bool) {
                bar.max
            } else {
                0
            }
        }
    }
}