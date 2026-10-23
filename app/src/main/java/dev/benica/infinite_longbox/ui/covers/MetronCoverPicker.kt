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

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.repository.MetronCoverOption

/**
 * The covers Metron has for an issue, to pick one from. [loadPreview] gets the picture of an
 * option (null if it can't be had); a tap on a cover is the choice.
 */
@Composable
fun MetronCoverPicker(
    options: List<MetronCoverOption>,
    loadPreview: suspend (MetronCoverOption) -> Bitmap?,
    onPick: (MetronCoverOption) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colorResource(R.color.surface_raised),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.cover_metron_pick),
                    color = colorResource(R.color.text_primary),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                    items(options, key = { it.id }) { option ->
                        Column(
                            Modifier
                                .padding(4.dp)
                                .clickable { onPick(option) }
                        ) {
                            val preview by produceState<Bitmap?>(null, option.id) { value = loadPreview(option) }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.65f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorResource(R.color.surface_sunken))
                            ) {
                                preview?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = option.name,
                                        contentScale = ContentScale.Crop,
                                        alignment = Alignment.TopCenter,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            Text(
                                text = option.name.ifBlank { stringResource(R.string.cover_metron_standard) },
                                color = colorResource(R.color.text_secondary),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
