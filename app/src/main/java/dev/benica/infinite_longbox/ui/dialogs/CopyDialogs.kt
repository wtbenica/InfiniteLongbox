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

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.ui.filter.SheetChip
import dev.benica.infinite_longbox.ui.issue.MyCopyState
import dev.benica.infinite_longbox.ui.issue.SignerRef
import dev.benica.infinite_longbox.ui.theme.Archivo
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Shows [content] (a dialog) over this activity, for screens that aren't Compose themselves. The
 * dialog calls the function it is given to go away, and the one returned does the same from outside.
 */
fun ComponentActivity.showDialog(content: @Composable (dismiss: () -> Unit) -> Unit): () -> Unit {
    val host = findViewById<ViewGroup>(android.R.id.content)
    val view = ComposeView(this)
    val dismiss = { host.removeView(view) }
    view.setContent { InfiniteLongboxTheme { content(dismiss) } }
    host.addView(view)
    return dismiss
}

/** Asks for a storage place, suggesting the ones already used so the same box isn't spelled two ways. */
@Composable
fun StoragePromptDialog(
    title: Int,
    message: String?,
    current: String?,
    clearLabel: Int?,
    onChosen: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(current.orEmpty()) }
    var places by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(Unit) { places = Repository.get().storageLocations() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column {
                message?.let { Text(it, Modifier.padding(bottom = 12.dp)) }
                SuggestField(
                    value = text,
                    onChange = { text = it },
                    label = stringResource(R.string.my_copy_storage),
                    suggestions = places.filter { it.contains(text.trim(), ignoreCase = true) && !it.equals(text.trim(), true) },
                    onPick = { text = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                text.trim().takeIf { it.isNotEmpty() }?.let(onChosen)
                onDismiss()
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            clearLabel?.let {
                TextButton(onClick = {
                    onChosen(null)
                    onDismiss()
                }) { Text(stringResource(it)) }
            }
        },
    )
}

/** Asks for what is kept about the copy in My Collection: who signed it, where it is, and so on. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalCoroutinesApi::class)
@Composable
fun MyCopyDialog(issueId: Int, current: MyCopyState, onDismiss: () -> Unit) {
    val signers = remember { mutableStateListOf<SignerRef>().apply { addAll(current.signedBy) } }
    var signerText by remember { mutableStateOf("") }
    var creators by remember { mutableStateOf(emptyList<SignerRef>()) }
    var storage by remember { mutableStateOf(current.storageLocation.orEmpty()) }
    var condition by remember { mutableStateOf(current.condition.orEmpty()) }
    var notes by remember { mutableStateOf(current.notes.orEmpty()) }
    var places by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(Unit) { places = Repository.get().storageLocations() }
    // Who signed it: creators, found as the filter finds them
    LaunchedEffect(signerText) {
        creators = if (signerText.isBlank()) emptyList()
        else Repository.get().findCreators(signerText).map { SignerRef(it.creatorId, it.name) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.my_copy_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    signers.forEach { signer ->
                        SheetChip(signer.name, selected = true, onClick = {}, onClose = { signers.remove(signer) })
                    }
                }
                SuggestField(
                    value = signerText,
                    onChange = { signerText = it },
                    label = stringResource(R.string.my_copy_signed_by),
                    suggestions = creators.map { it.name }.take(SUGGESTIONS),
                    onPick = { name ->
                        creators.firstOrNull { it.name == name }
                            ?.takeIf { pick -> signers.none { it.creatorId == pick.creatorId } }
                            ?.let { signers.add(it) }
                        signerText = ""
                    },
                )
                SuggestField(
                    value = storage,
                    onChange = { storage = it },
                    label = stringResource(R.string.my_copy_storage),
                    suggestions = places.filter { it.contains(storage.trim(), ignoreCase = true) && !it.equals(storage.trim(), true) },
                    onPick = { storage = it },
                )
                SuggestField(condition, { condition = it }, stringResource(R.string.my_copy_condition), emptyList(), {})
                SuggestField(notes, { notes = it }, stringResource(R.string.my_copy_notes), emptyList(), {}, multiline = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Repository.get().setCopyInfo(
                    issueId, BaseCollection.MY_COLL.id, storage, condition, notes, signers.map { it.creatorId },
                )
                onDismiss()
            }) { Text(stringResource(R.string.my_copy_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

private const val SUGGESTIONS = 5

/** Puts a popup under the box it is declared in, as far left as that box */
private object BelowAnchor : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ) = IntOffset(
        anchorBounds.left,
        anchorBounds.bottom.coerceAtMost(windowSize.height - popupContentSize.height).coerceAtLeast(0),
    )
}

/** A text field that, while it has focus, floats [suggestions] under itself; a tap picks one. */
@Composable
private fun SuggestField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    onPick: (String) -> Unit,
    multiline: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    var width by remember { mutableStateOf(0) }
    Box(Modifier.onSizeChanged { width = it.width }) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            label = { Text(label) },
            singleLine = !multiline,
            minLines = if (multiline) 2 else 1,
            keyboardOptions = KeyboardOptions(
                capitalization = if (multiline) KeyboardCapitalization.Sentences else KeyboardCapitalization.Words
            ),
        )
        if (focused && suggestions.isNotEmpty()) {
            Popup(popupPositionProvider = BelowAnchor, properties = PopupProperties(focusable = false)) {
                Surface(
                    modifier = Modifier.width(with(LocalDensity.current) { width.toDp() }),
                    shape = RoundedCornerShape(12.dp),
                    color = colorResource(R.color.surface_raised),
                    border = BorderStroke(1.dp, colorResource(R.color.outline)),
                    shadowElevation = 4.dp,
                ) {
                    Column {
                        suggestions.take(SUGGESTIONS).forEach { suggestion ->
                            Text(
                                suggestion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(suggestion) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                color = colorResource(R.color.text_primary),
                                fontFamily = Archivo,
                            )
                        }
                    }
                }
            }
        }
    }
}
