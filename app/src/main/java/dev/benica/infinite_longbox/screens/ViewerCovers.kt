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
package dev.benica.infinite_longbox.screens

import dev.benica.infinite_longbox.ui.covers.ViewerCover

/**
 * The covers the viewer goes through, opening at the one on the page. [stops] are the jump bar's
 * covers, with each one's issue it is a variant of ([variantOf]); [here] is the page's place among
 * them. A page whose cover is no stop (an issue or variant outside the collection) is put in among
 * the stops of its issue (the issue before its variants, a variant after them) and opened first,
 * so the cover that was clicked is the one that shows.
 *
 * @return the covers, and the index to open at
 */
internal fun viewerCovers(
    stops: List<ViewerCover>,
    variantOf: List<Int?>,
    here: Int,
    shown: ViewerCover,
    shownIssue: Int,
): Pair<List<ViewerCover>, Int> {
    if (stops.any { it.issueId == shown.issueId }) {
        return stops.map { if (it.issueId == shown.issueId) shown else it } to here
    }
    val related = stops.indices.filter { stops[it].issueId == shownIssue || variantOf[it] == shownIssue }
    val at = when {
        related.isEmpty() -> here + 1
        shown.issueId == shownIssue -> related.first()
        else -> related.last() + 1
    }
    return stops.toMutableList().apply { add(at, shown) } to at
}
