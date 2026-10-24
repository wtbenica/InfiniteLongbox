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

/** The paged sync fetches only the pages whose version (a short fingerprint) changed since the last sync. */
object PageDiff {
    /** Which pages are still current: 't' for each manifest page the phone has at that version, else 'f'. */
    fun stalePages(savedFlags: String, savedHashes: List<String>, manifest: List<String>): String =
        manifest.indices.joinToString("") { page ->
            val has = savedFlags.getOrNull(page) == 't'
            val same = savedHashes.getOrNull(page)?.let { it.isNotEmpty() && it == manifest[page] } == true
            if (has && same) "t" else "f"
        }

    /** The versions to remember after a sync: what was remembered, plus the fetched pages at their manifest version. */
    fun remember(saved: List<String>, pages: Int, fetched: Map<Int, String>): List<String> =
        List(pages) { page -> fetched[page] ?: saved.getOrNull(page).orEmpty() }

    fun parse(stored: String?): List<String> =
        if (stored.isNullOrEmpty()) emptyList() else stored.split(',')

    fun format(hashes: List<String>): String = hashes.joinToString(",")
}
