package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.MainActivity
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.*
import java.net.ConnectException
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
    internal suspend fun updateAsync(): Deferred<Unit> {
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
            val publishers = CoroutineScope(Dispatchers.IO).async pubs@{
                var result = emptyList<Item<GcdPublisher, Publisher>>()

                if (checkIfStale(UPDATED_PUBLISHERS, STATIC_DATA_LIFETIME, prefs)) {
                    try {
                        Log.d(TAG, "update: Getting Publishers")
                        result = webservice.getPublishers()
                    } catch (e: SocketTimeoutException) {
                        Log.d(TAG, "update: Getting Publishers: $e")
                    } catch (e: ConnectException) {
                        Log.d(TAG, "update: Getting Publishers: $e")
                    }
                }

                return@pubs result
            }

            val roles = CoroutineScope(Dispatchers.IO).async roles@{
                var result = emptyList<Item<GcdRole, Role>>()

                if (checkIfStale(UPDATED_ROLES, STATIC_DATA_LIFETIME, prefs)) {
                    try {
                        Log.d(TAG, "update: Getting Roles")
                        result = webservice.getRoles()
                    } catch (e: SocketTimeoutException) {
                        Log.d(TAG, "update: Getting Roles: $e")
                    } catch (e: ConnectException) {
                        Log.d(TAG, "update: Getting Roles: $e")
                    }
                }

                return@roles result
            }

            val storyTypes = CoroutineScope(Dispatchers.IO).async types@{
                var result = emptyList<Item<GcdStoryType, StoryType>>()

                if (checkIfStale(UPDATED_STORY_TYPES, STATIC_DATA_LIFETIME, prefs)) {
                    try {
                        Log.d(TAG, "update: Getting StoryTypes")
                        result = webservice.getStoryTypes()
                    } catch (e: SocketTimeoutException) {
                        Log.d(TAG, "update: Getting StoryTypes SocketTimeout: $e")
                    } catch (e: ConnectException) {
                        Log.d(TAG, "update: Getting Publishers: $e")
                    }
                }
                return@types result
            }

            val seriesBondTypes = CoroutineScope(Dispatchers.IO).async bondTypes@{
                var result = emptyList<Item<GcdBondType, BondType>>()

                if (checkIfStale(UPDATED_BOND_TYPE, STATIC_DATA_LIFETIME, prefs)) {
                    Log.d(TAG, "Getting Series Bond Types")
                    try {
                        result = webservice.getBondTypes()
                    } catch (e: SocketTimeoutException) {
                        Log.d(TAG, "update: Getting BondTypes SocketTimeout: $e")
                    } catch (e: ConnectException) {
                        Log.d(TAG, "update: Getting BondTypes: $e")
                    }
                }
                Log.d(TAG, "SBT RESULTS: ${result.size}")
                return@bondTypes result
            }.await().also {
                Log.d(TAG, "bondTypes complete")
            }

            val seriesBonds = CoroutineScope(Dispatchers.IO).async seriesBonds@{
                var result = emptyList<Item<GcdSeriesBond, SeriesBond>>()

                if (checkIfStale(UPDATED_SERIES_BONDS, SERIES_LIST_LIFETIME, prefs)) {
                    Log.d(TAG, "Getting Series Bonds")
                    try {
                        result = webservice.getSeriesBonds()
                        Log.d(TAG, "setting new result")
                    } catch (e: SocketTimeoutException) {
                        Log.d(TAG, "update: Getting SeriesBonds SocketTimeout: $e")
                    } catch (e: ConnectException) {
                        Log.d(TAG, "update: Getting SeriesBonds: $e")
                    }
                }
                Log.d(TAG, "SBS RESULTS: ${result.size}")
                return@seriesBonds result
            }.await().also {
                Log.d(TAG, "seriesBonds complete")
            }

            withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
                Log.d(TAG, "Uploading *(#RESPFPSEJFPWIHR(*")
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
                    bondTypes = seriesBondTypes.map { it.toRoomModel() }
                        .also {
                            if (it.isNotEmpty()) {
                                Repository.saveTime(prefs, UPDATED_BOND_TYPE)
                            }
                        },
                    seriesBonds = seriesBonds.map { it.toRoomModel() }
                        .also {
                            if (it.isNotEmpty()) {
                                Repository.saveTime(prefs, UPDATED_SERIES_BONDS)
                            }
                        }
                )
            }.let {
                withContext(Dispatchers.Default) {
                    updateSeries()
                }.let {
                    updateCreators()
                }
            }
        }
    }

    private suspend fun updateSeries() {
        if (checkIfStale(UPDATED_SERIES, SERIES_LIST_LIFETIME, prefs)) {
            var page = 0
            var stop = false

            do {
                try {
                    webservice.getSeries(page).let { seriesItems ->
                        if (seriesItems.isEmpty()) {
                            stop = true
                        } else {
                            database.seriesDao().upsertSus(seriesItems.map {
                                it.toRoomModel()
                            })
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    Log.d(TAG, "updateSeries: $e")
                } catch (e: ConnectException) {
                    Log.d(TAG, "updateSeries: $e")
                }

                page += 1
            } while (!stop)

            Repository.saveTime(prefs, UPDATED_SERIES)
        }
    }

    private suspend fun updateCreators() {
        if (checkIfStale(UPDATED_CREATORS, SERIES_LIST_LIFETIME, prefs)) {
            var page = 0
            var stop = false

            CoroutineScope(Dispatchers.IO).async outer@{
                do {
                    async creatorIds@{
                        var result = emptyList<Int>()

                        try {
                            webservice.getCreators(page).let { creatorItems ->
                                if (creatorItems.isEmpty()) {
                                    stop = true
                                } else {
                                    val creatorList = creatorItems.map {
                                        it.toRoomModel()
                                    }
                                    database.creatorDao().upsertSus(creatorList)
                                    result = creatorList.map { it.creatorId }
                                }
                            }
                        } catch (e: SocketTimeoutException) {
                            Log.d(TAG, "updateCreators - creatorIds: $e")
                        } catch (e: ConnectException) {
                            Log.d(TAG, "updateCreators - creatorIds: $e")
                        }

                        return@creatorIds result
                    }.await().let { creatorIds ->
                        if (creatorIds.isEmpty()) {
                            stop = true
                        } else {
                            try {
                                webservice.getNameDetailsByCreatorIds(creatorIds)
                                    .let { nameDetailItems ->
                                        database.nameDetailDao()
                                            .upsertSus(nameDetailItems.map { it.toRoomModel() })
                                    }
                            } catch (e: SocketTimeoutException) {
                                Log.d(TAG, "updateCreators - nameDetails: $e")
                            } catch (e: ConnectException) {
                                Log.d(TAG, "updateCreators - nameDetails: $e")
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