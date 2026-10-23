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

import androidx.compose.foundation.background
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
    }
}

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
