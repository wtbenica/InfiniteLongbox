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

package dev.benica.infinite_longbox.database

import android.net.Uri
import androidx.room.TypeConverter
import java.time.LocalDate
import java.util.*

class IssueTypeConverters {
    @TypeConverter
    fun toUUID(uuid: String?): UUID? {
        return if (uuid == null) {
            null
        } else {
            UUID.fromString(uuid)
        }
    }

    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }

    @TypeConverter
    fun toUri(uri: String?): Uri? {
        return if (uri == null || uri == "null") {
            null
        } else {
            Uri.parse(uri)
        }
    }

    @TypeConverter
    fun fromUri(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun toLocalDate(date: String?): LocalDate? {
        return if (date == null || date == "null") {
            null
        } else {
            LocalDate.parse(date)
        }
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toBoolean(num: Number): Boolean {
        return num == 1
    }

    @TypeConverter
    fun fromBoolean(boolean: Boolean): Number = if (boolean) {
        1
    } else {
        0
    }
}