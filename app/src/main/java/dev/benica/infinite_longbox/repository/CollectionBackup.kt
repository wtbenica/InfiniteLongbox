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

import dev.benica.infinite_longbox.APP
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException

/** A creator who signed a copy; the name is there for people reading the file. */
data class BackupSigner(val creatorId: Int, val name: String)

/** One issue in a collection. The series and number are there for people reading the file. */
data class BackupEntry(
    val issueId: Int,
    val seriesId: Int,
    val series: String,
    val number: String,
    val signedBy: List<BackupSigner> = emptyList(),
    val storageLocation: String? = null,
    val condition: String? = null,
    val notes: String? = null,
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

/** My Collection and the Wish List as a small JSON file. */
object CollectionBackup {
    private const val APP = "InfiniteLongbox"
    const val FORMAT = 1

    /** Collections that exist: My Collection and the Wish List. */
    private val KNOWN_COLLECTIONS = setOf(1, 2)

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /** What goes into the file. */
    private data class Written(
        val app: String,
        val format: Int,
        val exportedAt: String,
        val collections: List<BackupCollection>,
    )

    // Gson fills these in and knows nothing of null rules, so every field is nullable; [parse] makes them non-null
    private data class Read(
        val app: String?,
        val format: Int?,
        val collections: List<ReadCollection>?,
    )

    private data class ReadCollection(
        val id: Int?,
        val name: String?,
        val issues: List<ReadEntry>?,
    )

    private data class ReadSigner(val creatorId: Int?, val name: String?)

    private data class ReadEntry(
        val issueId: Int?,
        val seriesId: Int?,
        val series: String?,
        val number: String?,
        val signedBy: List<ReadSigner>?,
        val storageLocation: String?,
        val condition: String?,
        val notes: String?,
    )

    fun toJson(collections: List<BackupCollection>, exportedAt: String): String =
        gson.toJson(Written(APP, FORMAT, exportedAt, collections))

    /** The collections in [json], or why it isn't a backup this version can read. */
    fun parse(json: String): Parsed {
        val backup = try {
            gson.fromJson(json, Read::class.java)
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
                BackupCollection(
                    id = collection.id ?: 0,
                    name = collection.name.orEmpty(),
                    issues = collection.issues.orEmpty()
                        .filter { (it.issueId ?: 0) > 0 }
                        .map {
                            BackupEntry(
                                issueId = it.issueId ?: 0,
                                seriesId = it.seriesId ?: 0,
                                series = it.series.orEmpty(),
                                number = it.number.orEmpty(),
                                signedBy = it.signedBy.orEmpty()
                                    .filter { signer -> (signer.creatorId ?: 0) > 0 }
                                    .map { signer -> BackupSigner(signer.creatorId ?: 0, signer.name.orEmpty()) },
                                storageLocation = it.storageLocation?.takeIf { text -> text.isNotBlank() },
                                condition = it.condition?.takeIf { text -> text.isNotBlank() },
                                notes = it.notes?.takeIf { text -> text.isNotBlank() },
                            )
                        },
                )
            }
        return Parsed.Ok(collections)
    }

    sealed class Parsed {
        data class Ok(val collections: List<BackupCollection>) : Parsed()
        data class Unreadable(val reason: String) : Parsed()
    }
}
