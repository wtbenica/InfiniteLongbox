package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IssueUpdater(val webservice: Webservice, val database: IssueDatabase, val prefs: SharedPreferences) {
    internal fun update(seriesId: Int) {
        if (IssueRepository.checkIfStale(SERIES_TAG(seriesId), ISSUE_LIFETIME, prefs))
            CoroutineScope(Dispatchers.IO).launch {
                webservice.getIssuesBySeries(seriesId).let { issueItems ->
                    database.issueDao().upsertSus(issueItems.map { it.toRoomModel() })
                }
                IssueRepository.saveTime(prefs, SERIES_TAG(seriesId))
            }
    }
}