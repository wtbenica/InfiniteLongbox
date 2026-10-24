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

package dev.benica.infinite_longbox.ui.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme

/** The collapsed filter sheet: an arrow, and a line saying what the list is filtered by. */
@Composable
fun FilterSheetHeader(summary: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.bottom_sheet_handle_description)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.peek_height))
            .clickable(onClickLabel = description, role = Role.Button, onClick = onClick)
            .padding(horizontal = dimensionResource(R.dimen.margin_wide)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(dimensionResource(R.dimen.peek_height)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.icon_arrow_up),
                contentDescription = description,
                modifier = Modifier.size(32.dp),
                tint = colorResource(R.color.accent_strong),
            )
        }
        if (summary.isNotEmpty()) {
            Text(
                text = summary,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = dimensionResource(R.dimen.margin_wide)),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_10_pro", showSystemUi = true)
@Composable
private fun FilterSheetHeaderPreview() {
    InfiniteLongboxTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Bottom,
            ) {
                FilterSheetHeader(
                    summary = "My Collection · Doom Patrol · Negative Man · Rachel Pollack",
                    onClick = {},
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_10_pro", showSystemUi = true)
@Composable
private fun FilterSheetHeaderEmptyPreview() {
    InfiniteLongboxTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Bottom,
            ) { FilterSheetHeader("", onClick = {}) }
        }
    }
}
