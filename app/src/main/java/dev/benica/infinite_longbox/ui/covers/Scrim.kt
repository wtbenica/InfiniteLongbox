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

package dev.benica.infinite_longbox.ui.covers

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import dev.benica.infinite_longbox.R

/**
 * The one place overlays on covers are defined: a wash (white, or dark in dark mode) that fades
 * the artwork so text and controls read over it.
 * - Every missing-cover placeholder gets it, via [PlaceholderCover].
 * - Every series card gets it behind its title tab, whether it has a cover (see
 *   SeriesListItem), so a series placeholder doesn't use [PlaceholderCover]'s own.
 */
/** Washes whatever this element draws; an empty, sized element becomes a plain band. */
@Composable
fun Modifier.scrim(): Modifier {
    // A whitewash by day; by night it darkens instead, so covers don't glare
    val wash = colorResource(R.color.cover_wash)
    return drawWithContent {
        drawContent()
        drawRect(wash)
    }
}

/**
 * The blank cover that stands in for a missing one, in the color picked for [key] (see
 * [placeholderArtworkFor]). Pass [scrim] = false where the caller already washes the area. [contentDescription] is for
 * placeholders that stand alone (the issue's cover); decorative ones leave it null.
 */
@Composable
fun PlaceholderCover(
    key: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.TopStart,
    scrim: Boolean = true,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(placeholderArtworkFor(key)),
        contentDescription = contentDescription,
        modifier = if (scrim) modifier.scrim() else modifier,
        contentScale = contentScale,
        alignment = alignment,
    )
}
