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

package dev.benica.infinite_longbox

/** How many list rows fall in one [SortType.letterBucketSql] bucket; a database result row. */
data class LetterBucket(val bucket: String, val n: Int)

/** Where each letter starts in a name-sorted list, so a letter strip can jump to it. */
class LetterIndex(buckets: List<LetterBucket>, descending: Boolean) {
    private val counts = buckets.associate { it.bucket to it.n }
    private val starts = HashMap<String, Int>()

    init {
        val order = listOf("#") + ('a'..'z').map { it.toString() } + "~"
        var position = 0
        (if (descending) order.reversed() else order).forEach {
            starts[it] = position
            position += counts[it] ?: 0
        }
    }

    fun has(letter: Char): Boolean = (counts[key(letter)] ?: 0) > 0

    /** The list position of the first name under [letter], or null if there is none. */
    fun positionOf(letter: Char): Int? = if (has(letter)) starts[key(letter)] else null

    private fun key(letter: Char) = if (letter == '#') "#" else letter.lowercaseChar().toString()

    companion object {
        /** The letters a strip shows, in ascending order. */
        val LETTERS: List<Char> = listOf('#') + ('A'..'Z')
    }
}
