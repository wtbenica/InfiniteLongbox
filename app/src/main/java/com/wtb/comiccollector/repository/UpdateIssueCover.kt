package com.wtb.comiccollector.repository

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.models.Cover
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL


private const val TAG = APP + "UpdateIssueCover"

@ExperimentalCoroutinesApi
class UpdateIssueCover private constructor(
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

@Suppress("DEPRECATION")
fun Bitmap.saveToInternalStorage(context: Context, filename: String): Uri? {
    val mimeType = "images/jpeg"
    val directory = Environment.DIRECTORY_PICTURES
    val mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val imageOutputStream: OutputStream?
    val uri: Uri?

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, directory)
        }

        context.contentResolver.run {
            uri = context.contentResolver.insert(mediaUri, values)
            imageOutputStream = uri?.let { openOutputStream(it) }
        }
    } else {
        val imagePath = Environment.getExternalStoragePublicDirectory(directory).absolutePath
        val image = File(imagePath, filename)
        imageOutputStream = FileOutputStream(image)
        uri = Uri.parse(image.absolutePath)
    }

    imageOutputStream.use { compress(Bitmap.CompressFormat.JPEG, 100, it) }

    return uri
}