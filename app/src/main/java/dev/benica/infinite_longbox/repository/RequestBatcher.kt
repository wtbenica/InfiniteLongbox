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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Gathers what is asked for during a short wait and handles it in one go: a screen full of cards
 * binds together, and one request for all their issues costs far less than one for each.
 *
 * An item asked for again while it waits or is being handled is not handled twice.
 *
 * @param waitMs how long after the first item arrives to wait for more
 * @param handle gets the items gathered, once
 */
class RequestBatcher<T : Any>(
    private val scope: CoroutineScope,
    private val waitMs: Long,
    private val handle: suspend (Set<T>) -> Unit,
) {
    private val pending = ConcurrentHashMap.newKeySet<T>()
    private val inFlight = ConcurrentHashMap.newKeySet<T>()
    private var scheduled = false

    fun request(item: T) {
        if (item in inFlight || !pending.add(item)) return
        val schedule = synchronized(this) { (!scheduled).also { if (it) scheduled = true } }
        if (schedule) {
            scope.launch {
                delay(waitMs)
                val batch = synchronized(this@RequestBatcher) {
                    scheduled = false
                    pending.toSet().also { pending.removeAll(it); inFlight.addAll(it) }
                }
                try {
                    handle(batch)
                } finally {
                    inFlight.removeAll(batch)
                }
            }
        }
    }
}
