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

/**
 * The paged sync used to throw away everything it had once a week and fetch it all again. The
 * server now says what version each page is (a short fingerprint), and the phone remembers the
 * version of each page it has: a week later it fetches only the pages whose version changed.
 */
object PageDiff {
    /**
     * Which pages are still current: a string with a 't' for each page of [manifest] the phone has
     * at that version and an 'f' for each it does not have or has an older version of.
     *
     * @param savedFlags the phone's record of the pages it has, 't' or 'f' for each
     * @param savedHashes the version of each page it has (blank if it does not know)
     * @param manifest the server's current version of each page
     */
    fun stalePages(savedFlags: String, savedHashes: List<String>, manifest: List<String>): String =
        manifest.indices.joinToString("") { page ->
            val has = savedFlags.getOrNull(page) == 't'
            val same = savedHashes.getOrNull(page)?.let { it.isNotEmpty() && it == manifest[page] } == true
            if (has && same) "t" else "f"
        }

    /**
     * The versions to remember after a sync of [pages] pages: what was remembered, with each page
     * that was fetched (a key of [fetched]) at the version it had in the manifest.
     */
    fun remember(saved: List<String>, pages: Int, fetched: Map<Int, String>): List<String> =
        List(pages) { page -> fetched[page] ?: saved.getOrNull(page).orEmpty() }

    fun parse(stored: String?): List<String> =
        if (stored.isNullOrEmpty()) emptyList() else stored.split(',')

    fun format(hashes: List<String>): String = hashes.joinToString(",")
}
