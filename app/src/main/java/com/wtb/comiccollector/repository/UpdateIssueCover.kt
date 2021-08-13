package com.wtb.comiccollector.repository

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.models.Cover
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

private const val TAG = APP + "UpdateIssueCover"

@ExperimentalCoroutinesApi
class UpdateIssueCover(
    webservice: Webservice,
    prefs: SharedPreferences,
    val context: Context,
) : Updater(webservice, prefs) {
    internal fun update(issueId: Int) {
        if (Companion.checkIfStale(ISSUE_TAG(issueId), ISSUE_LIFETIME, prefs)) {
            CoroutineScope(Dispatchers.IO).launch {
                database.issueDao().getIssueSus(issueId)?.let { issue ->
                    if (issue.coverUri == null) {
                        kotlin.runCatching {
                            val doc = Jsoup.connect(issue.issue.url).get()

                            val noCover = doc.getElementsByClass("no_cover").size == 1

                            val coverImgElements = doc.getElementsByClass("cover_img")
                            val wraparoundElements =
                                doc.getElementsByClass("wraparound_cover_img")

                            val elements = when {
                                coverImgElements.size > 0   -> coverImgElements
                                wraparoundElements.size > 0 -> wraparoundElements
                                else                        -> null
                            }

                            val src = elements?.get(0)?.attr("src")

                            val url = src?.let { URL(it) }

                            if (!noCover && url != null) {
                                val image = CoroutineScope(Dispatchers.IO).async {
                                    url.toBitmap()
                                }

                                CoroutineScope(Dispatchers.Default).launch {
                                    val bitmap = image.await()

                                    bitmap?.let {
                                        val savedUri: Uri? =
                                            it.saveToInternalStorage(
                                                context,
                                                issue.issue.coverFileName
                                            )

                                        val cover =
                                            Cover(issue = issueId, coverUri = savedUri)
                                        database.coverDao().upsertSus(listOf(cover))
                                    }
                                }
                            } else if (noCover) {
                                val cover = Cover(issue = issueId, coverUri = null)
                                database.coverDao().upsertSus(cover)
                            } else {
                                Log.d(TAG, "COVER UPDATER No Cover Found")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun URL.toBitmap(): Bitmap? {
    return try {
        BitmapFactory.decodeStream(openStream())
    } catch (e: IOException) {
        null
    }
}

fun Bitmap.saveToInternalStorage(context: Context, uri: String): Uri? {
    val wrapper = ContextWrapper(context)

    var file = wrapper.getDir("images", Context.MODE_PRIVATE)

    file = File(file, uri)

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