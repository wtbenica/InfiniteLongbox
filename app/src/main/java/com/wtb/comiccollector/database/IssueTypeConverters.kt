package com.wtb.comiccollector.database

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
        return if (uri == "null") {
            null
        } else {
            Uri.parse(uri)
        }
    }

    @TypeConverter
    fun fromUri(uri: Uri?): String {
        return uri.toString()
    }

    @TypeConverter
    fun toLocalDate(date: String): LocalDate? {
        return if (date == "null") {
            null
        } else {
            LocalDate.parse(date)
        }
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?) : String {
        return date.toString()
    }
}

@TypeConverter
fun fromLocalDate(date: LocalDate?): String {
    return date.toString()
}
