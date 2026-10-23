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

package dev.benica.infinite_longbox.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.common.linkedString
import dev.benica.infinite_longbox.ui.theme.CondensedBlock

/** Who made the data and the covers possible, and under what licence. A tap anywhere closes it. */
@Composable
fun AboutScreen(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val margin = dimensionResource(R.dimen.margin_default)
    Box(
        modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(dimensionResource(R.dimen.margin_wide)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(margin),
            color = colorResource(R.color.surface_raised),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(margin * 2),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(margin),
            ) {
                Heading(stringResource(R.string.about_thanks))
                Text(linkedString(R.string.gcd_thanks), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Heading(stringResource(R.string.about_copyright_notices))
                Text(linkedString(R.string.text_gcd_cc_credit), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(linkedString(R.string.text_cover_credit), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.text_gcd_cc_changes), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.about_cover_acknowledgement), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontFamily = CondensedBlock,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
