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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.theme.CondensedBlock

/** A chip as the sheet draws them: tinted when on, with a cross when it can be taken away */
@Composable
fun SheetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    role: Role = Role.Button,
    stroke: Dp = 0.dp,
    textSize: TextUnit = 13.sp,
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.radius_default))
    val fill = colorResource(if (selected) R.color.accent_soft else R.color.surface_raised)
    val edge = colorResource(if (selected) R.color.accent else R.color.outline)
    Row(
        modifier
            .height(36.dp)
            .clip(shape)
            .background(fill)
            .then(if (stroke > 0.dp) Modifier.border(stroke, edge, shape) else Modifier)
            .clickable(role = role, onClick = onClick)
            .padding(horizontal = dimensionResource(R.dimen.margin_default)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            color = colorResource(R.color.text_primary),
            fontFamily = CondensedBlock,
            fontSize = textSize,
            maxLines = 1,
        )
        if (onClose != null) {
            Icon(
                painterResource(R.drawable.icon_close),
                contentDescription = stringResource(R.string.filter_remove),
                modifier = Modifier
                    .padding(start = dimensionResource(R.dimen.margin_narrow))
                    .size(20.dp)
                    .clickable(onClick = onClose),
                tint = colorResource(R.color.text_primary),
            )
        }
    }
}

