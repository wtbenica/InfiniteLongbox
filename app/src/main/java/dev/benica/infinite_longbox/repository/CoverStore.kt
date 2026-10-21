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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
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
import java.io.RandomAccessFile
import kotlin.math.max

private const val TAG = APP + "CoverStore"

/** Covers are scaled down to this on their longest side when picked; plenty for a phone. */
private const val MAX_COVER_SIDE = 2400

/**
 * Decodes an image the user picked. Never returns a partly decoded picture.
 *
 * The picked image is copied to a temp file first. Reading straight from the picker's stream is
 * unreliable (a streamed source such as Google Photos can hand back incomplete data, and Android
 * then returns a bitmap with the unread rows black). The copy's length is checked against what the
 * source declares, and ImageDecoder is used because it fails on incomplete input instead of
 * returning half an image, and applies the photo's rotation.
 *
 * @throws java.io.IOException if the image is unreadable or incomplete
 */
private fun decodeCover(context: Context, source: Uri): DecodedCover {
    val temp = File.createTempFile("picked_cover", null, context.cacheDir)
    try {
        val resolver = context.contentResolver
        val declaredLength = resolver.openAssetFileDescriptor(source, "r")?.use { it.length } ?: -1L
        val input = resolver.openInputStream(source) ?: throw IOException("Couldn't open $source")
        input.use { stream -> temp.outputStream().use { stream.copyTo(it) } }

        Log.d(
            TAG,
            "Picked image: type=${resolver.getType(source)} declared=$declaredLength " +
                    "copied=${temp.length()} head=${temp.readHead(16)}"
        )

        if (declaredLength > 0 && temp.length() != declaredLength) {
            throw IOException("Incomplete image: got ${temp.length()} of $declaredLength bytes")
        }

        return try {
            decodeWithImageDecoder(temp)
        } catch (e: Exception) {
            keepFailedPick(context, temp)
            throw e
        }
    } finally {
        temp.delete()
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

/** A decoded cover. [trimmed] means the image file was cut off a little at the bottom. */
private class DecodedCover(val bitmap: Bitmap, val trimmed: Boolean)

/** Up to this fraction of a cover may be missing (blank) at the bottom and still be accepted. */
private const val MAX_MISSING_FRACTION = 0.06f

private fun decodeWithImageDecoder(file: File): DecodedCover {
    var incomplete = false
    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // the bitmap gets compressed later
        var sample = 1
        while (max(info.size.width, info.size.height) / sample > MAX_COVER_SIDE) sample *= 2
        if (sample > 1) decoder.setTargetSampleSize(sample)

        // The decoder calls this when the file ends early (e.g. a JPEG missing its last bytes, which
        // viewers happily show). Keep the partial image and judge below how much is missing.
        decoder.setOnPartialImageListener {
            incomplete = true
            true
        }
    }

    if (incomplete) {
        val missing = missingTailFraction(bitmap)
        Log.d(TAG, "Image file was incomplete; about ${(missing * 100).toInt()}% of it is blank")
        if (missing > MAX_MISSING_FRACTION) {
            throw IOException("Image is cut off: ${(missing * 100).toInt()}% of it is missing")
        }
    }
    return DecodedCover(bitmap, trimmed = incomplete)
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
