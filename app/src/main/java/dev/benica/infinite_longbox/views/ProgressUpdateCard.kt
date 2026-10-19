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
import android.widget.TextView
import androidx.cardview.widget.CardView
import dev.benica.infinite_longbox.R

/**
 * One line telling what the database update is doing: "Updating database…" on the left, and on
 * the right which part (publishers, series, creators, characters) is being fetched now.
 */
class ProgressUpdateCard(context: Context, attributeSet: AttributeSet) :
    CardView(context, attributeSet) {

    private val title: TextView
    private val stage: TextView

    init {
        val view = inflate(context, R.layout.fragment_static_update, this)
        (view as CardView).cardElevation = 0f
        title = view.findViewById(R.id.update_title)
        stage = view.findViewById(R.id.update_stage)
    }

    val publisherWrapper = ProgressWrapper("Publishers")
    val seriesWrapper = ProgressWrapper("Series")
    val creatorWrapper = ProgressWrapper("Creators")
    val characterWrapper = ProgressWrapper("Characters")

    fun setTitle(text: String) {
        title.post { title.text = "$text…" }
    }

    /** The starter-database download is the publishers' turn. */
    fun setSeedProgress(done: Long, total: Long) = publisherWrapper.start()

    private fun showStage(name: String) {
        stage.post {
            if (stage.text.toString() == name) return@post
            stage.animate().cancel()
            if (stage.text.isEmpty()) {
                stage.text = name
                stage.animate().alpha(1f).setDuration(FADE_MS).start()
            } else {
                // Fade the old part out, then the new one in
                stage.animate().alpha(0f).setDuration(FADE_MS).withEndAction {
                    stage.text = name
                    stage.animate().alpha(1f).setDuration(FADE_MS).start()
                }.start()
            }
        }
    }

    /**
     * One part of the update. May be called from any thread. The updates arrive a page at a time
     * and finish too quickly to draw a bar for, so a part only announces that it is under way.
     */
    inner class ProgressWrapper(private val name: String) {
        /** This part is the one being fetched now. */
        fun start() = showStage(name)

        fun setMax(max: Int) = Unit

        fun setProgress(progress: Int) = Unit

        fun incrementProgress() = showStage(name)

        fun isComplete(bool: Boolean) = Unit
    }

    private companion object {
        const val FADE_MS = 180L
    }
}
