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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.benica.infinite_longbox.R
import java.io.File

/** Everything a series card shows, so the card itself holds no logic. */
data class SeriesCardState(
    val name: String,
    val dateRange: String,
    val format: String,
    /** The first issue's cover, or null to show the placeholder */
    val cover: File?,
    /** True while the cover is still being looked for */
    val loadingCover: Boolean,
)

@Composable
fun SeriesListItem(state: SeriesCardState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.radius_list_item_series))

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                dimensionResource(R.dimen.margin_narrow)
            ),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.margin_narrow)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.surface_raised)),
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.onBackground),
    ) {
        Box {
            Cover(state, Modifier.matchParentSize())

            // Title block over a scrim; its height sets the card's height
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(Modifier.height(150.dp))

                FolderTabPanel(
                    fill = MaterialTheme.colorScheme.background,
                    stroke = MaterialTheme.colorScheme.onBackground,
                    strokeWidth = 4.dp,
                    cornerRadius = dimensionResource(R.dimen.margin_wide),
                    tab = {
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
                            Subtitle(state.dateRange)
                            Spacer(Modifier.width(dimensionResource(R.dimen.margin_default)))
                            Subtitle(state.format)
                        }
                    }
                )
            }

            if (state.loadingCover) {
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

@Composable
private fun Subtitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = PermanentMarker,
        fontSize = 16.sp,
    )
}

/** Fills the card, scaled to cover and aligned top-left like the old FitTopImageView. */
@Composable
private fun Cover(state: SeriesCardState, modifier: Modifier) {
    val file = state.cover
    if (file == null) {
        Image(
            painter = painterResource(R.drawable.placeholder),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopStart,
        )
    } else {
        // The file name stays the same when a cover is replaced, so key on its modified time
        val request = ImageRequest.Builder(LocalContext.current)
            .data(file)
            .memoryCacheKey("${file.path}@${file.lastModified()}")
            .build()
        AsyncImage(
            model = request,
            contentDescription = stringResourceCoverDescription(),
            modifier = modifier,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopStart,
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
                dateRange = "(1990-2005)",
                format = "ongoing series",
                cover = File("43418.jpg"),
                loadingCover = true,
            ),
            onClick = {}
        )
    }
}
