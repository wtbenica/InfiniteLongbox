package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.*
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
class UpdateCharacter(
    private val webservice: Webservice,
    val database: IssueDatabase,
    val prefs: SharedPreferences
) : Updater() {

    /**
     * Update - refreshes series issues
     *
     * @param seriesId
     */
    internal fun update(filter: SearchFilter) {
        CoroutineScope(Dispatchers.IO).launch {
            val issues: List<FullIssue> = database.issueDao().getIssuesByFilterSus(filter)
            val stories: List<Story> =
                issues.flatMap { database.storyDao().getStoriesSus(it.issue.issueId) }
            try {
                val appearances: List<Appearance> = webservice
                    .getAppearancesByStory(stories.map { it.issueId }).map { it.toRoomModel() }
                database.appearanceDao().upsertSus(appearances)
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "getAppearancesByStory $e")
            } catch (e: ConnectException) {
                Log.d(TAG, "getAppearancesByStory $e")
            }
        }
    }
}