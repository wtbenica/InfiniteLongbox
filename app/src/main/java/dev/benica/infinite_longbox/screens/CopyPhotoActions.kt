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
package dev.benica.infinite_longbox.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dev.benica.infinite_longbox.MainActivity
import dev.benica.infinite_longbox.Notices
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.repository.CopyPhotos
import dev.benica.infinite_longbox.ui.covers.CoverViewer
import dev.benica.infinite_longbox.ui.covers.ViewerCover
import dev.benica.infinite_longbox.ui.dialogs.ChoiceDialog
import dev.benica.infinite_longbox.ui.dialogs.showDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.io.File

/** Adding, looking at and deleting photos of the copy of the issue [issueId] gives: taken now, or from this device. */
@OptIn(ExperimentalCoroutinesApi::class)
class CopyPhotoActions(
    private val activity: MainActivity,
    private val scope: CoroutineScope,
    private val issueId: () -> Int,
    private val photos: () -> List<File>,
) {
    // Set when composed: the launchers need the activity result registry
    private var takePicture: ManagedActivityResultLauncher<Uri, Boolean>? = null
    private var pickPicture: ManagedActivityResultLauncher<String, Uri?>? = null
    private var askCamera: ManagedActivityResultLauncher<String, Boolean>? = null

    /** Where the camera is putting the photo being taken. */
    private var capture: File? = null

    private var viewer by mutableStateOf<Pair<List<ViewerCover>, Int>?>(null)

    /** "Take a photo" or "Choose from this device". */
    fun add() {
        val options = listOf(activity.getString(R.string.copy_photo_take), activity.getString(R.string.copy_photo_choose))
        activity.showDialog { dismiss ->
            ChoiceDialog(
                R.string.copy_photo_title, options, android.R.string.cancel,
                onChoose = { which ->
                    when (which) {
                        0 -> take()
                        1 -> pickPicture?.launch("image/*")
                    }
                },
                dismiss = dismiss,
            )
        }
    }

    private fun take() {
        // The manifest asks for the camera (for the scanner), so the camera app can't be used without it
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            askCamera?.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val file = File(File(activity.cacheDir, "photos").apply { mkdirs() }, "capture.jpg")
        capture = file
        takePicture?.launch(FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file))
    }

    private fun keep(source: Uri) {
        scope.launch {
            val ok = CopyPhotos.add(issueId(), source)
            capture?.delete()
            Notices.show(if (ok) R.string.copy_photo_added else R.string.copy_photo_failed)
        }
    }

    /** Opens the photo at [index] (of the copy's photos) in the viewer. */
    fun open(index: Int) {
        val shown = photos()
        if (index !in shown.indices) return
        viewer = shown.mapIndexed { i, file ->
            ViewerCover(-1 - i, activity.getString(R.string.copy_photo_title), "", file, "photo$i")
        } to index
    }

    /** A long press on a photo: offers to delete it. */
    fun options(index: Int) {
        val photo = photos().getOrNull(index) ?: return
        activity.showDialog { dismiss ->
            ChoiceDialog(
                R.string.copy_photo_title, listOf(activity.getString(R.string.copy_photo_delete)), android.R.string.cancel,
                onChoose = { which -> if (which == 0) CopyPhotos.remove(photo) },
                dismiss = dismiss,
            )
        }
    }

    /** What the actions put on screen: the viewer, and the launchers for the camera and the picker. */
    @Composable
    fun Dialogs() {
        val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
            val file = capture
            if (taken && file != null) keep(Uri.fromFile(file)) else file?.delete()
        }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(::keep) }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera() else Notices.show(R.string.copy_photo_camera_denied)
        }
        SideEffect {
            takePicture = camera
            pickPicture = picker
            askCamera = permission
        }
        viewer?.let { (covers, start) ->
            Dialog(
                onDismissRequest = { viewer = null },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) { CoverViewer(covers, start, onDismiss = { viewer = null }) }
        }
    }
}
