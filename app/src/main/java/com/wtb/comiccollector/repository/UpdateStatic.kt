package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.MainActivity
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import kotlinx.coroutines.*
import java.net.SocketTimeoutException

private const val TAG = APP + "StaticUpdater"

@ExperimentalCoroutinesApi
class StaticUpdater(
    private val webservice: Webservice,
    val database: IssueDatabase,
    val prefs: SharedPreferences,
) : Updater() {
    /**
     *  Updates publisher, series, role, and storytype tables
     */
    @ExperimentalCoroutinesApi
    internal suspend fun update(): Deferred<Unit> {
        Log.d(
            TAG,
            "Debug: ${!DEBUG} hasConnection: ${MainActivity.hasConnection.value} oldPubs: ${
                checkIfStale(
                    UPDATED_PUBLISHERS,
                    STATIC_DATA_LIFETIME,
                    prefs
                )
            } oldSeries: ${
                checkIfStale(
                    UPDATED_SERIES,
                    SERIES_LIST_LIFETIME,
                    prefs
                )
            }"
        )

        return CoroutineScope(Dispatchers.IO).async {
            if (!DEBUG) {
                val publishers = CoroutineScope(Dispatchers.IO).async {
                    if (checkIfStale(UPDATED_PUBLISHERS, STATIC_DATA_LIFETIME, prefs)) {
                        try {
                            Log.d(TAG, "update: Getting Publishers")
                            webservice.getPublishers()
                        } catch (e: SocketTimeoutException) {
                            Log.d(TAG, "update: Getting Publishers SocketTimeout: $e")
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                }

                val roles = CoroutineScope(Dispatchers.IO).async {
                    if (checkIfStale(UPDATED_ROLES, STATIC_DATA_LIFETIME, prefs)) {
                        try {
                            Log.d(TAG, "update: Getting Roles")
                            webservice.getRoles()
                        } catch (e: SocketTimeoutException) {
                            Log.d(TAG, "update: Getting Roles SocketTimeout: $e")
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                }

                val storyTypes = CoroutineScope(Dispatchers.IO).async {
                    if (checkIfStale(UPDATED_STORY_TYPES, STATIC_DATA_LIFETIME, prefs)) {
                        try {
                            Log.d(TAG, "update: Getting StoryTypes")
                            webservice.getStoryTypes()
                        } catch (e: SocketTimeoutException) {
                            Log.d(TAG, "update: Getting StoryTypes SocketTimeout: $e")
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                }

                CoroutineScope(Dispatchers.IO).async {
                    database.transactionDao().upsertStatic(
                        publishers = publishers.await()
                            .map { it.toRoomModel() }
                            .also {
                                if (it.isNotEmpty()) {
                                    Repository.saveTime(prefs, UPDATED_PUBLISHERS)
                                }
                            },
                        roles = roles.await()
                            .map { it.toRoomModel() }
                            .also {
                                if (it.isNotEmpty()) {
                                    Repository.saveTime(prefs, UPDATED_ROLES)
                                }
                            },

                        storyTypes = storyTypes.await()
                            .map { it.toRoomModel() }
                            .also {
                                if (it.isNotEmpty()) {
                                    Repository.saveTime(prefs, UPDATED_STORY_TYPES)
                                }
                            },
                    )
                }.await().let {
                    async {
                        updateSeries()
                    }.await().let {
                        updateCreators()
                    }
                }
            } else {
                CoroutineScope(Dispatchers.IO).async {
                    updateSeries()
                }.await().let {
                    updateCreators()
                }
            }
        }
    }

    private suspend fun updateSeries() {
        if (checkIfStale(UPDATED_SERIES, SERIES_LIST_LIFETIME, prefs) && !DEBUG) {
            var page = 0
            var stop = false

            do {
                webservice.getSeries(page).let { seriesItems ->
                    if (seriesItems.isEmpty()) {
                        stop = true
                    } else {
                        database.seriesDao().upsertSus(seriesItems.map {
                            it.toRoomModel()
                        })
                    }
                }
                page += 1
            } while (!stop)

            Repository.saveTime(prefs, UPDATED_SERIES)
        }
    }

    private suspend fun updateCreators() {
        if (checkIfStale(UPDATED_CREATORS, SERIES_LIST_LIFETIME, prefs) && !DEBUG) {
            var page = 0
            var stop = false

            CoroutineScope(Dispatchers.IO).async outer@{
                do {
                    async {
                        webservice.getCreators(page).let { creatorItems ->
                            if (creatorItems.isEmpty()) {
                                stop = true
                                return@async emptyList()
                            } else {
                                val creatorList = creatorItems.map {
                                    it.toRoomModel()
                                }
                                database.creatorDao().upsertSus(creatorList)
                                return@async creatorList.map { it.creatorId }
                            }
                        }
                    }.await().let { creatorIds ->
                        if (creatorIds.isEmpty()) {
                            stop = true
                        } else {
                            webservice.getNameDetailsByCreatorIds(creatorIds)
                                .let { nameDetailItems ->
                                    database.nameDetailDao()
                                        .upsertSus(nameDetailItems.map { it.toRoomModel() })
                                }
                        }
                    }
                    page += 1
                } while (!stop)
            }.await().let {
                Repository.saveTime(prefs, UPDATED_CREATORS)
            }
        }
    }
}