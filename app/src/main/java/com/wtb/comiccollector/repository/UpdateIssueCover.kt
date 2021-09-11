package com.wtb.comiccollector.repository

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.models.Cover
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate


private const val TAG = APP + "UpdateIssueCover"

fun coverFileName(issueId: Int): String = "COVER_$issueId.jpg"

@ExperimentalCoroutinesApi
class UpdateIssueCover private constructor(
    webservice: Webservice,
    prefs: SharedPreferences,
    val context: Context,
) : Updater(webservice, prefs) {
    internal fun update(issueId: Int) {
        Log.d(TAG, "Starting cover update $issueId")
        CoroutineScope(Dispatchers.IO).launch {
            val file = getFileHandle(context, coverFileName(issueId))
            val fileDNE = !file.exists()
            Log.d(TAG, "UPDATING COVER $issueId -- FILE_DNE: $fileDNE")
            database.issueDao().getIssueSus(issueId)?.let { issue ->
                val uriDNE = issue.coverUri == null &&
                        issue.cover?.lastUpdated?.plusDays(7) ?: LocalDate.MIN < LocalDate.now()
                Log.d(TAG,
                      "$issueId ${issue.series.seriesName} ${issue.issue.issueNumRaw} FILE: $fileDNE URI: $uriDNE")
                if (fileDNE || uriDNE || DEBUG) {
                    if (fileDNE) {
                        kotlin.runCatching {
                            val url = URL(issue.issue.url)
                            val image = url.toBitmap()
                            if (image != null) {
                                val savedUri: Uri? = image.saveToInternalStorage(file)

                                val cover = Cover(issue = issueId, coverUri = savedUri)
                                database.coverDao().upsertSus(listOf(cover))
                            } else {
                                val cover = Cover(issue = issueId, coverUri = null)
                                database.coverDao().upsertSus(cover)
                            }
                        }
                    } else {
                        val cover =
                            Cover(issue = issueId, coverUri = Uri.parse(file.absolutePath))
                        database.coverDao().upsertSus(listOf(cover))
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

    private fun checkForExistingCover(file: File): Boolean =
        file.exists()
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

private fun getFileHandle(context: Context, uri: String): File {
    val wrapper = ContextWrapper(context)

    var file: File = wrapper.getDir("images", Context.MODE_PRIVATE)

    file = File(file, uri)
    return file
}

@Suppress("DEPRECATION")
fun Bitmap.saveToInternalStorage2(context: Context, filename: String): Uri? {
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
    Log.d(TAG, "SAVED URI: $uri")
    return uri
}