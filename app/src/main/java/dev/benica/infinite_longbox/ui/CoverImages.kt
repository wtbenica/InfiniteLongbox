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

package dev.benica.infinite_longbox.ui

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Dimension
import coil3.size.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * The image requests of the list cards, built in one place so a cover loaded ahead of the scroll
 * ([prefetch]) is the very entry the card asks for: same key, same size.
 */
object CoverImages {
    /** How many cards past (and before) the one being bound have their covers loaded ahead. */
    const val PREFETCH_AHEAD = 10
    const val PREFETCH_BEHIND = 4

    private val scope = CoroutineScope(Dispatchers.Default)

    /** Series cards run the width of the screen, up to a size past which more pixels show nothing. */
    private fun seriesWidth(context: Context) =
        context.resources.displayMetrics.widthPixels.coerceAtMost(1080)

    private fun issueWidth(context: Context) =
        (context.resources.displayMetrics.widthPixels / 2).coerceAtMost(540)

    fun seriesRequest(context: Context, file: File): ImageRequest = request(context, file, seriesWidth(context))

    fun issueRequest(context: Context, file: File): ImageRequest = request(context, file, issueWidth(context))

    private fun request(context: Context, file: File, width: Int): ImageRequest =
        ImageRequest.Builder(context)
            .data(file)
            // The file name stays the same when a cover is replaced, so key on its modified time
            .memoryCacheKey("${file.path}@${file.lastModified()}@$width")
            .size(Size(width, Dimension.Undefined))
            .crossfade(true)
            .build()

    /** Loads a series card's cover (and works out where its crop goes) before the card is shown. */
    fun prefetchSeries(context: Context, file: File) {
        if (!file.exists()) return
        SingletonImageLoader.get(context).enqueue(seriesRequest(context, file))
        scope.launch { CoverFocus.alignmentFor(file) }
    }

    fun prefetchIssue(context: Context, file: File) {
        if (!file.exists()) return
        SingletonImageLoader.get(context).enqueue(issueRequest(context, file))
    }
}
