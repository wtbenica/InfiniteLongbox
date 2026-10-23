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

package dev.benica.infinite_longbox.repository

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Counts the requests the app is waiting for, so the screen can say it is still fetching: a creator
 * picked whose credits have not all arrived, covers on their way. Every fetch from the server
 * goes through [track].
 */
object LoadTracker {
    private val active = MutableStateFlow(0)

    /** How many requests are in flight. */
    val activeCount: StateFlow<Int> get() = active

    suspend fun <T> track(block: suspend () -> T): T {
        active.update { it + 1 }
        try {
            return block()
        } finally {
            active.update { it - 1 }
        }
    }

    /**
     * Whether to show that the app is fetching. It waits [showAfterMs] before saying yes, so
     * requests that finish at once show nothing, and [hideAfterMs] before saying no, so a run of
     * requests with gaps between them does not flicker.
     */
    @OptIn(FlowPreview::class)
    fun busy(showAfterMs: Long = 300, hideAfterMs: Long = 600): Flow<Boolean> =
        active.map { it > 0 }
            .distinctUntilChanged()
            .debounce { if (it) showAfterMs else hideAfterMs }
            .distinctUntilChanged()
}
