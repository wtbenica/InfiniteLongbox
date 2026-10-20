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

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R

data class IssueActionState(
    val issueId: Int = 0,
    val inCollection: Boolean = false,
    val inWishList: Boolean = false,
)

/** Have / want / find, each an icon over a small label. */
@Composable
fun IssueActionBar(
    state: IssueActionState,
    onToggleCollection: () -> Unit,
    onToggleWishList: () -> Unit,
    onSearchEbay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = colorResource(R.color.text_secondary)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.margin_wide)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        // Keyed by issue so switching issues or variants doesn't animate from the last one
        key(state.issueId) {
            Action(R.string.label_have) {
                MorphButton(
                    image = AnimatedImageVector.animatedVectorResource(R.drawable.issue_added_to_collection),
                    on = state.inCollection,
                    description = stringResource(
                        if (state.inCollection) R.string.description_remove_from_collection
                        else R.string.description_add_to_collection
                    ),
                    onClick = onToggleCollection,
                    tint = tint,
                )
            }
            Action(R.string.label_want) {
                MorphButton(
                    image = AnimatedImageVector.animatedVectorResource(R.drawable.issue_added_to_wish_list),
                    on = state.inWishList,
                    description = stringResource(
                        if (state.inWishList) R.string.description_remove_from_wish_list
                        else R.string.description_add_to_wish_list
                    ),
                    onClick = onToggleWishList,
                    tint = tint,
                )
            }
        }
        Action(R.string.label_find) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(role = Role.Button, onClick = onSearchEbay),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_search_24),
                    contentDescription = stringResource(R.string.description_search_ebay),
                    modifier = Modifier.size(40.dp),
                    colorFilter = ColorFilter.tint(tint),
                )
            }
        }
    }
}

@Composable
private fun Action(label: Int, button: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        button()
        androidx.compose.material3.Text(
            text = stringResource(label).uppercase(),
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.margin_default)),
            color = colorResource(R.color.text_secondary),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 220)
@Composable
private fun IssueActionBarPreview() {
    InfiniteLongboxTheme {
        IssueActionBar(
            state = IssueActionState(issueId = 1, inCollection = true),
            onToggleCollection = {}, onToggleWishList = {}, onSearchEbay = {},
        )
    }
}
