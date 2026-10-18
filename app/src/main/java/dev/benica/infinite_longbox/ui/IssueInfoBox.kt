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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Release date, cover date and notes; a row is left out when its value is missing. */
data class IssueInfoState(
    val releaseDate: LocalDate? = null,
    val coverDate: LocalDate? = null,
    val notes: String? = null,
)

private val releaseFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val coverFormat = DateTimeFormatter.ofPattern("MMM yyyy")

@Composable
fun IssueInfoBox(state: IssueInfoState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        state.releaseDate?.let {
            InfoRow(stringResource(R.string.label_release_date), it.format(releaseFormat))
        }
        state.coverDate?.let {
            InfoRow(stringResource(R.string.label_cover_date), it.format(coverFormat))
        }
        state.notes?.let { InfoRow(stringResource(R.string.label_notes), it) }
    }
}

@Composable
private fun InfoRow(label: String, text: String) {
    val margin = dimensionResource(R.dimen.margin_narrow)

    Column {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(margin)
                .background(
                    colorResource(R.color.accent_soft),
                    RoundedCornerShape(dimensionResource(R.dimen.margin_default))
                )
                .padding(margin),
            color = colorResource(R.color.accent_strong),
            fontFamily = CondensedBlock,
            fontSize = 14.sp,
        )
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.margin_default)),
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = CondensedBlock,
            fontSize = 14.sp,
        )
    }
}

@Preview(showBackground = true, widthDp = 200)
@Composable
private fun IssueInfoBoxPreview() {
    InfiniteLongboxTheme {
        IssueInfoBox(
            IssueInfoState(
                releaseDate = LocalDate.of(2021, 5, 6),
                coverDate = LocalDate.of(2021, 8, 1),
                notes = $$"Cover price $3.99",
            )
        )
    }
}
