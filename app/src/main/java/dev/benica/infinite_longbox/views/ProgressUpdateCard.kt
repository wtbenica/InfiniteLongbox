/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.infinite_longbox.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import dev.benica.infinite_longbox.R

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
        characterProgress = view.findViewById(R.id.progress_bar_characters)
    }

    private val title: TextView = findViewById(R.id.update_title)

    fun setTitle(text: String) {
        title.post { title.text = text }
    }

    /** Shows the starter-database download on the first row. */
    fun setSeedProgress(done: Long, total: Long) {
        publisherWrapper.start()
        publisherWrapper.setMax(100)
        publisherWrapper.setProgress(if (total > 0) (done * 100 / total).toInt() else 0)
    }

    val publisherWrapper = ProgressWrapper(publisherProgress, publisherStatus)
    val seriesWrapper = ProgressWrapper(seriesProgress, seriesStatus)
    val creatorWrapper = ProgressWrapper(creatorProgress, creatorStatus)
    val characterWrapper = ProgressWrapper(characterProgress, characterStatus)

    /**
     * Drives one row of the card: a progress bar plus a status dot that is waiting (gray), in
     * progress, or done. May be called from any thread; updates are posted to the UI thread.
     */
    class ProgressWrapper(
        private val bar: ProgressBar,
        private val status: ImageView,
    ) {
        private var started = false

        private fun updateStatus() {
            status.setImageResource(
                when {
                    bar.max > 0 && bar.progress >= bar.max -> R.drawable.status_done
                    started -> R.drawable.status_in_progress
                    else -> R.drawable.status_waiting
                }
            )
        }

        /** Marks this row as the one currently downloading. */
        fun start() {
            bar.post {
                started = true
                updateStatus()
            }
        }

        fun setMax(max: Int) {
            bar.post {
                bar.max = max
                updateStatus()
            }
        }

        fun setProgress(progress: Int) {
            bar.post {
                bar.progress = progress
                updateStatus()
            }
        }

        fun incrementProgress() {
            bar.post {
                bar.setProgress(bar.progress + 1, true)
                updateStatus()
            }
        }

        fun isComplete(bool: Boolean) {
            bar.post {
                bar.progress = if (bool) bar.max else 0
                updateStatus()
            }
        }
    }
}
