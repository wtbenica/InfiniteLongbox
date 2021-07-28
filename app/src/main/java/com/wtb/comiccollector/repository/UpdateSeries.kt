package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.Issue
import com.wtb.comiccollector.database.models.models
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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
    internal suspend fun update(seriesId: Int) {
        Log.d(TAG, "About to get series $seriesId issues")
        if (Companion.checkIfStale(SERIES_TAG(seriesId), WEEKLY, prefs)) {
            coroutineScope {
                val issues: List<Issue> = runSafely("getIssuesBySeriesId", seriesId) {
                    async { webservice.getIssuesBySeries(seriesId) }
                }?.models ?: emptyList()

                database.issueDao().upsertSus(issues)
            }.let {
                Repository.saveTime(prefs, SERIES_TAG(seriesId))
            }
        }
    }
}