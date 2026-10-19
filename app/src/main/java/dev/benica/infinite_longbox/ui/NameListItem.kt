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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import dev.benica.infinite_longbox.R

/** A character or creator card: a name and a few details, none of which has a cover of its own. */
data class NameCardState(
    val name: String,
    val details: List<String>,
)

/** Same card as a series, with a blank cover for decoration behind the name tab. */
@Composable
fun NameListItem(
    state: NameCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** When given, an info button opens the item's page; a tap on the card still filters. */
    onInfo: (() -> Unit)? = null,
    infoDescription: String = stringResource(R.string.character_info_description),
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.margin_narrow)),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_list_item_series)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.margin_narrow)),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.surface_raised)),
        border = BorderStroke(4.dp, colorResource(R.color.card_border)),
    ) {
        Box {
            PlaceholderCover(key = state.name, modifier = Modifier.matchParentSize(), scrim = false)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NAME_COVER_WINDOW + 2 * dimensionResource(R.dimen.margin_wide))
                    .scrim()
            )

            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(NAME_COVER_WINDOW))

                FolderTabPanel(
                    fill = colorResource(R.color.card_tab),
                    stroke = colorResource(R.color.card_border),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = dimensionResource(R.dimen.margin_wide),
                                    end = dimensionResource(R.dimen.margin_wide),
                                    top = dimensionResource(R.dimen.margin_default),
                                    bottom = dimensionResource(R.dimen.margin_default),
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val shown = state.details.filter { it.isNotBlank() }

//                            if (shown.isEmpty()) {
//                                Spacer(Modifier.height(20.dp))
//                            }
//
                            // Takes what the info button leaves, so long text can't push it off the card
                            Row(Modifier.weight(1f)) {
                                shown.forEachIndexed { i, detail ->
                                    if (i > 0) {
                                        Spacer(Modifier.width(dimensionResource(R.dimen.margin_default)))

                                        Text("|")

                                        Spacer(Modifier.width(dimensionResource(R.dimen.margin_default)))
                                    }

                                    Subtitle(detail, Modifier.weight(1f, fill = false))
                                }
                            }

                            onInfo?.let { info ->
                                IconButton(
                                    onClick = info,
                                    modifier = Modifier.size(NAME_COVER_WINDOW)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_info_outline),
                                        contentDescription = infoDescription,
                                        tint = colorResource(R.color.card_border),
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

/** Just a band of color above the tab: the blank cover is only decoration here. */
private val NAME_COVER_WINDOW = 36.dp

@Preview(showBackground = true)
@Composable
private fun NameListItemPreview() {
    InfiniteLongboxTheme {
        NameListItem(
            state = NameCardState("Negative Man", listOf("Larry Trainor", "DC")),
            onClick = {},
            onInfo = {},
        )
    }
}
