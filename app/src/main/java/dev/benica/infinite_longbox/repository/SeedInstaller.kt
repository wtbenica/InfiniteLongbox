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

import android.content.SharedPreferences
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.Webservice
import dev.benica.infinite_longbox.database.IssueDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.zip.GZIPInputStream

private const val TAG = APP + "SeedInstaller"

/**
 * Describes the starter database served by the backend (see tools/build_seed.py there).
 */
data class SeedMeta(
    val version: String,
    val builtOn: String,
    val dbVersion: Int,
    val identityHash: String,
    val dbBytes: Long,
    val gzBytes: Long,
    /** Static-sync progress markers (page-complete strings and update dates) at build time. */
    val prefs: Map<String, String>,
)

/**
 * Installs a pre-synced copy of the static data (publishers, series, creators, characters,
 * series bonds) on first run, so the paged static sync only has to catch up instead of making
 * thousands of requests.
 *
 * It only runs on an empty database and only if the seed was built from the same Room schema.
 * Anything unexpected (no seed on the server, no network, schema mismatch, a failed import)
 * returns false and leaves the database untouched, and the normal sync runs as before.
 */
@ExperimentalCoroutinesApi
class SeedInstaller(
    private val webservice: Webservice,
    private val prefs: SharedPreferences,
    private val database: IssueDatabase,
    private val cacheDir: File,
) {
    /**
     * @param onStart called once a seed is going to be installed
     * @param onProgress download progress as (bytes so far, total bytes)
     * @return true if a seed was installed
     */
    suspend fun installIfNeeded(
        onStart: () -> Unit = {},
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isFresh()) return@withContext false

        val meta = try {
            webservice.getSeedMeta()
        } catch (e: Exception) {
            Log.d(TAG, "No seed available: ${e.message}")
            return@withContext false
        }

        if (meta.identityHash != roomIdentityHash()) {
            Log.w(TAG, "Seed was built from a different schema; skipping")
            return@withContext false
        }

        val gz = File(cacheDir, "seed.db.gz")
        val seedDb = File(cacheDir, "seed.db")
        try {
            onStart()
            download(gz, meta, onProgress)
            GZIPInputStream(gz.inputStream().buffered()).use { input ->
                FileOutputStream(seedDb).use { output -> input.copyTo(output) }
            }
            importSeed(seedDb)
            restoreMarkers(meta)
            Log.d(TAG, "Installed seed ${meta.version}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Seed install failed; falling back to normal sync", e)
            false
        } finally {
            gz.delete()
            seedDb.delete()
        }
    }

    private fun isFresh(): Boolean = database.openHelper.readableDatabase
        .query(SimpleSQLiteQuery("SELECT COUNT(*) FROM Series WHERE seriesId != ?", arrayOf(DUMMY_ID)))
        .use { it.moveToFirst() && it.getInt(0) == 0 }

    private fun roomIdentityHash(): String? = database.openHelper.readableDatabase
        .query(SimpleSQLiteQuery("SELECT identity_hash FROM room_master_table WHERE id = 42"))
        .use { if (it.moveToFirst()) it.getString(0) else null }

    private suspend fun download(target: File, meta: SeedMeta, onProgress: (Long, Long) -> Unit) {
        val body = webservice.getSeedDb()
        val total = body.contentLength().takeIf { it > 0 } ?: meta.gzBytes
        body.byteStream().use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(64 * 1024)
                var done = 0L
                var lastReported = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    done += read
                    if (done - lastReported >= 256 * 1024) {
                        lastReported = done
                        onProgress(done, total)
                    }
                }
                onProgress(done, total)
            }
        }
    }

    /**
     * Copies every table from the seed into the live database in one transaction. Rows that
     * already exist are left alone.
     */
    private fun importSeed(seedDb: File) {
        val db = database.openHelper.writableDatabase
        db.execSQL("ATTACH DATABASE '${seedDb.absolutePath.replace("'", "''")}' AS seed")
        try {
            val tables = mutableListOf<String>()
            db.query(
                SimpleSQLiteQuery(
                    "SELECT name FROM seed.sqlite_master WHERE type = 'table' AND name NOT LIKE " +
                            "'sqlite_%' AND name NOT IN ('android_metadata', 'room_master_table')"
                )
            ).use { while (it.moveToNext()) tables.add(it.getString(0)) }

            db.beginTransaction()
            try {
                // Foreign keys are checked at commit, so table order doesn't matter
                db.execSQL("PRAGMA defer_foreign_keys = ON")
                tables.forEach { db.execSQL("INSERT OR IGNORE INTO main.`$it` SELECT * FROM seed.`$it`") }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            db.execSQL("DETACH DATABASE seed")
        }
    }

    /**
     * The sync-progress markers say which pages are already downloaded. Update dates are set to
     * today: the data is as current as the server's was at seed time, and a stale date would
     * make the app throw the markers away and re-download everything.
     */
    private fun restoreMarkers(meta: SeedMeta) {
        val today = LocalDate.now().toString()
        val dateFormat = Regex("""\d{4}-\d{2}-\d{2}""")
        prefs.edit().apply {
            meta.prefs.forEach { (key, value) ->
                putString(key, if (dateFormat.matches(value)) today else value)
            }
        }.commit()
    }
}
