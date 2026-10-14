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
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.Webservice
import dev.benica.infinite_longbox.database.IssueDatabase
import dev.benica.infinite_longbox.database.models.Cover
import dev.benica.infinite_longbox.network.RetrofitAPIClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.math.max

private const val TAG = APP + "CoverStore"

/** Covers are scaled down to this on their longest side when picked; plenty for a phone. */
private const val MAX_COVER_SIDE = 2400

/**
 * Saves a cover the user picked (from their phone's gallery or downloads) as the cover for
 * [issueId]. The image is stored with the app, like a downloaded cover would be, and it is never
 * marked for cleanup. Replaces any existing cover.
 *
 * @return true if the image could be read and saved
 */
@ExperimentalCoroutinesApi
suspend fun saveCoverFromUri(context: Context, issueId: Int, source: Uri): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext false

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_COVER_SIDE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = resolver.openInputStream(source)
                ?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@withContext false

            val file = getFileHandle(context, coverFileName(issueId))
            val savedUri = bitmap.saveToInternalStorage(file) ?: return@withContext false

            IssueDatabase.getInstance(context).coverDao().setCover(
                Cover(issue = issueId, coverUri = savedUri, markedDelete = false)
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't save picked cover for issue $issueId", e)
            false
        }
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
