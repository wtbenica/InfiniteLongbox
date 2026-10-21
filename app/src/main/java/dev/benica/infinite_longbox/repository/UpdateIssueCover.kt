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
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.Webservice
import dev.benica.infinite_longbox.database.models.Cover
import dev.benica.infinite_longbox.network.RetrofitAPIClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap


private const val TAG = APP + "UpdateIssueCover"

fun coverFileName(issueId: Int): String = "COVER_$issueId.jpg"

/** What asking the server for a cover came to. */
private sealed class CoverFetch {
    /** The image was saved to the file. */
    object Saved : CoverFetch()

    /** The server has not changed the image since the phone's copy. */
    object NotModified : CoverFetch()

    /** The server has no cover for the issue. */
    object None : CoverFetch()

    /** The server could not be asked (offline, an error): nothing is known. */
    object Failed : CoverFetch()
}

@ExperimentalCoroutinesApi
class UpdateIssueCover private constructor(
    webservice: Webservice,
    prefs: SharedPreferences,
    val context: Context,
) : Updater(webservice, prefs) {
    private val inFlight = ConcurrentHashMap.newKeySet<Int>()

    /**
     * Brings an issue's cover up to date (see [CoverPlan]). Covers are looked at each time a card
     * binds, so an issue already being worked on is left to the one doing it.
     */
    internal fun update(issueId: Int, markedDelete: Boolean = true): Job {
        if (!inFlight.add(issueId)) return Job().apply { complete() }
        return CoroutineScope(Dispatchers.IO).launch {
            try {
                refresh(issueId, markedDelete)
            } finally {
                inFlight.remove(issueId)
            }
        }
    }

    private suspend fun refresh(issueId: Int, markedDelete: Boolean) {
        val file = getFileHandle(context, coverFileName(issueId))
        val issue = database.issueDao().getIssueSus(issueId) ?: return
        val row = issue.cover?.let { CoverRow(it.coverUri != null, it.lastUpdated) }
        val marked = markedDelete && issue.collectionItems.isEmpty()
        val uri = Uri.parse(file.absolutePath)

        when (CoverPlan.decide(file.exists(), row, LocalDate.now())) {
            CoverPlan.NOTHING -> Unit
            CoverPlan.LINK -> database.coverDao().setCover(Cover(issue = issueId, coverUri = uri, markedDelete = marked))
            CoverPlan.DOWNLOAD, CoverPlan.RECHECK -> {
                // A row pointing at a missing file changes when the file arrives, so the card redraws
                if (row?.hasImage == true && !file.exists()) {
                    database.coverDao().setCover(Cover(issue = issueId, coverUri = null, markedDelete = marked))
                }
                val since = file.takeIf { it.exists() }?.lastModified()
                when (LoadTracker.track { fetchCover(issue.issue.url, file, since) }) {
                    CoverFetch.Saved, CoverFetch.NotModified ->
                        database.coverDao().setCover(Cover(issue = issueId, coverUri = uri, markedDelete = marked))
                    CoverFetch.None -> {
                        file.delete()
                        database.coverDao().setCover(Cover(issue = issueId, coverUri = null, markedDelete = marked))
                    }
                    CoverFetch.Failed -> Unit
                }
            }
        }
    }

    /**
     * Asks for the cover once and keeps the bytes as they came, so the image is not decoded and
     * compressed again. With [since] (the time of the copy on the phone) the server sends nothing
     * if its image has not changed.
     */
    private fun fetchCover(url: String, file: File, since: Long?): CoverFetch {
        val request = Request.Builder().url(url).apply {
            if (since != null && since > 0) header("If-Modified-Since", httpDate(since))
        }.build()
        return try {
            RetrofitAPIClient.httpClient.newCall(request).execute().use { response ->
                when {
                    response.code == 304 -> CoverFetch.NotModified
                    response.code == 404 -> CoverFetch.None
                    !response.isSuccessful -> CoverFetch.Failed
                    else -> {
                        val body = response.body ?: return@use CoverFetch.Failed
                        val part = File(file.parentFile, file.name + ".part")
                        body.byteStream().use { input ->
                            FileOutputStream(part).use { input.copyTo(it) }
                        }
                        if (!isImage(part)) {
                            part.delete()
                            CoverFetch.Failed
                        } else {
                            response.headers.getDate("Last-Modified")?.let { part.setLastModified(it.time) }
                            if (part.renameTo(file)) CoverFetch.Saved else CoverFetch.Failed
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.d(TAG, "cover $url: $e")
            CoverFetch.Failed
        }
    }

    private fun isImage(file: File): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    private fun httpDate(millis: Long): String =
        DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC))

    companion object {
        private var INSTANCE: UpdateIssueCover? = null

        fun get(): UpdateIssueCover {
            return INSTANCE
                ?: throw IllegalStateException("UpdateIssueCover must be initialized")
        }

        fun initialize(
            webservice: Webservice,
            prefs: SharedPreferences,
            context: Context,
        ) {
            if (INSTANCE == null) {
                INSTANCE = UpdateIssueCover(webservice, prefs, context)
            }
        }
    }

}

fun Bitmap.saveToInternalStorage(file: File): Uri? {
    return try {
        val stream = FileOutputStream(file)

        compress(Bitmap.CompressFormat.JPEG, 100, stream)

        stream.flush()

        stream.close()

        Uri.parse(file.absolutePath)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

fun getFileHandle(context: Context, uri: String): File {
    val wrapper = ContextWrapper(context)

    var file: File = wrapper.getDir("images", Context.MODE_PRIVATE)

    file = File(file, uri)
    return file
}

