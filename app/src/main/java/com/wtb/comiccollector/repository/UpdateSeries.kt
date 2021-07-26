package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException

private const val TAG = APP + "UpdateSeries"

/**
 * UpdateSeries
 *
 * @property webservice
 * @property database
 * @property prefs
 * @constructor Create empty UpdateSeries
 */
@ExperimentalCoroutinesApi
class UpdateSeries(
    private val webservice: Webservice,
    val database: IssueDatabase,
    val prefs: SharedPreferences
) : Updater() {

    /**
     * Update - refreshes series issues
     *
     * @param seriesId
     */
    internal fun update(seriesId: Int) {
        Log.d(TAG, "About to get series $seriesId issues")
        if (checkIfStale(SERIES_TAG(seriesId), SERIES_LIST_LIFETIME, prefs)) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    webservice.getIssuesBySeries(seriesId).let { issueItems ->
                        database.issueDao().upsertSus(issueItems.map { it.toRoomModel() })
                    }
                    Repository.saveTime(prefs, SERIES_TAG(seriesId))
                } catch (e: SocketTimeoutException) {
                    Log.d(TAG, "update: $e")
                } catch (e: ConnectException) {
                    Log.d(TAG, "update: $e")
                }
            }
        }
    }
}