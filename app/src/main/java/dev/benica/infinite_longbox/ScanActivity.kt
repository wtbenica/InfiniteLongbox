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

package dev.benica.infinite_longbox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dev.benica.infinite_longbox.database.IssueDatabase
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.repository.MetronCoverOption
import dev.benica.infinite_longbox.repository.fetchCoverFromMetron
import dev.benica.infinite_longbox.repository.metronCoverOptions
import dev.benica.infinite_longbox.repository.metronCoverPreview
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import zxingcpp.BarcodeReader
import dev.benica.infinite_longbox.ui.covers.MetronCoverPicker
import dev.benica.infinite_longbox.ui.scan.ScanResultState
import dev.benica.infinite_longbox.ui.scan.ScanScreen
import dev.benica.infinite_longbox.ui.scan.ScanWindow
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Reads a comic's barcode: the 12-digit UPC plus the 5-digit add-on beside it, which says which
 * issue, cover and printing it is. (Google's code scanner ignores the add-on.)
 *
 * Each barcode is looked up and its issue added to My Collection, and the scanner stays open for
 * the next: the issue just added shows over the camera with Undo and Details. If the add-on never
 * shows up, e.g. the comic doesn't have one, the UPC alone is used after [ADD_ON_GRACE_MS].
 *
 * Returns [RESULT_OK] on Done or Details; Details also carries the issue in [EXTRA_ISSUE_ID] (and
 * [EXTRA_VARIANT_OF]), for the caller to open.
 */
private const val TAG = APP + "ScanActivity"

class ScanActivity : AppCompatActivity() {

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

    private class Added(val issueId: Int, val variantOf: Int?, val label: String)

    private val added = mutableStateListOf<Added>()
    private var latest by mutableStateOf<Added?>(null)

    /** The covers Metron has for a variant just added, to pick one from. */
    private var picker by mutableStateOf<Pair<Added, List<MetronCoverOption>>?>(null)

    private val requestCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, R.string.scan_camera_denied, Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The picture draws under the system bars; the hint and the light switch keep clear of them
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(ComposeView(this).apply {
            setContent {
                InfiniteLongboxTheme {
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
                            else requestCamera.launch(Manifest.permission.CAMERA)
                        },
                        onTapToFocus = ::focusAt,
                        result = shown?.let { ScanResultState(it.label, cover, canPickCover = it.variantOf != null) },
                        addedCount = added.size,
                        onUndo = { shown?.let(::undo) },
                        onDetails = { shown?.let(::showDetails) },
                        onPickCover = { shown?.let(::offerCovers) },
                        onShowAdded = ::showAdded,
                        onDone = ::finish,
                    )
                }
            }
        })
    }

    private fun coverOf(issueId: Int) =
        IssueDatabase.getInstance(this).issueDao().getFullIssue(issueId)
            .map { issue -> issue?.coverUri?.path?.let { File(it) }?.takeIf { it.exists() } }

    /** Width over height of what the analysis was given, turned upright. */
    private fun viewAspect(image: ImageProxy): Float {
        val crop = image.cropRect
        val upright = image.imageInfo.rotationDegrees % 180 == 0
        val w = if (upright) crop.width() else crop.height()
        val h = if (upright) crop.height() else crop.width()
        return w.toFloat() / h
    }

    /**
     * Whether the barcode [result] found is where the frame is (see [ScanWindow]).
     *
     * The position the reader gives is taken to be on the upright picture the analysis was cropped
     * to. If that isn't so (it might be on the sensor's own, unrotated, picture) the position is
     * tried that way too, and either one in the frame is enough, so a wrong guess about the
     * reader can't leave the scanner reading nothing.
     */
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
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        if (previewView == null) return
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
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

            // The analysis sees what the preview shows (not the wider picture), so a position in
            // it is a position on screen
            val group = UseCaseGroup.Builder().addUseCase(preview).addUseCase(analysis)
                .apply { previewView?.viewPort?.let { setViewPort(it) } }
                .build()
            provider.unbindAll()
            camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, group)
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * [text] is what the reader saw: the UPC-A (12 digits) or EAN-13, with the add-on after it
     * (separated by a space or not) when it was read.
     */
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
        runOnUiThread {
            lifecycleScope.launch {
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

    /**
     * [code] is the UPC (12 digits) followed by the add-on (5 digits) if it was read. The add-on
     * says which issue, cover and printing, so with it there's normally exactly one match.
     */
    private suspend fun resolve(code: String) {
        val digits = code.filter { it.isDigit() }
        val upc = digits.take(12)
        val addOn = digits.drop(12).takeIf { it.length == 5 }

        if (addOn == null) {
            // Only the UPC was read, so it covers the whole series: don't list a slice of it,
            // ask for the issue number (checking first whether it's actually one issue).
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

        // The add-on isn't always recorded the same way; fall back to the series and the
        // issue number (the first 3 digits of the add-on for most publishers)
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
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.scan_issue_number_hint)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.scan_which_number)
            .setMessage(R.string.scan_which_number_message)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                continuation.resume(input.text.toString().trim().trimStart('0'))
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> continuation.resume(null) }
            .setOnCancelListener { if (continuation.isActive) continuation.resume(null) }
            .show()
        continuation.invokeOnCancellation { dialog.dismiss() }
    }

    /** The barcode alone doesn't always pin down the issue; ask which one it is. */
    private suspend fun choose(matches: List<FullIssue>): FullIssue? =
        suspendCancellableCoroutine { continuation ->
            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.scan_choose_issue)
                .setItems(matches.map { it.scanLabel() }.toTypedArray()) { _, which ->
                    continuation.resume(matches[which])
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> continuation.resume(null) }
                .setOnCancelListener { if (continuation.isActive) continuation.resume(null) }
                .show()
            continuation.invokeOnCancellation { dialog.dismiss() }
        }

    private fun add(issue: FullIssue) {
        Repository.get().addToCollection(issue, BaseCollection.MY_COLL.id)
        val entry = Added(issue.issue.issueId, issue.issue.variantOf, issue.scanLabel())
        added.removeAll { it.issueId == entry.issueId }
        added.add(0, entry)
        latest = entry
    }

    private fun undo(entry: Added) {
        Repository.get().removeFromCollection(entry.issueId, BaseCollection.MY_COLL.id)
        added.remove(entry)
        latest = added.firstOrNull()
    }

    /** The covers Metron has for a variant whose cover couldn't be told from its name. */
    private fun offerCovers(entry: Added) {
        lifecycleScope.launch {
            val options = metronCoverOptions(entry.issueId, entry.variantOf)
            if (options.isNullOrEmpty()) notice(getString(R.string.cover_metron_none))
            else picker = entry to options
        }
    }

    private fun pickCover(entry: Added, option: MetronCoverOption) {
        picker = null
        lifecycleScope.launch {
            val result = fetchCoverFromMetron(applicationContext, entry.issueId, entry.variantOf, choice = option.id)
            notice(getString(result.message))
        }
    }

    private fun showDetails(entry: Added) {
        setResult(
            RESULT_OK,
            Intent().putExtra(EXTRA_ISSUE_ID, entry.issueId).putExtra(EXTRA_VARIANT_OF, entry.variantOf ?: -1)
        )
        finish()
    }

    /** Everything added since the scanner opened; a tap opens that issue's page. */
    private fun showAdded() {
        val entries = added.toList()
        AlertDialog.Builder(this)
            .setTitle(R.string.scan_added_title)
            .setItems(entries.map { it.label }.toTypedArray()) { _, which -> showDetails(entries[which]) }
            .setNegativeButton(R.string.scan_done, null)
            .show()
    }

    private fun notice(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    /** Series, number and variant, so the copies sharing one barcode can be told apart. */
    private fun FullIssue.scanLabel(): String {
        val variant = issue.variantName.ifBlank {
            if (issue.variantOf == null) getString(R.string.scan_standard_edition) else ""
        }
        return if (variant.isBlank()) "$this" else "$this ($variant)"
    }

    override fun onDestroy() {
        super.onDestroy()
        analysisExecutor.shutdown()
    }

    companion object {
        const val EXTRA_ISSUE_ID = "issue_id"
        const val EXTRA_VARIANT_OF = "variant_of"
        private const val ADD_ON_GRACE_MS = 3000L

        /** The same code read again within this long is the comic still in view, not another one. */
        private const val SAME_CODE_MS = 4000L
    }
}
