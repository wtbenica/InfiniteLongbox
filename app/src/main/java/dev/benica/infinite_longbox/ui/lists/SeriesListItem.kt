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

package dev.benica.infinite_longbox.ui.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import coil3.compose.AsyncImage
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.common.Bar
import dev.benica.infinite_longbox.ui.common.FolderTabPanel
import dev.benica.infinite_longbox.ui.common.Subtitle
import dev.benica.infinite_longbox.ui.common.listCardFrame
import dev.benica.infinite_longbox.ui.covers.CoverFocus
import dev.benica.infinite_longbox.ui.covers.CoverImages
import dev.benica.infinite_longbox.ui.covers.PlaceholderCover
import dev.benica.infinite_longbox.ui.covers.TitlePlaceholder
import dev.benica.infinite_longbox.ui.covers.placeholderKey
import dev.benica.infinite_longbox.ui.covers.scrim
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import dev.benica.infinite_longbox.ui.theme.PermanentMarker
import java.io.File

/** Everything a series card shows, so the card itself holds no logic. */
data class SeriesCardState(
    val name: String,
    val publisher: String,
    val dateRange: String,
    val format: String,
    val cover: File?,
    /** When the cover file last changed, so a replaced one (same name) redraws */
    val coverStamp: Long = cover?.lastModified() ?: 0L,
    val loadingCover: Boolean,
)

@Composable
fun SeriesListItem(state: SeriesCardState, onClick: () -> Unit, modifier: Modifier = Modifier) =
    SeriesCard(state, onClick, modifier)

/** The card laid out like a real one, with bars where the text goes, for a series still loading. */
@Composable
fun SeriesListItemSkeleton(modifier: Modifier = Modifier, placeholderKey: String = "") =
    SeriesCard(null, {}, modifier, placeholderKey)

@Composable
private fun SeriesCard(
    state: SeriesCardState?,
    onClick: () -> Unit,
    modifier: Modifier,
    skeletonKey: String = ""
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.radius_list_item_series))

    Card(
        onClick = onClick,
        modifier = modifier.listCardFrame(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.surface_raised)),
        border = BorderStroke(4.dp, colorResource(R.color.card_border)),
    ) {
        Box {
            if (state == null) {
                // The scrim below washes it, as it does a real placeholder
                PlaceholderCover(skeletonKey, Modifier.matchParentSize(), scrim = false)
            } else {
                Cover(state, Modifier.matchParentSize())
            }

            // On every series card, cover or not
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(COVER_WINDOW + 2 * dimensionResource(R.dimen.margin_wide))
                    .scrim()
            )

            if (state != null && state.cover == null) {
                TitlePlaceholder(state.name, Modifier.matchParentSize(), showArt = false)
            }

            // Title block over a scrim; its height sets the card's height
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(COVER_WINDOW))

                    FolderTabPanel(
                        fill = colorResource(R.color.card_tab),
                        stroke = colorResource(R.color.card_border),
                        strokeWidth = 4.dp,
                        cornerRadius = dimensionResource(R.dimen.margin_wide),
                        tab = {
                            if (state == null) {
                                Bar(
                                    Modifier
                                        .padding(
                                            horizontal = dimensionResource(R.dimen.margin_wide),
                                            vertical = dimensionResource(R.dimen.margin_default)
                                        )
                                        .width(140.dp)
                                        .height(20.dp)
                                )
                            } else {
                                Text(
                                    text = state.name,
                                    modifier = Modifier.padding(
                                        start = dimensionResource(R.dimen.margin_wide),
                                        end = dimensionResource(R.dimen.margin_wide),
                                        top = dimensionResource(R.dimen.margin_default),
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = PermanentMarker,
                                    fontSize = 20.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        body = {
                            Row(
                                modifier = Modifier.padding(
                                    start = dimensionResource(R.dimen.margin_wide),
                                    end = dimensionResource(R.dimen.margin_wide),
                                    top = dimensionResource(R.dimen.margin_default),
                                    bottom = dimensionResource(R.dimen.margin_default),
                                )
                            ) {
                                val gap = dimensionResource(R.dimen.margin_default)
                                if (state == null) {
                                    Bar(
                                        Modifier
                                            .width(200.dp)
                                            .height(16.dp)
                                    )
                                    return@Row
                                }

                                Subtitle(state.dateRange)

                                if (state.publisher != "") {
                                    Spacer(Modifier.width(gap))

                                    Text("|")

                                    Spacer(Modifier.width(gap))
                                }

                                Subtitle(state.publisher)

                                if (state.format != "") {
                                    Spacer(Modifier.width(gap))

                                    Text("|")

                                    Spacer(Modifier.width(gap))
                                }


                                Subtitle(state.format)
                            }
                        }
                    )
                }
            }

            if (state != null && state.loadingCover) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

/** The part of the card above the title tab, where the cover (or its stand-in) shows. */
internal val COVER_WINDOW = 150.dp

/** Fills the card, scaled to cover; which part shows is picked by [CoverFocus]. */
@Composable
private fun Cover(state: SeriesCardState, modifier: Modifier) {
    val file = state.cover
    if (file == null) {
        // The card's wash goes between the art and the title, so the title isn't washed
        TitlePlaceholder(state.name, modifier, showTitle = false)
    } else {
        val context = LocalContext.current
        val request = remember(file, file.lastModified()) { CoverImages.seriesRequest(context, file) }
        val alignment by produceState(CoverFocus.peek(file) ?: Alignment.TopStart, file) {
            value = CoverFocus.alignmentFor(file)
        }
        AsyncImage(
            model = request,
            contentDescription = stringResourceCoverDescription(),
            modifier = modifier,
            contentScale = ContentScale.Crop,
            alignment = alignment,
        )
    }
}

@Composable
private fun stringResourceCoverDescription(): String =
    androidx.compose.ui.res.stringResource(R.string.description_series_list_item_background_image)

@Preview(showBackground = true)
@Composable
private fun SeriesListItemPreview() {
    InfiniteLongboxTheme {
        SeriesListItem(
            state = SeriesCardState(
                name = "Doom Patrol",
                publisher = "DC Comics",
                dateRange = "1990-2005",
                format = "ongoing series",
                cover = null,
                loadingCover = true,
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SeriesListItemSkeletonPreview() {
    InfiniteLongboxTheme { SeriesListItemSkeleton(placeholderKey = "series|3") }
}
