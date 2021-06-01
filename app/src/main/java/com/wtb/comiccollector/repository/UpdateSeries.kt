package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = APP + "UpdateSeries"

class UpdateSeries(
    private val webservice: Webservice,
    val database: IssueDatabase,
    val prefs: SharedPreferences
) {
    internal fun update(seriesId: Int) {
        if (Repository.checkIfStale(SERIES_TAG(seriesId), ISSUE_LIFETIME, prefs))
            Log.d(TAG, "About to get series $seriesId issues")
            CoroutineScope(Dispatchers.IO).launch {
                webservice.getIssuesBySeries(seriesId).let { issueItems ->
                    database.issueDao().upsertSus(issueItems.map { it.toRoomModel() })
                }
                Repository.saveTime(prefs, SERIES_TAG(seriesId))
            }
    }
}