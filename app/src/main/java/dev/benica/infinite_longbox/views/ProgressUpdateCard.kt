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
import android.os.SystemClock
import android.util.AttributeSet
import android.widget.TextView
import androidx.cardview.widget.CardView
import dev.benica.infinite_longbox.R
import kotlinx.coroutines.delay

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

    /** The starter database being downloaded: how many megabytes of how many. */
    fun setSeedProgress(done: Long, total: Long) {
        if (total <= 0) return
        showStage("seed", "${done / MEGABYTE} of ${total / MEGABYTE} MB")
    }

    /**
     * Says which part is under way: [key] names it, and [text] is what is shown. A new part fades
     * in; the same part with a new count just changes the number.
     */
    private fun showStage(key: String, text: String = key) {
        // Set now, not when the fade starts: [hold] must not take the old part's time for the new one's
        if (key != shownKey) {
            shownKey = key
            visibleSince = Long.MAX_VALUE
        }
        stage.post {
            if (stage.tag == key) {
                stage.text = text
                return@post
            }
            stage.tag = key
            stage.animate().cancel()
            if (stage.text.isEmpty()) {
                stage.text = text
                stage.animate().alpha(1f).setDuration(FADE_MS).withEndAction { visible(key) }.start()
            } else {
                // Fade the old part out, then the new one in
                stage.animate().alpha(0f).setDuration(FADE_MS).withEndAction {
                    stage.text = text
                    stage.animate().alpha(1f).setDuration(FADE_MS).withEndAction { visible(key) }.start()
                }.start()
            }
        }
    }

    /** The part named [key] has finished fading in. */
    private fun visible(key: String) {
        if (key == shownKey) visibleSince = SystemClock.uptimeMillis()
    }

    @Volatile
    private var shownKey: String? = null

    @Volatile
    private var visibleSince = Long.MAX_VALUE

    /**
     * Waits until the part now shown has been fully readable for [DWELL_MS], so a part that had
     * nothing to fetch isn't gone before it can be read. Gives up after a few seconds whatever happens.
     */
    private suspend fun holdStage(key: String) {
        val giveUp = SystemClock.uptimeMillis() + HOLD_LIMIT_MS
        while (shownKey == key && SystemClock.uptimeMillis() < giveUp) {
            val since = visibleSince
            if (since != Long.MAX_VALUE && SystemClock.uptimeMillis() - since >= DWELL_MS) return
            delay(POLL_MS)
        }
    }

    /**
     * One part of the update. May be called from any thread. A part announces that it is under
     * way, and when it has a good many pages to fetch it also says how far along it is; a few
     * pages go too quickly for a count to be worth reading.
     */
    inner class ProgressWrapper(private val name: String) {
        /** This part is the one being fetched now. */
        fun start() = showStage(name)

        /** Keeps this part on show for a moment, if it is the one shown; for a part with nothing to fetch. */
        suspend fun hold() = holdStage(name)

        /** [fetched] of the [toFetch] pages this part has to fetch are in. */
        fun report(fetched: Int, toFetch: Int) {
            if (toFetch > SHOW_COUNT_FROM) showStage(name, "$name · $fetched of $toFetch")
            else showStage(name)
        }
    }

    private companion object {
        const val FADE_MS = 180L
        const val MEGABYTE = 1024L * 1024L

        /** A part with nothing to fetch stays readable this long (after its name has faded in). */
        const val DWELL_MS = 250L
        const val HOLD_LIMIT_MS = 3000L
        const val POLL_MS = 25L

        /** Parts with more pages to fetch than this show a count. */
        const val SHOW_COUNT_FROM = 5
    }
}
