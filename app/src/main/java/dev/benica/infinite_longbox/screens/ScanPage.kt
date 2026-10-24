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

import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.BuildConfig
import dev.benica.infinite_longbox.R

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import android.util.Size
import dev.benica.infinite_longbox.Notices
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.benica.infinite_longbox.nav.Screen
import dev.benica.infinite_longbox.nav.ScreenHost
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import dev.benica.infinite_longbox.database.IssueDatabase
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.ui.dialogs.StoragePrompt
import dev.benica.infinite_longbox.ui.dialogs.ChoiceDialog
import dev.benica.infinite_longbox.ui.dialogs.NumberDialog
import dev.benica.infinite_longbox.ui.dialogs.showDialog
import dev.benica.infinite_longbox.repository.MetronCoverOption
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.repository.fetchCoverFromMetron
import dev.benica.infinite_longbox.repository.metronCoverOptions
import dev.benica.infinite_longbox.repository.metronCoverPreview
import dev.benica.infinite_longbox.ui.covers.MetronCoverPicker
import dev.benica.infinite_longbox.ui.scan.ScanResultState
import dev.benica.infinite_longbox.ui.scan.ScanScreen
import dev.benica.infinite_longbox.ui.scan.ScanWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import zxingcpp.BarcodeReader
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

private const val TAG = APP + "ScanPage"

/** Scans a comic's barcode (UPC plus 5-digit add-on) and adds the issue to My Collection, staying open for the next. */
class ScanPage(host: ScreenHost) : Screen(host) {

    private var previewView: PreviewView? = null
    private var torchOn by mutableStateOf(false)
    private var camera: Camera? = null
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val reader = BarcodeReader(
        BarcodeReader.Options(
            formats = setOf(BarcodeReader.Format.UPC_A, BarcodeReader.Format.EAN_13),
            tryHarder = true,
            tryRotate = true,
            tryDownscale = true,
            eanAddOnSymbol = BarcodeReader.EanAddOnSymbol.READ,
        )
    )

    /** A code is being looked up or asked about: the camera reads nothing meanwhile. */
    @Volatile
    private var busy = false
    private var baseSeen: String? = null
    private var baseSeenAt = 0L

    /** What the last scan was, so holding the same comic in view doesn't add it twice. */
    private var lastCode: String? = null
    private var lastCodeAt = 0L

    private class Added(val issueId: Int, val variantOf: Int?, val label: String, val storage: String? = null)

    private val added = mutableStateListOf<Added>()
    private var latest by mutableStateOf<Added?>(null)

    /** Where the copies scanned now are being put, or null for nowhere in particular. */
    private var storage by mutableStateOf<String?>(null)

    /** The covers Metron has for a variant just added, to pick one from. */
    private var picker by mutableStateOf<Pair<Added, List<MetronCoverOption>>?>(null)

    @Composable
    override fun Content() {
        val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Notices.show(R.string.scan_camera_denied, long = true)
                activity.goBack()
            }
        }
        // The launcher isn't ready until this composition is applied, so it can't be asked from the preview's factory
        LaunchedEffect(Unit) {
            if (!hasCameraPermission()) requestCamera.launch(Manifest.permission.CAMERA)
        }
            val shown = latest
            // The cover arrives a moment after the issue is added: follow it
            val cover by remember(shown?.issueId) {
                shown?.let { coverOf(it.issueId) } ?: kotlinx.coroutines.flow.flowOf(null)
            }.collectAsState(null)
            picker?.let { (entry, options) ->
                MetronCoverPicker(
                    options = options,
                    loadPreview = { metronCoverPreview(entry.issueId, entry.variantOf, it.id) },
                    onPick = { pickCover(entry, it) },
                    onDismiss = { picker = null },
                )
            }
            ScanScreen(
                torchOn = torchOn,
                onTorchChange = {
                    torchOn = it
                    camera?.cameraControl?.enableTorch(it)
                },
                onPreviewView = { view ->
                    previewView = view
                    if (hasCameraPermission()) startCamera()
                },
                onTapToFocus = ::focusAt,
                result = shown?.let { ScanResultState(it.label, cover, canPickCover = it.variantOf != null, storage = it.storage) },
                storage = storage,
                onStorage = ::askStorage,
                addedCount = added.size,
                onUndo = { shown?.let(::undo) },
                onDetails = { shown?.let(::showDetails) },
                onPickCover = { shown?.let(::offerCovers) },
                onShowAdded = ::showAdded,
                onDone = activity::goBack,
            )
    }

    private fun coverOf(issueId: Int) =
        IssueDatabase.getInstance(context).issueDao().getFullIssue(issueId)
            .map { issue -> issue?.coverUri?.path?.let { File(it) }?.takeIf { it.exists() } }

    /** Width over height of what the analysis was given, turned upright. */
    private fun viewAspect(image: ImageProxy): Float {
        val crop = image.cropRect
        val upright = image.imageInfo.rotationDegrees % 180 == 0
        val w = if (upright) crop.width() else crop.height()
        val h = if (upright) crop.height() else crop.width()
        return w.toFloat() / h
    }

    /** Whether the barcode [result] found is inside the frame (see [ScanWindow]). */
    private fun inFrame(result: BarcodeReader.Result, image: ImageProxy, aspect: Float): Boolean {
        val crop = image.cropRect
        val rotation = image.imageInfo.rotationDegrees
        val rawW = crop.width().toFloat()
        val rawH = crop.height().toFloat()
        val upright = rotation % 180 == 0
        val w = if (upright) rawW else rawH
        val h = if (upright) rawH else rawW
        val p = result.position
        val points = listOf(p.topLeft, p.topRight, p.bottomRight, p.bottomLeft)
        val x = points.map { it.x }.average().toFloat()
        val y = points.map { it.y }.average().toFloat()

        val asUpright = ScanWindow.accepts(x / w, y / h, aspect)
        val (ux, uy) = when (rotation) {
            90 -> (rawH - y) / h to x / w
            180 -> (rawW - x) / w to (rawH - y) / h
            270 -> y / h to (rawW - x) / w
            else -> x / w to y / h
        }
        val asRaw = ScanWindow.accepts(ux, uy, aspect)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "read at ($x, $y) of raw ${rawW}x$rawH rotated $rotation: in frame upright=$asUpright raw=$asRaw")
        }
        return asUpright || asRaw
    }

    /** Tap to focus on the barcode. */
    private fun focusAt(view: PreviewView, point: Offset) {
        val metering = view.meteringPointFactory.createPoint(point.x, point.y)
        camera?.cameraControl?.startFocusAndMetering(
            FocusMeteringAction.Builder(metering).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
        )
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        if (previewView == null) return
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            // Backed out before the camera was ready: there is nothing to bind to
            if (viewLifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return@addListener
            val provider = providerFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView?.surfaceProvider
            }

            // The add-on is small; ask for a high resolution so its bars are resolved
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1920, 1080),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        ).build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analysisExecutor) { image ->
                image.use {
                    if (!busy) {
                        val aspect = viewAspect(it)
                        reader.read(it).forEach { result ->
                            if (inFrame(result, it, aspect)) onRead(result.text.orEmpty())
                        }
                    }
                }
            }

            // The analysis sees what the preview shows, so a position in it is a position on screen
            val group = UseCaseGroup.Builder().addUseCase(preview).addUseCase(analysis)
                .apply { previewView?.viewPort?.let { setViewPort(it) } }
                .build()
            provider.unbindAll()
            camera = provider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, group)
        }, ContextCompat.getMainExecutor(context))
    }

    /** [text] is what the reader saw: a UPC-A or EAN-13, with the add-on if it was read. */
    private fun onRead(text: String) {
        val digits = text.filter { it.isDigit() }
        val code = when (digits.length) {
            17 -> digits
            18 if digits.startsWith("0") -> digits.drop(1)
            12 -> digits
            13 if digits.startsWith("0") -> digits.drop(1)
            else -> return
        }

        if (code.length == 17) {
            handle(code)
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (code != baseSeen) {
            baseSeen = code
            baseSeenAt = now
        } else if (now - baseSeenAt >= ADD_ON_GRACE_MS) {
            handle(code)
        }
    }

    /** Looks the barcode up and adds the issue; the camera waits until that's done. */
    private fun handle(code: String) {
        val now = SystemClock.elapsedRealtime()
        if (busy || (code == lastCode && now - lastCodeAt < SAME_CODE_MS)) return
        busy = true
        activity.runOnUiThread {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    resolve(code)
                } finally {
                    lastCode = code
                    lastCodeAt = SystemClock.elapsedRealtime()
                    baseSeen = null
                    busy = false
                }
            }
        }
    }

    /** [code] is the UPC plus the add-on if it was read; with the add-on there is normally one match. */
    private suspend fun resolve(code: String) {
        val digits = code.filter { it.isDigit() }
        val upc = digits.take(12)
        val addOn = digits.drop(12).takeIf { it.length == 5 }

        if (addOn == null) {
            // UPC only: it covers the whole series, so ask for the issue number
            val probe = lookUp(upc, limit = 2)
            when {
                probe == null -> notice(getString(R.string.scan_lookup_failed))
                probe.isEmpty() -> notice(getString(R.string.scan_no_match, upc))
                probe.size == 1 -> add(probe[0])
                else -> askIssueNumber(upc)
            }
            return
        }

        // Exact barcode first
        var matches = lookUp(digits, limit = 30)

        // No add-on match: fall back to the series and issue number
        if (matches != null && matches.isEmpty()) {
            val number = addOn.take(3).trimStart('0').ifEmpty { null }
            if (number != null) matches = lookUp(upc, number)
        }

        when {
            matches == null -> notice(getString(R.string.scan_lookup_failed))
            matches.isEmpty() -> notice(getString(R.string.scan_no_match, digits))
            matches.size == 1 -> add(matches[0])
            else -> choose(matches)?.let { add(it) }
        }
    }

    private suspend fun lookUp(upc: String, number: String? = null, limit: Int? = null) =
        withContext(Dispatchers.IO) { Repository.get().getIssuesByUpc(upc, number, limit) }

    private suspend fun askIssueNumber(upc: String) {
        val number = ask() ?: return
        val matches = lookUp(upc, number.ifEmpty { null })
        when {
            matches == null -> notice(getString(R.string.scan_lookup_failed))
            matches.isEmpty() -> notice(getString(R.string.scan_no_match_number, number))
            matches.size == 1 -> add(matches[0])
            else -> choose(matches)?.let { add(it) }
        }
    }

    /** The issue number, typed in; null if cancelled. */
    private suspend fun ask(): String? = suspendCancellableCoroutine { continuation ->
        val close = activity.showDialog { dismiss ->
            NumberDialog(
                R.string.scan_which_number, R.string.scan_which_number_message, R.string.scan_issue_number_hint,
                onResult = { if (continuation.isActive) continuation.resume(it?.trim()?.trimStart('0')) },
                dismiss = dismiss,
            )
        }
        continuation.invokeOnCancellation { close() }
    }

    /** The barcode alone doesn't always pin down the issue; ask which one it is. */
    private suspend fun choose(matches: List<FullIssue>): FullIssue? =
        suspendCancellableCoroutine { continuation ->
            val close = activity.showDialog { dismiss ->
                ChoiceDialog(
                    R.string.scan_choose_issue, matches.map { it.scanLabel() }, android.R.string.cancel,
                    onChoose = { if (continuation.isActive) continuation.resume(it?.let(matches::get)) },
                    dismiss = dismiss,
                )
            }
            continuation.invokeOnCancellation { close() }
        }

    private fun add(issue: FullIssue) {
        Repository.get().addToCollection(issue, BaseCollection.MY_COLL.id, storage)
        val entry = Added(issue.issue.issueId, issue.issue.variantOf, issue.scanLabel(), storage)
        added.removeAll { it.issueId == entry.issueId }
        added.add(0, entry)
        latest = entry
    }

    /** "The rest of these are going in Box 2": asked once, then every copy scanned is filed there. */
    private fun askStorage() {
        StoragePrompt.show(
            activity, R.string.scan_storage_title, getString(R.string.scan_storage_message),
            storage, R.string.scan_storage_clear,
        ) { storage = it }
    }

    private fun undo(entry: Added) {
        Repository.get().removeFromCollection(entry.issueId, BaseCollection.MY_COLL.id)
        added.remove(entry)
        latest = added.firstOrNull()
    }

    /** The covers Metron has for a variant whose cover couldn't be told from its name. */
    private fun offerCovers(entry: Added) {
        viewLifecycleOwner.lifecycleScope.launch {
            val options = metronCoverOptions(entry.issueId, entry.variantOf)
            if (options.isNullOrEmpty()) notice(getString(R.string.cover_metron_none))
            else picker = entry to options
        }
    }

    private fun pickCover(entry: Added, option: MetronCoverOption) {
        picker = null
        viewLifecycleOwner.lifecycleScope.launch {
            val result = fetchCoverFromMetron(context.applicationContext, entry.issueId, entry.variantOf, choice = option.id)
            notice(getString(result.message))
        }
    }

    private fun showDetails(entry: Added) {
        activity.showScannedIssue(entry.issueId, entry.variantOf)
    }

    /** Everything added since the scanner opened; a tap opens that issue's page. */
    private fun showAdded() {
        val entries = added.toList()
        activity.showDialog { dismiss ->
            ChoiceDialog(
                R.string.scan_added_title, entries.map { it.label }, R.string.scan_done,
                onChoose = { which -> which?.let { showDetails(entries[it]) } },
                dismiss = dismiss,
            )
        }
    }

    private fun notice(message: String) = Notices.show(message, long = true)

    /** Series, number and variant, so the copies sharing one barcode can be told apart. */
    private fun FullIssue.scanLabel(): String {
        val variant = issue.variantName.ifBlank {
            if (issue.variantOf == null) getString(R.string.scan_standard_edition) else ""
        }
        return if (variant.isBlank()) "$this" else "$this ($variant)"
    }

    override fun onHidden() {
        analysisExecutor.shutdown()
    }

    private companion object {
        const val ADD_ON_GRACE_MS = 3000L

        /** The same code read again within this long is the comic still in view, not another one. */
        const val SAME_CODE_MS = 4000L
    }
}
