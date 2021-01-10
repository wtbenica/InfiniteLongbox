package com.wtb.comiccollector

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Issue(
    @PrimaryKey
    val issueId: UUID = UUID.randomUUID(),
    var series: String = "New Issue",
    var volume: Int = 1,
    var issueNum: Int = 1,
    var writer: String = "",
    var penciller: String = "",
    var inker: String = "",
    var coverUri: Uri? = null
)