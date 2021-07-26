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

private const val TAG = APP + "UpdateStatic"

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
            val publishers = refreshPublishersAsync()
            val roles = refreshRolesAsync()
            val storyTypes = refreshStoryTypesAsync()
            val seriesBondTypes = refreshSeriesBondTypesAsync()

            withContext(Dispatchers.Default) {
                Log.d(TAG, "EMOSTO")
                database.transactionDao().upsertStatic(
                    publishers = prepareItems(publishers, UPDATED_PUBLISHERS),
                    roles = prepareItems(roles, UPDATED_ROLES),
                    storyTypes = prepareItems(storyTypes, UPDATED_STORY_TYPES),
                    bondTypes = prepareItems(seriesBondTypes, UPDATED_BOND_TYPE),
                )
                Log.d(TAG, "ENDING")
            }.let {
                withContext(Dispatchers.Default) {
                    Log.d(TAG, "Start")
                    refreshSeries()
                    refreshSeriesBonds()
                    refreshCreators()
                    refreshCharacters()
                }
            }
        }
    }

    private suspend fun <G : GcdJson<M>, M : DataModel> prepareItems(
        items: Deferred<List<Item<G, M>>>,
        saveTag: String
    ): List<M> =
        items.await()
            .map { it.toRoomModel() }
            .also {
                if (it.isNotEmpty()) {
                    Repository.saveTime(prefs, saveTag)
                }
            }


    private fun refreshStoryTypesAsync() = CoroutineScope(Dispatchers.IO).async types@{
        var result = emptyList<Item<GcdStoryType, StoryType>>()

        if (checkIfStale(UPDATED_STORY_TYPES, STATIC_DATA_LIFETIME, prefs) && !DEBUG) {
            try {
                result = webservice.getStoryTypes()
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "update: Getting StoryTypes SocketTimeout: $e")
            } catch (e: ConnectException) {
                Log.d(TAG, "update: Getting Publishers: $e")
            }
        }
        return@types result
    }

    private fun refreshSeriesBondTypesAsync() = CoroutineScope(Dispatchers.IO).async bondTypes@{
        var result = emptyList<Item<GcdBondType, BondType>>()

        if (checkIfStale(UPDATED_BOND_TYPE, STATIC_DATA_LIFETIME, prefs) && !DEBUG) {
            try {
                result = webservice.getBondTypes()
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "update: Getting BondTypes SocketTimeout: $e")
            } catch (e: ConnectException) {
                Log.d(TAG, "update: Getting BondTypes: $e")
            }
        }
        return@bondTypes result
    }

    private fun refreshRolesAsync() = CoroutineScope(Dispatchers.IO).async roles@{
        var result = emptyList<Item<GcdRole, Role>>()

        if (checkIfStale(UPDATED_ROLES, STATIC_DATA_LIFETIME, prefs) && !DEBUG) {
            try {
                result = webservice.getRoles()
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "update: Getting Roles: $e")
            } catch (e: ConnectException) {
                Log.d(TAG, "update: Getting Roles: $e")
            }
        }

        return@roles result
    }

    private fun refreshPublishersAsync() = CoroutineScope(Dispatchers.IO).async pubs@{
        var result = emptyList<Item<GcdPublisher, Publisher>>()

        if (checkIfStale(UPDATED_PUBLISHERS, STATIC_DATA_LIFETIME, prefs) && !DEBUG) {
            try {
                result = webservice.getPublishers()
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "update: Getting Publishers: $e")
            } catch (e: ConnectException) {
                Log.d(TAG, "update: Getting Publishers: $e")
            }
        }

        return@pubs result
    }

    private suspend fun refreshSeriesBonds() {
        CoroutineScope(Dispatchers.IO).async seriesBonds@{
            var result = emptyList<Item<GcdSeriesBond, SeriesBond>>()
            if (checkIfStale(UPDATED_SERIES_BONDS, SERIES_LIST_LIFETIME, prefs) && !DEBUG) {
                try {
                    result = webservice.getSeriesBonds()
                } catch (e: SocketTimeoutException) {
                    Log.d(TAG, "update: Getting SeriesBonds SocketTimeout: $e")
                } catch (e: ConnectException) {
                    Log.d(TAG, "update: Getting SeriesBonds: $e")
                }
            }
            return@seriesBonds result
        }.await().let { seriesBondItems: List<Item<GcdSeriesBond, SeriesBond>> ->
            val issueIds =
                seriesBondItems.mapNotNull { it.toRoomModel().originIssueId } +
                        seriesBondItems.mapNotNull { it.toRoomModel().targetIssueId }
            var issueItems = emptyList<Item<GcdIssue, Issue>>()

            if (issueItems.isNotEmpty()) {
                try {
                    issueItems = webservice.getIssues(issueIds = issueIds)
                } catch (e: SocketTimeoutException) {
                    Log.d(TAG, "update: Getting issues SocketTimeout: $e")
                } catch (e: ConnectException) {
                    Log.d(TAG, "update: issues SeriesBonds: $e")
                }
            }
            val issues = issueItems.map { it.toRoomModel() }
            val seriesBonds = seriesBondItems.map { it.toRoomModel() }
            database.transactionDao().upsertSus(
                issues = issues,
                seriesBonds = seriesBonds,
            ).also { Log.d(TAG, "upsert seriesBonds/issues") }
        }
    }

    private suspend fun refreshSeries() {
        if (checkIfStale(UPDATED_SERIES, SERIES_LIST_LIFETIME, prefs) && !DEBUG) {
            var page = 0
            var stop = false
            var success = true

            do {
                try {
                    Log.d(TAG, "SERIES!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
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
                    stop = true
                    success = false
                } catch (e: ConnectException) {
                    Log.d(TAG, "updateSeries: $e")
                    stop = true
                    success = false
                }

                page += 1
            } while (!stop)

            if (success)
                Repository.saveTime(prefs, UPDATED_SERIES)

            Log.d(TAG, "ENDSERIES SLFKJ!!!????@")
        }
    }

    private suspend fun refreshCreators() {
        if (checkIfStale(UPDATED_CREATORS, SERIES_LIST_LIFETIME, prefs) && !DEBUG) {
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

    private suspend fun refreshCharacters() {
        if (checkIfStale(UPDATED_CHARACTERS, SERIES_LIST_LIFETIME, prefs) && !DEBUG) {
            var page = 0
            var stop = false
            var success = true

            do {
                try {
                    webservice.getCharacters(page).let { characterItems ->
                        if (characterItems.isEmpty()) {
                            stop = true
                        } else {
                            Log.d(TAG, "$characterItems")
                            characterItems.map { it.toRoomModel() }.forEach {
                                Log.d(TAG, "Insert: ${it.name}, ${it.alterEgo} ${it.publisher}")
                                database.characterDao().upsertSus(it)
                            }
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    Log.d(TAG, "updateCharacters: $e")
                    stop = true
                    success = false
                } catch (e: ConnectException) {
                    Log.d(TAG, "updateCharacters: $e")
                    stop = true
                    success = false
                }

                page += 1
            } while (!stop)

            if (success)
                Repository.saveTime(prefs, UPDATED_CHARACTERS)
        }
    }
}