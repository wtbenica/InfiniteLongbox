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

package dev.benica.infinite_longbox.ui.series

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.ui.theme.CondensedBlock
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * What the series is: volume, publisher and dates, the series it continues from and as, and its
 * tracking notes and notes (which open and close). A series named here opens as the list's series.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun SeriesInfoBox(series: FullSeries, onSeries: (FullSeries) -> Unit, modifier: Modifier = Modifier) {
    val info = series.series
    Column(
        modifier
            .fillMaxWidth()
            .background(colorResource(R.color.accent_soft))
            .padding(dimensionResource(R.dimen.margin_default)),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Labeled(stringResource(R.string.hint_volume), info.volume.toString())
            Labeled(stringResource(R.string.hint_publisher), series.publisher.publisher)
            Text(info.dateRange, color = MaterialTheme.colorScheme.onSurface)
        }

        series.seriesBondFrom?.originSeries?.let {
            Linked(stringResource(R.string.series_continues_from), FullSeries(series = it), onSeries)
        }
        series.seriesBondTo?.targetSeries?.let {
            Linked(stringResource(R.string.series_continues_as), FullSeries(series = it), onSeries)
        }

        info.description?.takeIf { it.isNotBlank() }?.let {
            Expandable(stringResource(R.string.series_detail_tracking_notes), it)
        }
        info.notes?.takeIf { it.isNotBlank() }?.let {
            Expandable(stringResource(R.string.series_detail_notes), it)
        }
    }
}

@Composable
private fun Labeled(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Label(label)
        Text(value, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        fontFamily = CondensedBlock,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = colorResource(R.color.accent_strong),
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun Linked(label: String, series: FullSeries, onSeries: (FullSeries) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Label(label)
        Text(
            text = series.series.fullDescription,
            modifier = Modifier.clickable { onSeries(series) },
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        )
    }
}

/** A heading that opens and closes the [text] under it. */
@Composable
private fun Expandable(label: String, text: String) {
    var open by rememberSaveable(label) { mutableStateOf(false) }
    Column(Modifier.animateContentSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Label(label)
            Icon(
                painter = painterResource(R.drawable.icon_arrow_down),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(24.dp)
                    .rotate(if (open) 180f else 0f),
                tint = colorResource(R.color.accent_strong),
            )
        }
        AnimatedVisibility(open) {
            Text(text, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
