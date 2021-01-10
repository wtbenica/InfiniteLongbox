package com.wtb.comiccollector.database

import android.net.Uri
import androidx.room.TypeConverter
import java.util.*

class IssueTypeConverters {
    @TypeConverter
    fun toUUID(uuid: String?): UUID? {
        return UUID.fromString(uuid)
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
        val toString = uri.toString()
        return toString
    }
}