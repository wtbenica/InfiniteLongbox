package com.wtb.comiccollector.repository

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.Cover
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

private const val TAG = APP + "IssueCoverUpdater"

class UpdateIssueCover(val database: IssueDatabase, val context: Context) {
    internal fun update(issueId: Int) {
        Log.d(TAG, "CoverUpdater_____________________________")
        CoroutineScope(Dispatchers.IO).launch {
            val issueDef =
                CoroutineScope(Dispatchers.IO).async {
                    database.issueDao().getIssueSus(issueId)
                }
            Log.d(TAG, "MADE IT THIS FAR!")
            issueDef.await()?.let { issue ->
                if (issue.coverUri == null) {
                    Log.d(TAG, "COVER UPDATER needsCover... starting")
                    CoroutineScope(Dispatchers.IO).launch {
                        kotlin.runCatching {
                            Log.d(TAG, "COVER UPDATER Starting connection.....")
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
                                    val bitmap = image.await().also {
                                        Log.d(TAG, "COVER UPDATER Got an image!")
                                    }

                                    bitmap?.apply {
                                        val savedUri: Uri? =
                                            saveToInternalStorage(
                                                context,
                                                issue.issue.coverFileName
                                            )

                                        Log.d(TAG, "COVER UPDATER Saving image $savedUri")
                                        val cover =
                                            Cover(issueId = issueId, coverUri = savedUri)
                                        database.coverDao().upsertSus(listOf(cover))
                                    }
                                }
                            } else {
                                Log.d(TAG, "COVER UPDATER No Cover Found")
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
}