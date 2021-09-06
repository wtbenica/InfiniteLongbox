package com.wtb.comiccollector.repository

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.models.Cover
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


private const val TAG = APP + "UpdateIssueCover"

@ExperimentalCoroutinesApi
class UpdateIssueCover(
    webservice: Webservice,
    prefs: SharedPreferences,
    val context: Context,
) : Updater(webservice, prefs) {
    internal fun update(issueId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            database.issueDao().getIssueSus(issueId)?.let { issue ->
                if (issue.coverUri == null || DEBUG) {
                    kotlin.runCatching {
                        val url = URL(issue.issue.url)

                        val image = CoroutineScope(Dispatchers.IO).async {
                            url.toBitmap()
                        }

                        CoroutineScope(Dispatchers.Default).launch {
                            val bitmap = image.await()

                            if (bitmap != null) {
                                val savedUri: Uri? =
                                    bitmap.saveToInternalStorage(
                                        context,
                                        issue.issue.coverFileName
                                    )

                                val cover = Cover(issue = issueId, coverUri = savedUri)
                                database.coverDao().upsertSus(listOf(cover))
                            } else {
                                val cover = Cover(issue = issueId, coverUri = null)
                                database.coverDao().upsertSus(cover)
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
        val connection: HttpURLConnection = this.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect()
        return if (connection.responseCode == 200)
            BitmapFactory.decodeStream(this.openStream())
        else
            null
    } catch (e: IOException) {
        null
    }
}

fun Bitmap.saveToInternalStorage(context: Context, uri: String): Uri? {
    val wrapper = ContextWrapper(context)

    var file: File = wrapper.getDir("images", Context.MODE_PRIVATE)

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