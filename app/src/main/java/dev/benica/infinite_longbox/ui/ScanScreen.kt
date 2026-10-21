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

package dev.benica.infinite_longbox.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.benica.infinite_longbox.R

/**
 * The barcode scanner: the camera filling the screen, a hint along the top and a light switch at
 * the bottom. The camera itself is the activity's to bind to the [PreviewView] it is handed.
 *
 * @param onPreviewView the view the camera should draw into, once it exists
 * @param onTapToFocus a tap on the picture, as a point on the [PreviewView], to focus there
 */
@Composable
fun ScanScreen(
    torchOn: Boolean,
    onTorchChange: (Boolean) -> Unit,
    onPreviewView: (PreviewView) -> Unit,
    onTapToFocus: (PreviewView, Offset) -> Unit,
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
        ScanOverlay(torchOn, onTorchChange)
    }
}

/** What sits over the camera: the hint, and the light switch. */
@Composable
private fun BoxScope.ScanOverlay(torchOn: Boolean, onTorchChange: (Boolean) -> Unit) {
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
    FilterChip(
        selected = torchOn,
        onClick = { onTorchChange(!torchOn) },
        label = { Text(stringResource(R.string.scan_light)) },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 32.dp),
    )
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun ScanOverlayPreview() {
    InfiniteLongboxTheme {
        Box(Modifier.fillMaxSize().background(Color.DarkGray)) {
            ScanOverlay(torchOn = true, onTorchChange = {})
        }
    }
}
