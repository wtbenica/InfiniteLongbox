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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.benica.infinite_longbox.R
import java.io.File

/** Everything an issue card shows, so the card itself holds no logic. */
data class IssueCardState(
    val issueId: Int,
    val number: String,
    /** Set only for a variant that has its own name */
    val variantName: String?,
    /** The cover file, or null if there isn't one (yet) */
    val cover: File?,
    /** True while the cover is still being looked for; false once it's known to be missing */
    val loadingCover: Boolean,
    val inCollection: Boolean,
    val inWishList: Boolean,
)

@Composable
fun IssueListItem(
    state: IssueCardState,
    onClick: () -> Unit,
    onToggleCollection: () -> Unit,
    onToggleWishList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val boxColor by animateColorAsState(
        targetValue = if (state.inCollection) colorResource(R.color.accent_soft)
        else colorResource(R.color.surface_sunken),
        animationSpec = tween(200),
        label = "labelBox"
    )

    val cardShape = RoundedCornerShape(dimensionResource(R.dimen.radius_list_item_issue))
    val borderColor = MaterialTheme.colorScheme.onBackground

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.margin_narrow)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.surface_raised)),
    ) {
        Box {
            Column {
                // Fixed ratio so every card in a row is the same height whatever each cover's own
                // aspect ratio; filled from the top, cropping the bottom
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(13f / 20f)
                ) {
                    Cover(state, Modifier.fillMaxSize())

                    if (state.loadingCover) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }

                    // Overlaps the cover so variant names don't change the card's height
                    state.variantName?.let {
                        Text(
                            text = it,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                                .padding(
                                    horizontal = dimensionResource(R.dimen.margin_default),
                                    vertical = dimensionResource(R.dimen.margin_narrow),
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = PermanentMarker,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 4.dp,
                    color = MaterialTheme.colorScheme
                        .onBackground
                )

                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)

                        .padding(
                            start = dimensionResource(R.dimen.margin_narrow),
                            bottom = dimensionResource(R.dimen.margin_narrow)
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Keyed by issue, so a recycled holder showing another issue starts at its
                    // resting icon instead of animating from the previous one
                    key(state.issueId) {
                        MorphButton(
                            image = AnimatedImageVector.animatedVectorResource(
                                R.drawable
                                    .issue_added_to_collection
                            ),
                            on = state.inCollection,
                            description = stringResource(
                                if (state.inCollection) R.string.description_remove_from_collection
                                else R.string.description_add_to_collection
                            ),
                            onClick = onToggleCollection,
                        )
                        MorphButton(
                            image = AnimatedImageVector.animatedVectorResource(R.drawable.issue_added_to_wish_list),
                            on = state.inWishList,
                            description = stringResource(
                                if (state.inWishList) R.string.description_remove_from_wish_list
                                else R.string.description_add_to_wish_list
                            ),
                            onClick = onToggleWishList,
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Pushed past the corner so the card clips off its bottom-right quarter
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 12.dp, y = 12.dp)
                    .size(72.dp),
                shape = CircleShape,
                color = boxColor,
                border = BorderStroke(4.dp, borderColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = state.number,
                        // Centered in the part that stays visible
                        modifier = Modifier.offset(x = (-4).dp, y = (-4).dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = PermanentMarker,
                        fontSize = 22.sp,
                        maxLines = 1,
                    )
                }
            }

            // Drawn last so the card's border stays unbroken across the badge
            Box(
                Modifier
                    .matchParentSize()
                    .border(4.dp, borderColor, cardShape)
            )
        }
    }
}

/**
 * The plus that morphs into a check. Flips at once when tapped, without waiting for the database
 * to catch up; the real state then takes over.
 */
@Composable
private fun MorphButton(
    image: AnimatedImageVector,
    on: Boolean,
    description: String,
    onClick: () -> Unit,
) {
    var shown by remember(on) { mutableStateOf(on) }
    val painter = rememberAnimatedVectorPainter(image, atEnd = shown)

    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(role = Role.Button, onClickLabel = description) {
                shown = !shown
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = description,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
        )
    }
}

/**
 * Placeholder art underneath, the cover on top (fading in). Portrait covers are centered and
 * top-aligned; wider-than-tall ones (wraparound, gatefold) are aligned right to show the front.
 */
@Composable
private fun Cover(state: IssueCardState, modifier: Modifier) {
    Image(
        painter = painterResource(R.drawable.placeholder),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopStart,
    )

    val file = state.cover ?: return
    var wide by remember(file) { mutableStateOf(false) }
    // The file name stays the same when a cover is replaced, so key on its modified time
    val request = ImageRequest.Builder(LocalContext.current)
        .data(file)
        .memoryCacheKey("${file.path}@${file.lastModified()}")
        .crossfade(true)
        .build()
    AsyncImage(
        model = request,
        contentDescription = stringResource(R.string.description_issue_cover),
        modifier = modifier,
        contentScale = ContentScale.Crop,
        alignment = if (wide) Alignment.TopEnd else Alignment.TopCenter,
        onSuccess = { success ->
            val image = success.result.image
            wide = image.width.toFloat() / image.height > WIDE_ASPECT_RATIO
        },
    )
}

/** Width/height above which a cover is treated as a wraparound or gatefold spread. */
private const val WIDE_ASPECT_RATIO = 0.95f

@Preview(showBackground = true, widthDp = 200)
@Composable
private fun IssueListItemPreview() {
    InfiniteLongboxTheme {
        IssueListItem(
            state = IssueCardState(
                issueId = 1,
                number = "8",
                variantName = "Ed McGuinness Cover",
                cover = null,
                loadingCover = true,
                inCollection = true,
                inWishList = false,
            ),
            onClick = {},
            onToggleCollection = {},
            onToggleWishList = {},
        )
    }
}
