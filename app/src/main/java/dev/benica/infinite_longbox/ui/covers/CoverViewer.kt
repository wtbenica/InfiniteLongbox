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

package dev.benica.infinite_longbox.ui.covers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.theme.CondensedBlock
import java.io.File

/** A cover as large as the screen allows, with what it is underneath. A tap anywhere closes it. */
@Composable
fun CoverViewer(
    file: File,
    title: String,
    variantName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(R.dimen.margin_default)
    Column(
        modifier
            .fillMaxSize()
            .background(colorResource(R.color.surface_raised))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(margin),
    ) {
        AsyncImage(
            model = file,
            contentDescription = stringResource(R.string.description_issue_cover_set),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        Column(
            Modifier
                .padding(margin)
                .fillMaxWidth()
                .background(colorResource(R.color.accent_soft), RoundedCornerShape(dimensionResource(R.dimen.margin_wide)))
                .padding(dimensionResource(R.dimen.margin_wide)),
        ) {
            CoverCaption(title)
            if (variantName.isNotBlank()) CoverCaption(variantName)
        }
    }
}

@Composable
private fun CoverCaption(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontFamily = CondensedBlock,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
