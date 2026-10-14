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


package dev.benica.infinite_longbox.repository

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.BuildConfig
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.Webservice
import dev.benica.infinite_longbox.database.IssueDatabase
import dev.benica.infinite_longbox.database.models.Cover
import dev.benica.infinite_longbox.network.RetrofitAPIClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import kotlin.math.max

private const val TAG = APP + "CoverStore"

/** Covers are scaled down to this on their longest side when picked; plenty for a phone. */
private const val MAX_COVER_SIDE = 2400

/**
 * Decodes an image the user picked. Never returns a partly decoded picture it hasn't judged.
 *
 * A picked image comes from another app (Files, Drive, Photos, ...) through a content URI, and
 * the app that serves it can hand over less than the whole file: Google Photos does that for items
 * that aren't saved on the phone. So the image is read through several different routes, each into
 * a temp file that is decoded with ImageDecoder (which reports incomplete input instead of
 * returning half an image, and applies the photo's rotation). The first route that yields a
 * complete image wins. Each route's result is logged, which shows which routes work.
 *
 * If no route is complete, a nearly complete one (see [MAX_MISSING_FRACTION]) is accepted.
 *
 * @throws java.io.IOException if no route gives a complete or nearly complete image
 */
private fun decodeCover(context: Context, source: Uri): DecodedCover {
    val resolver = context.contentResolver

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        val attempt = copyToTemp(context, resolver, source, readRoutes[0])
        try {
            return DecodedCover(decodeWithBitmapFactory(attempt.file), 0f)
        } finally {
            attempt.file.delete()
        }
    }

    var best: DecodedCover? = null
    var firstAttempt: File? = null
    val temps = mutableListOf<File>()

    try {
        for (route in readRoutes) {
            val attempt = try {
                copyToTemp(context, resolver, source, route)
            } catch (e: Exception) {
                Log.d(TAG, "Route ${route.name}: failed to read (${e.javaClass.simpleName}: ${e.message})")
                continue
            }
            temps.add(attempt.file)
            if (firstAttempt == null) firstAttempt = attempt.file

            if (attempt.declared > 0 && attempt.file.length() != attempt.declared) {
                Log.d(TAG, "Route ${route.name}: got ${attempt.file.length()} of ${attempt.declared} bytes")
                continue
            }

            val decoded = try {
                decodeWithImageDecoder(attempt.file)
            } catch (e: Exception) {
                Log.d(TAG, "Route ${route.name}: can't decode (${e.javaClass.simpleName}: ${e.message})")
                continue
            }
            Log.d(
                TAG,
                "Route ${route.name}: ${attempt.file.length()} bytes, type=${attempt.type}, " +
                        if (decoded.incomplete) "incomplete, ${(decoded.missing * 100).toInt()}% blank"
                        else "complete"
            )
            if (!decoded.incomplete) return decoded
            if (best == null || decoded.missing < best.missing) best = decoded
        }

        // Last route: let ImageDecoder read the URI itself (it opens it differently again)
        try {
            val direct = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, source)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                var sample = 1
                while (max(info.size.width, info.size.height) / sample > MAX_COVER_SIDE) sample *= 2
                if (sample > 1) decoder.setTargetSampleSize(sample)
            }
            Log.d(TAG, "Route direct: complete")
            return DecodedCover(direct, 0f)
        } catch (e: Exception) {
            Log.d(TAG, "Route direct: ${e.javaClass.simpleName}: ${e.message}")
        }

        best?.takeIf { it.missing <= MAX_MISSING_FRACTION }?.let { return it }

        firstAttempt?.let { keepFailedPick(context, it) }
        throw IOException(
            best?.let { "Image is cut off: ${(it.missing * 100).toInt()}% of it is missing" }
                ?: "No way of reading the image gave a usable result"
        )
    } finally {
        temps.forEach { it.delete() }
    }
}

/** One way of reading a content URI. */
private class ReadRoute(val name: String, val open: (ContentResolver, Uri) -> InputStream?)

private val readRoutes = listOf(
    ReadRoute("stream") { resolver, uri -> resolver.openInputStream(uri) },
    ReadRoute("fileDescriptor") { resolver, uri ->
        resolver.openFileDescriptor(uri, "r")?.let { ParcelFileDescriptor.AutoCloseInputStream(it) }
    },
    ReadRoute("assetFileDescriptor") { resolver, uri ->
        resolver.openAssetFileDescriptor(uri, "r")?.createInputStream()
    },
    // Ask the provider for a specific format; some serve a different version when asked
    ReadRoute("typed image/jpeg") { resolver, uri ->
        resolver.openTypedAssetFileDescriptor(uri, "image/jpeg", null)?.createInputStream()
    },
    ReadRoute("typed image/*") { resolver, uri ->
        resolver.openTypedAssetFileDescriptor(uri, "image/*", null)?.createInputStream()
    },
)

private class Attempt(val file: File, val declared: Long, val type: String?)

/** Reads [source] through [route] into a temp file, noting the length the source declares. */
private fun copyToTemp(context: Context, resolver: ContentResolver, source: Uri, route: ReadRoute): Attempt {
    val temp = File.createTempFile("picked_cover", null, context.cacheDir)
    try {
        val declared = try {
            resolver.openAssetFileDescriptor(source, "r")?.use { it.length } ?: -1L
        } catch (e: Exception) {
            -1L
        }
        val input = route.open(resolver, source) ?: throw IOException("Couldn't open $source")
        input.use { stream -> temp.outputStream().use { stream.copyTo(it) } }
        Log.d(TAG, "Route ${route.name}: copied ${temp.length()} (declared $declared), head=${temp.readHead(8)}")
        return Attempt(temp, declared, resolver.getType(source))
    } catch (e: Exception) {
        temp.delete()
        throw e
    }
}

private fun File.readHead(count: Int): String =
    inputStream().use { stream ->
        ByteArray(count).let { buffer ->
            val read = stream.read(buffer).coerceAtLeast(0)
            buffer.take(read).joinToString(" ") { "%02x".format(it) }
        }
    }

/** Debug builds keep an image that failed to decode, so it can be inspected (adb pull). */
private fun keepFailedPick(context: Context, picked: File) {
    if (!BuildConfig.DEBUG) return
    try {
        val dir = context.getExternalFilesDir(null) ?: return
        picked.copyTo(File(dir, "failed_pick.bin"), overwrite = true)
        Log.d(TAG, "Kept the failed image as ${dir.absolutePath}/failed_pick.bin")
    } catch (e: Exception) {
        Log.w(TAG, "Couldn't keep the failed image", e)
    }
}

/**
 * A decoded cover. [incomplete] means the file ended early; [missing] is then the fraction of the
 * bottom that is blank. A complete cover has missing = 0.
 */
private class DecodedCover(val bitmap: Bitmap, val missing: Float, val incomplete: Boolean = missing > 0f) {
    val trimmed: Boolean get() = incomplete
}

/** Up to this fraction of a cover may be missing (blank) at the bottom and still be accepted. */
private const val MAX_MISSING_FRACTION = 0.06f

@RequiresApi(Build.VERSION_CODES.P)
private fun decodeWithImageDecoder(file: File): DecodedCover {
    var incomplete = false
    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // the bitmap gets compressed later
        var sample = 1
        while (max(info.size.width, info.size.height) / sample > MAX_COVER_SIDE) sample *= 2
        if (sample > 1) decoder.setTargetSampleSize(sample)

        // Called when the file ends early (e.g. a JPEG missing its last bytes). Keep the partial
        // image; the caller decides whether enough of it is there.
        decoder.setOnPartialImageListener {
            incomplete = true
            true
        }
    }
    return if (incomplete) DecodedCover(bitmap, missingTailFraction(bitmap), incomplete = true)
    else DecodedCover(bitmap, 0f, incomplete = false)
}

/**
 * How much of the bottom of a partly decoded image is the blank fill the decoder leaves where data
 * is missing: the fraction of rows, counted up from the bottom, that are one flat color matching
 * the bottom-left pixel. Only used on images already known to be incomplete.
 */
private fun missingTailFraction(bitmap: Bitmap): Float {
    val width = bitmap.width
    val height = bitmap.height
    val row = IntArray(width)
    bitmap.getPixels(row, 0, width, 0, height - 1, width, 1)
    val fill = row[0]

    var blank = 0
    for (y in height - 1 downTo 0) {
        bitmap.getPixels(row, 0, width, 0, y, width, 1)
        if (row.any { it != fill }) break
        blank++
    }
    return blank.toFloat() / height
}

/** Fallback for Android 8: no rotation handling, but it still refuses truncated JPEGs. */
private fun decodeWithBitmapFactory(file: File): Bitmap {
    val bytes = file.length()
    file.inputStream().use { stream ->
        val head = ByteArray(2).also { stream.read(it) }
        val isJpeg = head[0] == 0xFF.toByte() && head[1] == 0xD8.toByte()
        if (isJpeg && bytes >= 2) {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(bytes - 2)
                if (raf.readUnsignedShort() != 0xFFD9) throw IOException("Truncated JPEG")
            }
        }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_COVER_SIDE) sample *= 2
    return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: throw IOException("Couldn't decode the image")
}

/**
 * Saves a picked image as the cover for [issueId], replacing any existing cover. In debug builds
 * it also replaces the dev server's copy, so a swapped cover isn't undone the next time the
 * phone syncs with the server.
 *
 * @return the message to show the user
 */
@ExperimentalCoroutinesApi
suspend fun pickedCoverToIssue(context: Context, issueId: Int, source: Uri): Int =
    withContext(Dispatchers.IO) {
        try {
            val decoded = decodeCover(context, source)
            val file = getFileHandle(context, coverFileName(issueId))
            val savedUri = decoded.bitmap.saveToInternalStorage(file)
                ?: return@withContext R.string.cover_not_saved

            IssueDatabase.getInstance(context).coverDao().setCover(
                Cover(issue = issueId, coverUri = savedUri, markedDelete = false)
            )

            when {
                BuildConfig.DEBUG && !uploadCoverToServer(context, issueId) ->
                    R.string.cover_saved_here_only
                decoded.trimmed -> R.string.cover_saved_trimmed
                else -> R.string.cover_saved
            }
        } catch (e: IOException) {
            Log.e(TAG, "Picked cover for issue $issueId was unreadable or incomplete", e)
            R.string.cover_incomplete
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't save picked cover for issue $issueId", e)
            R.string.cover_not_saved
        }
    }

/**
 * Removes the cover for [issueId] from this phone and (debug builds) from the dev server. Without
 * the server part the cover would simply be downloaded again.
 *
 * @return the message to show the user
 */
@ExperimentalCoroutinesApi
suspend fun removeCover(context: Context, issueId: Int): Int = withContext(Dispatchers.IO) {
    getFileHandle(context, coverFileName(issueId)).delete()
    IssueDatabase.getInstance(context).coverDao().deleteForIssue(issueId)

    if (BuildConfig.DEBUG && !removeCoverFromServer(issueId)) {
        R.string.cover_removed_here_only
    } else {
        R.string.cover_removed
    }
}

private fun webservice(): Webservice =
    RetrofitAPIClient.getRetrofitClient().create(Webservice::class.java)

/** Replaces the server's cover for [issueId] with the one stored on this phone. */
@ExperimentalCoroutinesApi
private suspend fun uploadCoverToServer(context: Context, issueId: Int): Boolean = try {
    val file = getFileHandle(context, coverFileName(issueId))
    webservice().uploadCover(issueId, file.readBytes().toRequestBody("image/jpeg".toMediaType()))
        .isSuccessful
} catch (e: Exception) {
    Log.e(TAG, "Couldn't upload cover for issue $issueId", e)
    false
}

@ExperimentalCoroutinesApi
private suspend fun removeCoverFromServer(issueId: Int): Boolean = try {
    webservice().removeCover(issueId).isSuccessful
} catch (e: Exception) {
    Log.e(TAG, "Couldn't remove cover for issue $issueId from the server", e)
    false
}

/** How a push of local covers to the server went. [error] is set if it couldn't even start. */
data class CoverPushResult(
    val uploaded: Int = 0,
    val alreadyOnServer: Int = 0,
    val failed: Int = 0,
    val error: String? = null,
) {
    override fun toString(): String = error
        ?: "Uploaded $uploaded cover(s); $alreadyOnServer already on the server; $failed failed"
}

/**
 * Uploads the covers stored on this phone that the server doesn't have yet. The server must be
 * started with ALLOW_COVER_UPLOAD=1; otherwise this reports that uploads are disabled.
 *
 * @param onProgress called with (done, total) as covers are uploaded
 */
@ExperimentalCoroutinesApi
suspend fun pushCoversToServer(
    context: Context,
    onProgress: (Int, Int) -> Unit = { _, _ -> },
): CoverPushResult = withContext(Dispatchers.IO) {
    val webservice = RetrofitAPIClient.getRetrofitClient().create(Webservice::class.java)

    val onServer: Set<Int> = try {
        webservice.getServerCoverIds().toSet()
    } catch (e: Exception) {
        Log.e(TAG, "Couldn't list the server's covers", e)
        return@withContext CoverPushResult(error = "Couldn't reach the server: ${e.message}")
    }

    val local = IssueDatabase.getInstance(context).coverDao().getAll()
        .filter { it.coverUri != null }
    val toUpload = local.filter { it.issue !in onServer }

    var uploaded = 0
    var failed = 0
    toUpload.forEachIndexed { index, cover ->
        onProgress(index, toUpload.size)
        val file = getFileHandle(context, coverFileName(cover.issue))
        if (!file.exists()) {
            failed++
            return@forEachIndexed
        }
        try {
            val response = webservice.uploadCover(
                cover.issue,
                file.readBytes().toRequestBody("image/jpeg".toMediaType())
            )
            when {
                response.isSuccessful -> uploaded++
                response.code() == 403 -> return@withContext CoverPushResult(
                    uploaded = uploaded,
                    failed = failed,
                    error = "The server has cover uploads turned off (start it with ALLOW_COVER_UPLOAD=1)"
                )
                else -> failed++
            }
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't upload cover for issue ${cover.issue}", e)
            failed++
        }
    }
    onProgress(toUpload.size, toUpload.size)

    CoverPushResult(
        uploaded = uploaded,
        alreadyOnServer = local.size - toUpload.size,
        failed = failed
    )
}
