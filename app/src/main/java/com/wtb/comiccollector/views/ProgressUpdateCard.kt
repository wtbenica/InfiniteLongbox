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
        publisherProgress = view.findViewById(R.id.progress_bar_publishers)

        seriesStatus = view.findViewById(R.id.status_series)
        seriesProgress = view.findViewById(R.id.progress_bar_series)

        creatorStatus = view.findViewById(R.id.status_creators)
        creatorProgress = view.findViewById(R.id.progress_bar_creators)

        characterStatus = view.findViewById(R.id.status_characters)
        characterProgress = view.findViewById<ProgressBar>(R.id.progress_bar_characters)
    }

    fun updatePublisherProgress(progressPct: Int) =
        updateProgress(progressPct,
                       publisherStatus,
                       seriesStatus,
                       publisherProgress)

    fun updateSeriesProgress(progressPct: Int) =
        updateProgress(progressPct, seriesStatus, creatorStatus, seriesProgress)

    fun updateCreatorProgress(progressPct: Int) =
        updateProgress(progressPct,
                       creatorStatus,
                       characterStatus,
                       creatorProgress)

    fun updateCharacterProgress(progressPct: Int) =
        updateProgress(progressPct, characterStatus, null, characterProgress)

    private fun updateProgress(
        progressPct: Int,
        currItemStatus: ImageView,
        nextItemStatus: ImageView? = null,
        currItemProgressBar: ProgressBar
    ) {
        if (progressPct == 100) {
            currItemStatus.setImageResource(R.drawable.status_done)
            nextItemStatus?.setImageResource(R.drawable.status_in_progress)
        }

        currItemProgressBar.progress = progressPct
    }
}