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

package dev.benica.infinite_longbox.ui.issue

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import java.io.File
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.theme.CondensedBlock
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme

/** What is kept about the copy in My Collection; each part is optional. */
data class MyCopyState(
    val signedBy: List<SignerRef> = emptyList(),
    val storageLocation: String? = null,
    val condition: String? = null,
    val notes: String? = null,
    val photos: List<File> = emptyList(),
) {
    val isEmpty: Boolean
        get() = signedBy.isEmpty() && listOf(storageLocation, condition, notes).all { it.isNullOrBlank() }
}

/** A creator who signed the copy, who the card links to. */
data class SignerRef(val creatorId: Int, val name: String)

/** "My copy": who signed it, where it is stored, its condition and notes, with an Edit button. */
@Composable
fun MyCopyCard(
    state: MyCopyState,
    onEdit: () -> Unit,
    onSigner: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onAddPhoto: () -> Unit = {},
    onPhoto: (Int) -> Unit = {},
    onPhotoLongClick: (Int) -> Unit = {},
) {
    val margin = dimensionResource(R.dimen.margin_default)
    Column(
        modifier
            .fillMaxWidth()
            .background(colorResource(R.color.surface_raised), RoundedCornerShape(margin))
            .padding(margin)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.my_copy_title),
                modifier = Modifier.weight(1f),
                color = colorResource(R.color.accent_strong),
                fontFamily = CondensedBlock,
                fontSize = 16.sp,
            )
            TextButton(onClick = onAddPhoto) { Text(stringResource(R.string.copy_photo_add)) }
            TextButton(onClick = onEdit) {
                Text(stringResource(if (state.isEmpty) R.string.my_copy_add else R.string.my_copy_edit))
            }
        }
        if (!state.isEmpty) {
            if (state.signedBy.isNotEmpty()) {
                FlowRow(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = stringResource(R.string.my_copy_signed_by),
                        modifier = Modifier.padding(end = 8.dp),
                        color = colorResource(R.color.text_secondary),
                        fontFamily = CondensedBlock,
                    )
                    state.signedBy.forEachIndexed { index, signer ->
                        Text(
                            text = signer.name + if (index < state.signedBy.lastIndex) "," else "",
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clickable { onSigner(signer.creatorId) },
                            color = colorResource(R.color.accent_strong),
                            fontFamily = CondensedBlock,
                            textDecoration = TextDecoration.Underline,
                        )
                    }
                }
            }
            CopyLine(label = R.string.my_copy_storage, value = state.storageLocation)
            CopyLine(label = R.string.my_copy_condition, value = state.condition)
            CopyLine(label = R.string.my_copy_notes, value = state.notes)
        }
        if (state.photos.isNotEmpty()) {
            Photos(state.photos, onPhoto, onPhotoLongClick, Modifier.padding(top = 8.dp))
        }
    }
}

/** Small pictures of the copy, side by side; a tap opens one and a long press offers to delete it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Photos(
    photos: List<File>,
    onPhoto: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        photos.forEachIndexed { index, file ->
            AsyncImage(
                model = ImageRequest.Builder(context).data(file).memoryCacheKey("\${file.path}@\${file.lastModified()}")
                    .size(Size(THUMB_PX, THUMB_PX)).build(),
                contentDescription = stringResource(R.string.copy_photo_title),
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(onClick = { onPhoto(index) }, onLongClick = { onLongClick(index) }),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private const val THUMB_PX = 288

@Composable
private fun CopyLine(label: Int, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(
            text = stringResource(label),
            modifier = Modifier.padding(end = 8.dp),
            color = colorResource(R.color.text_secondary),
            fontFamily = CondensedBlock,
        )
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontFamily = CondensedBlock)
    }
}

@Preview(showBackground = true)
@Composable
private fun MyCopyCardPreview() {
    InfiniteLongboxTheme {
        MyCopyCard(MyCopyState(signedBy = listOf(SignerRef(1, "Grant Morrison")), storageLocation = "Long box 4", condition = "VF"), {}, {})
    }
}
