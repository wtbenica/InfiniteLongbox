package com.wtb.comiccollector

import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class IssueBarcodeScanner {
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_UPC_A
        )
        .build()

    fun getBarcode(image: InputImage) {
        val scanner: BarcodeScanner = BarcodeScanning.getClient(options)

        val result: Task<MutableList<Barcode>> = scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    Toast.makeText(context!!, barcode.rawValue, Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {

            }
    }
}