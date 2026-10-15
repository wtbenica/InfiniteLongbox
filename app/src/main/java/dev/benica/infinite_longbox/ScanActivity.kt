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
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import zxingcpp.BarcodeReader

/**
 * Reads a comic's barcode: the 12-digit UPC plus the 5-digit add-on beside it, which says which
 * issue, cover and printing it is. (Google's code scanner ignores the add-on.)
 *
 * Returns the UPC and add-on joined as digits in [EXTRA_UPC]. If the add-on never shows up, e.g.
 * the comic doesn't have one, returns the UPC alone after [ADD_ON_GRACE_MS].
 */
class ScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
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

    @Volatile
    private var finished = false
    private var baseSeen: String? = null
    private var baseSeenAt = 0L

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
        setContentView(R.layout.activity_scan)

        previewView = findViewById(R.id.scan_preview)

        // The window draws under the system bars; keep the hint and button clear of them
        val hint = findViewById<View>(R.id.scan_hint)
        val torch = findViewById<View>(R.id.scan_torch)
        val hintPadding = hint.paddingTop
        val torchMargin = (torch.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scan_root)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            hint.updatePadding(top = hintPadding + bars.top)
            (torch.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                torchMargin + bars.bottom
            torch.requestLayout()
            insets
        }
        findViewById<CompoundButton>(R.id.scan_torch).setOnCheckedChangeListener { _, on ->
            camera?.cameraControl?.enableTorch(on)
        }
        // Tap to focus on the barcode
        previewView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                camera?.cameraControl?.startFocusAndMetering(
                    FocusMeteringAction.Builder(point).setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build()
                )
                v.performClick()
            }
            true
        }

        if (hasCameraPermission()) startCamera() else requestCamera.launch(Manifest.permission.CAMERA)
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
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
                try {
                    if (!finished) reader.read(image).forEach { onRead(it.text.orEmpty()) }
                } finally {
                    image.close()
                }
            }

            provider.unbindAll()
            camera = provider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * [text] is what the reader saw: the UPC-A (12 digits) or EAN-13, with the add-on after it
     * (separated by a space or not) when it was read.
     */
    private fun onRead(text: String) {
        val digits = text.filter { it.isDigit() }
        val code = when {
            digits.length == 17 -> digits
            digits.length == 18 && digits.startsWith("0") -> digits.drop(1)
            digits.length == 12 -> digits
            digits.length == 13 && digits.startsWith("0") -> digits.drop(1)
            else -> return
        }

        if (code.length == 17) {
            finishWith(code)
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (code != baseSeen) {
            baseSeen = code
            baseSeenAt = now
        } else if (now - baseSeenAt >= ADD_ON_GRACE_MS) {
            finishWith(code)
        }
    }

    private fun finishWith(code: String) {
        if (finished) return
        finished = true
        runOnUiThread {
            setResult(RESULT_OK, Intent().putExtra(EXTRA_UPC, code))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        analysisExecutor.shutdown()
    }

    companion object {
        const val EXTRA_UPC = "upc"
        private const val ADD_ON_GRACE_MS = 3000L
    }
}
