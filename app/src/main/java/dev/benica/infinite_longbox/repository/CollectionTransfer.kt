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

import android.net.Uri
import android.util.Log
import dev.benica.infinite_longbox.Notices
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import dev.benica.infinite_longbox.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.LocalDate

private const val TAG = "CC_CollectionTransfer"

/** Saves My Collection and the Wish List to a file the person picks, and reads one back. Create it before the activity starts. */
class CollectionTransfer(private val activity: ComponentActivity) {
    private val exportCollection =
        activity.registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { writeCollection(it) }
        }

    private val importCollection =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { readCollection(it) }
        }

    private fun collectionFileName() = "infinite-longbox-collection-${LocalDate.now()}.json"

    /** Saves My Collection and the Wish List to the file the person picked. */
    private fun writeCollection(uri: Uri) {
        activity.lifecycleScope.launch {
            val saved = try {
                val json = Repository.get().exportCollections(Instant.now().toString())
                withContext(Dispatchers.IO) {
                    activity.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
                        ?: throw IOException("could not open $uri")
                }
                true
            } catch (e: IOException) {
                Log.e(TAG, "export collection", e)
                false
            }
            Notices.show(if (saved) R.string.collection_export_done else R.string.collection_export_failed, long = true)
        }
    }

    /** Puts what is in the file the person picked into My Collection and the Wish List. */
    private fun readCollection(uri: Uri) {
        activity.lifecycleScope.launch {
            val text = try {
                withContext(Dispatchers.IO) {
                    activity.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                }
            } catch (e: IOException) {
                Log.e(TAG, "read collection", e)
                null
            }
            val parsed = text?.let { CollectionBackup.parse(it) }
            if (parsed !is CollectionBackup.Parsed.Ok) {
                val reason = (parsed as? CollectionBackup.Parsed.Unreadable)?.reason
                    ?: activity.getString(R.string.collection_import_failed)
                Notices.show(reason, long = true)
                return@launch
            }
            Notices.show(R.string.collection_import_working)
            val result = Repository.get().importCollections(parsed.collections)
            Notices.show(result.summary(), long = true)
        }
    }


    fun export() = exportCollection.launch(collectionFileName())

    fun import() = importCollection.launch(arrayOf("application/json", "text/plain", "*/*"))
}
