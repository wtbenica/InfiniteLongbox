package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private const val TAG = APP + "StaticUpdater"

class StaticUpdater(
    private val webservice: Webservice,
    val database: IssueDatabase,
    val prefs: SharedPreferences
) {
    /**
     *  Updates publisher, series, role, and storytype tables
     */
    internal fun update() {
        if (Repository.checkIfStale(STATIC_DATA_UPDATED, STATIC_DATA_LIFETIME, prefs)) {
            Log.d(TAG, "StaticUpdater update")
            val publishers = CoroutineScope(Dispatchers.IO).async {
                webservice.getPublishers()
            }

            val roles = CoroutineScope(Dispatchers.IO).async {
                webservice.getRoles()
            }

            val storyTypes = CoroutineScope(Dispatchers.IO).async {
                webservice.getStoryTypes()
            }

            CoroutineScope(Dispatchers.IO).launch {
                database.transactionDao().upsertStatic(
                    publishers = publishers.await().map { it.toRoomModel() },
                    roles = roles.await().map { it.toRoomModel() },
                    storyTypes = storyTypes.await().map { it.toRoomModel() }
                )
                    .let {
                        Repository.saveTime(prefs, STATIC_DATA_UPDATED)
                        updateSeries()
                    }
            }
        } else {
            if (Repository.checkIfStale(SERIES_LIST_UPDATED, SERIES_LIST_LIFETIME, prefs)) {
                CoroutineScope(Dispatchers.IO).launch {
                    updateSeries()
                }
            }
        }
    }

    private suspend fun updateSeries() {
        var page = 0
        var stop = false

        do {
            webservice.getSeries(page).let { seriesItems ->
                if (seriesItems.isEmpty()) {
                    stop = true
                } else {
                    database.seriesDao().upsertSus(seriesItems.map {
                        Log.d(TAG, "DOWNLOAD SERIES TRACKING_NOTES: ${it.fields.trackingNotes}")
                        it.toRoomModel()
                    })
                }
            }
            page += 1
        } while (!stop)

        Repository.saveTime(prefs, SERIES_LIST_UPDATED)
    }
}