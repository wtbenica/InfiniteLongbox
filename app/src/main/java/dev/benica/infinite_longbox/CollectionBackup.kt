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

package dev.benica.infinite_longbox

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException

/** One issue in a collection. The series and number are there for people reading the file. */
data class BackupEntry(
    val issueId: Int,
    val seriesId: Int,
    val series: String,
    val number: String,
)

/** A collection (My Collection or the Wish List) and what is in it. */
data class BackupCollection(
    val id: Int,
    val name: String,
    val issues: List<BackupEntry>,
)

/** What an import did: how many issues went in, were already there, or could not be found. */
data class ImportResult(
    val added: Int,
    val alreadyThere: Int,
    val unavailable: List<BackupEntry>,
) {
    /** One line for the person who ran the import. */
    fun summary(): String = buildString {
        append("Added $added")
        if (alreadyThere > 0) append(", $alreadyThere already there")
        if (unavailable.isNotEmpty()) {
            append(". ${unavailable.size} could not be found on the server")
            val shown = unavailable.take(3).joinToString { "${it.series} #${it.number}" }
            append(" ($shown${if (unavailable.size > 3) ", …" else ""})")
        }
    }
}

/**
 * My Collection and the Wish List as a small JSON file, so they can be kept somewhere and put
 * back later, on this phone after its data is cleared or on another one. The file lists issues
 * by their GCD id; importing fetches any issue the phone does not have yet.
 */
object CollectionBackup {
    private const val APP = "InfiniteLongbox"
    const val FORMAT = 1

    /** Collections that exist: My Collection and the Wish List. */
    private val KNOWN_COLLECTIONS = setOf(1, 2)

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private data class Backup(
        val app: String?,
        val format: Int?,
        val exportedAt: String?,
        val collections: List<BackupCollection>?,
    )

    fun toJson(collections: List<BackupCollection>, exportedAt: String): String =
        gson.toJson(Backup(APP, FORMAT, exportedAt, collections))

    /**
     * The collections in [json], or why it is not a backup this version can read. Entries
     * with no issue id are skipped, and so is a collection that does not exist.
     */
    fun parse(json: String): Parsed {
        val backup = try {
            gson.fromJson(json, Backup::class.java)
        } catch (e: JsonSyntaxException) {
            return Parsed.Unreadable("This is not a collection file")
        }
        if (backup?.app != APP) return Parsed.Unreadable("This is not a collection file")
        val format = backup.format ?: return Parsed.Unreadable("This is not a collection file")
        if (format > FORMAT) {
            return Parsed.Unreadable("This file is from a newer version of the app")
        }
        val collections = backup.collections.orEmpty()
            .filter { it.id in KNOWN_COLLECTIONS }
            .map { collection ->
                collection.copy(issues = collection.issues.orEmpty().filter { it.issueId > 0 })
            }
        return Parsed.Ok(collections)
    }

    sealed class Parsed {
        data class Ok(val collections: List<BackupCollection>) : Parsed()
        data class Unreadable(val reason: String) : Parsed()
    }
}
