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
import android.graphics.ImageDecoder
import android.net.Uri
import dev.benica.infinite_longbox.InfiniteLongboxApplication
import dev.benica.infinite_longbox.database.models.BaseCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/** The longest side a kept photo has: more pixels show nothing on a phone. */
private const val MAX_SIDE = 2400
private const val QUALITY = 90
private val MY_COLLECTION = BaseCollection.MY_COLL.id

/** The file name of a photo of the copy of [issueId] in [collectionId], taken at [takenAt] (ms). */
internal fun copyPhotoName(issueId: Int, collectionId: Int, takenAt: Long) = "${issueId}_${collectionId}_$takenAt.jpg"

/** Whether [name] is a photo of the copy of [issueId] in [collectionId]. */
internal fun isCopyPhoto(name: String, issueId: Int, collectionId: Int) = name.startsWith("${issueId}_${collectionId}_") && name.endsWith(".jpg")

/** How much to shrink an image of [width] x [height] by, in whole steps, to reach [MAX_SIDE] or less. */
internal fun photoSampleSize(width: Int, height: Int) = max(1, max(width, height) / MAX_SIDE)

/**
 * Photos of the copies in My Collection: the actual comic, not its cover. They are files named for
 * the issue and collection, so a copy removed and added again finds its photos again.
 */
object CopyPhotos {
    private val changes = MutableStateFlow(0)

    private fun dir(context: Context) = File(context.filesDir, "copy_photos").apply { mkdirs() }

    private fun context(): Context = InfiniteLongboxApplication.context!!.applicationContext

    private fun list(issueId: Int, collectionId: Int): List<File> =
        dir(context()).listFiles { _, name -> isCopyPhoto(name, issueId, collectionId) }
            ?.sortedBy { it.name }.orEmpty()

    /** The photos of the copy of [issueId] in My Collection, oldest first, kept up to date. */
    fun photos(issueId: Int): Flow<List<File>> =
        changes.map { list(issueId, MY_COLLECTION) }.flowOn(Dispatchers.IO)

    /** Keeps the picture at [source] as a photo of the copy of [issueId]; false if it can't be read. */
    suspend fun add(issueId: Int, source: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val context = context()
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, source)) { decoder, info, _ ->
                decoder.setTargetSampleSize(photoSampleSize(info.size.width, info.size.height))
            }
            val target = File(dir(context), copyPhotoName(issueId, MY_COLLECTION, System.currentTimeMillis()))
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
            bitmap.recycle()
            changes.value++
            true
        } catch (e: Exception) {
            false
        }
    }

    fun remove(photo: File) {
        photo.delete()
        changes.value++
    }
}

