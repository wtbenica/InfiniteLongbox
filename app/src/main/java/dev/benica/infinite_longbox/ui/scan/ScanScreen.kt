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

package dev.benica.infinite_longbox.ui.scan

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import java.io.File

/** The issue a scan just added, as the strip over the camera shows it. */
data class ScanResultState(
    val label: String,
    val cover: File?,
    val canPickCover: Boolean = false,
    /** Where the copy was put, when the scanner is filing copies somewhere. */
    val storage: String? = null,
)

/**
 * The barcode scanner: the camera filling the screen, a hint along the top, and along the bottom
 * the issue just added (with Undo and Details), a light switch, the count added and Done. The
 * camera itself is the activity's to bind to the [PreviewView] it is handed.
 *
 * @param onPreviewView the view the camera should draw into, once it exists
 * @param onTapToFocus a tap on the picture, as a point on the [PreviewView], to focus there
 * @param result what the last scan added, if anything
 * @param addedCount how many scans have added an issue since the scanner opened
 */
@Composable
fun ScanScreen(
    torchOn: Boolean,
    onTorchChange: (Boolean) -> Unit,
    onPreviewView: (PreviewView) -> Unit,
    onTapToFocus: (PreviewView, Offset) -> Unit,
    result: ScanResultState?,
    addedCount: Int,
    storage: String?,
    onUndo: () -> Unit,
    onDetails: () -> Unit,
    onPickCover: () -> Unit,
    onStorage: () -> Unit,
    onShowAdded: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                PreviewView(context).also {
                    previewView = it
                    onPreviewView(it)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { point -> previewView?.let { onTapToFocus(it, point) } }
                },
        )
        ScanFrame()
        ScanOverlay(torchOn, onTorchChange, result, addedCount, storage, onUndo, onDetails, onPickCover, onStorage, onShowAdded, onDone)
    }
}

/**
 * The frame to line the barcode up in (see [ScanWindow]): the picture outside it is dimmed and the
 * corners are marked.
 */
@Composable
private fun ScanFrame() {
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val r = ScanWindow.rect(size.width / size.height)
        val topLeft = Offset(r.left * size.width, r.top * size.height)
        val frame = Size((r.right - r.left) * size.width, (r.bottom - r.top) * size.height)
        val corner = CornerRadius(12.dp.toPx())

        drawRect(Color(0x66000000))
        drawRoundRect(Color.Transparent, topLeft, frame, corner, blendMode = BlendMode.Clear)
        drawRoundRect(Color.White, topLeft, frame, corner, style = Stroke(width = 2.dp.toPx()))
    }
}

/** What sits over the camera: the hint, the last issue added, and the buttons. */
@Composable
private fun BoxScope.ScanOverlay(
    torchOn: Boolean,
    onTorchChange: (Boolean) -> Unit,
    result: ScanResultState?,
    addedCount: Int,
    storage: String?,
    onUndo: () -> Unit,
    onDetails: () -> Unit,
    onPickCover: () -> Unit,
    onStorage: () -> Unit,
    onShowAdded: () -> Unit,
    onDone: () -> Unit,
) {
    Text(
        text = stringResource(R.string.scan_hint),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .background(Color(0x99000000))
            .statusBarsPadding()
            .padding(16.dp),
        color = Color.White,
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (result != null) ScanResultStrip(result, onUndo, onDetails, onPickCover)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = torchOn,
                onClick = { onTorchChange(!torchOn) },
                label = { Text(stringResource(R.string.scan_light)) },
            )
            AssistChip(
                onClick = onStorage,
                label = {
                    Text(
                        if (storage == null) stringResource(R.string.scan_storage_set)
                        else stringResource(R.string.scan_storage_in, storage),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
            if (addedCount > 0) {
                AssistChip(
                    onClick = onShowAdded,
                    label = { Text(stringResource(R.string.scan_added_count, addedCount)) },
                )
            }
            Button(onClick = onDone) { Text(stringResource(R.string.scan_done)) }
        }
    }
}

/** The cover and name of what was just added, with Undo and Details. */
@Composable
private fun ScanResultStrip(result: ScanResultState, onUndo: () -> Unit, onDetails: () -> Unit, onPickCover: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colorResource(R.color.surface_raised),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(width = 52.dp, height = 78.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colorResource(R.color.surface_sunken))
            ) {
                result.cover?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.scan_added, result.label) +
                        (result.storage?.let { " → $it" } ?: ""),
                    color = colorResource(R.color.text_primary),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row {
                    TextButton(onClick = onUndo) { Text(stringResource(R.string.scan_undo)) }
                    TextButton(onClick = onDetails) { Text(stringResource(R.string.scan_details)) }
                    if (result.canPickCover && result.cover == null) {
                        TextButton(onClick = onPickCover) { Text(stringResource(R.string.scan_pick_cover)) }
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun ScanOverlayPreview() {
    InfiniteLongboxTheme {
        Box(Modifier.fillMaxSize().background(Color.DarkGray)) {
            ScanOverlay(torchOn = true, onTorchChange = {}, result = ScanResultState("Batman (2016) #25", null, canPickCover = true, storage = "Box 2"), addedCount = 3, storage = "Box 2", onUndo = {}, onDetails = {}, onPickCover = {}, onStorage = {}, onShowAdded = {}, onDone = {})
        }
    }
}
