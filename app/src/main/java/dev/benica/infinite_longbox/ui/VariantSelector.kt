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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.Issue
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class VariantSelectorState(
    val variants: List<Issue> = emptyList(),
    val selectedId: Int = 0,
)

/** A titled drop-down of the issue's variants; shown only when there's more than one. */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun VariantSelector(
    state: VariantSelectorState,
    onSelect: (Issue) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.variants.size <= 1) return

    val narrow = dimensionResource(R.dimen.margin_narrow)
    val shape = RoundedCornerShape(dimensionResource(R.dimen.margin_default))
    // Switches at once on a pick, ahead of the data catching up
    var selectedId by remember(state.selectedId) { mutableIntStateOf(state.selectedId) }
    var open by remember { mutableStateOf(false) }
    val selected = state.variants.firstOrNull { it.issueId == selectedId }

    Column(modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.label_variants).uppercase(),
            modifier = Modifier
                .padding(horizontal = narrow)
                .fillMaxWidth()
                .background(colorResource(R.color.accent_soft), shape)
                .padding(narrow),
            color = colorResource(R.color.accent_strong),
            fontFamily = CondensedBlock,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )

        Box(
            Modifier
                .padding(
                    horizontal = dimensionResource(R.dimen.margin_default),
                    vertical = narrow,
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(R.color.surface_sunken), shape)
                    .clickable { open = true }
                    .padding(dimensionResource(R.dimen.margin_default)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selected?.label().orEmpty(),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = CondensedBlock,
                    fontSize = 16.sp,
                )
                Image(
                    painter = painterResource(R.drawable.icon_arrow_down),
                    contentDescription = stringResource(R.string.description_select_variant),
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(colorResource(R.color.text_secondary)),
                )
            }

            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                state.variants.forEach { variant ->
                    DropdownMenuItem(
                        text = { Text(variant.label(), fontFamily = CondensedBlock) },
                        onClick = {
                            open = false
                            selectedId = variant.issueId
                            onSelect(variant)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Issue.label(): String =
    if (variantOf == null) "$issueNumRaw – ${stringResource(R.string.scan_standard_edition)}"
    else "$issueNumRaw – $variantName"

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun VariantSelectorPreview() {
    val regular = Issue(issueId = 1, issueNumRaw = "34")
    val variant =
        Issue(issueId = 2, issueNumRaw = "34", variantName = "Mitch Gerads Variant", variantOf = 1)
    InfiniteLongboxTheme {
        VariantSelector(
            VariantSelectorState(listOf(regular, variant), selectedId = 2),
            onSelect = {})
    }
}
