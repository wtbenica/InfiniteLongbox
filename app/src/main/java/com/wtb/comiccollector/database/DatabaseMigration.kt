package com.wtb.comiccollector.database

import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class DatabaseMigration {
    val existing = IssueDatabase.getInstance(context!!)
    val incoming = IncomingIssueDatabase.getInstance(context!!)


    init {

        incoming.close()
    }
}