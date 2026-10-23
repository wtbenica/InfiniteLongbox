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

package dev.benica.infinite_longbox.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.theme.CondensedBlock
@Composable
internal fun LoadFailed(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_default))) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = CondensedBlock,
        )
        OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.character_retry)) }
    }
}

/** A heading in the same chip the issue page uses, over what it heads. */
@Composable
internal fun Section(title: String, content: @Composable () -> Unit) {
    val margin = dimensionResource(R.dimen.margin_narrow)
    Column(verticalArrangement = Arrangement.spacedBy(margin)) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    colorResource(R.color.accent_soft),
                    RoundedCornerShape(dimensionResource(R.dimen.margin_default)),
                )
                .padding(dimensionResource(R.dimen.margin_default)),
            color = colorResource(R.color.accent_strong),
            fontFamily = CondensedBlock,
            fontSize = 14.sp,
        )
        content()
    }
}

@Composable
internal fun Body(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.margin_default)),
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = CondensedBlock,
        fontSize = 15.sp,
    )
}

/** A line with a label to the left and what it says, opening something when [onClick] is given. */
@Composable
internal fun LinkRow(label: String, text: String, onClick: (() -> Unit)?) {
    val pad = dimensionResource(R.dimen.margin_default)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = pad, vertical = pad),
        horizontalArrangement = Arrangement.spacedBy(pad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = CondensedBlock,
            fontSize = 13.sp,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontFamily = CondensedBlock,
            fontWeight = if (onClick != null) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp,
        )
        if (onClick != null) Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
