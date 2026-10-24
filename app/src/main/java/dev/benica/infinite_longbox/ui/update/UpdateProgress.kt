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

package dev.benica.infinite_longbox.ui.update

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.benica.infinite_longbox.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** What the database update is doing: its title and the part (publishers, series, creators, characters) being fetched. */
class UpdateProgress(context: Context) {

    var titleText by mutableStateOf(context.getString(R.string.update_title_default))
        private set
    var stage by mutableStateOf<UpdateStage?>(null)
        private set

    /** Whether the line is on screen: while an update runs. */
    var visible by mutableStateOf(false)
        private set

    fun show() {
        visible = true
    }

    fun hide() {
        visible = false
    }

    val publisherWrapper = ProgressWrapper("Publishers")
    val seriesWrapper = ProgressWrapper("Series")
    val creatorWrapper = ProgressWrapper("Creators")
    val characterWrapper = ProgressWrapper("Characters")

    fun setTitle(text: String) {
        titleText = "$text…"
    }

    /** The starter database being downloaded: how many megabytes of how many. */
    fun setSeedProgress(done: Long, total: Long) {
        if (total <= 0) return
        showStage("seed", "${done / MEGABYTE} of ${total / MEGABYTE} MB")
    }

    /** Says which part is under way; a new part fades in, the same part just changes its count. */
    private fun showStage(key: String, text: String = key) {
        // The readable time is worked out here, not from the animation, which doesn't run when the line is off screen
        if (key != shownKey) {
            readableAt = SystemClock.uptimeMillis() + STAGE_FADE_MS
            shownKey = key
        }
        stage = UpdateStage(key, text)
    }

    @Volatile
    private var shownKey: String? = null

    /** When the part now shown has faded in and can be read. */
    @Volatile
    private var readableAt = 0L

    /** Waits until the part shown has been readable for [DWELL_MS]; not at all if the line is off screen, never for long. */
    private suspend fun holdStage(key: String) {
        val giveUp = SystemClock.uptimeMillis() + HOLD_LIMIT_MS
        while (visible && shownKey == key) {
            val now = SystemClock.uptimeMillis()
            if (now >= readableAt + DWELL_MS || now >= giveUp) return
            delay(POLL_MS.milliseconds)
        }
    }

    /** One part of the update; it announces itself and, with many pages, how far along it is. */
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
        const val MEGABYTE = 1024L * 1024L

        /** A part with nothing to fetch stays readable this long (after its name has faded in). */
        const val DWELL_MS = 250L
        const val HOLD_LIMIT_MS = 3000L
        const val POLL_MS = 25L

        /** Parts with more pages to fetch than this show a count. */
        const val SHOW_COUNT_FROM = 5
    }
}
