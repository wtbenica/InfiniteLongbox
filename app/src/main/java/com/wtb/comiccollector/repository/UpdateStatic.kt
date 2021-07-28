package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.*

private const val TAG = APP + "UpdateStatic"

/**
 * Static updater
 *
 * @property webservice
 * @property database
 * @property prefs
 * @constructor Create empty Static updater
 */
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
    internal suspend fun updateAsync() =
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            val publishers: List<Publisher> = getPublishers()
            val roles: List<Role> = getRoles()
            val storyTypes: List<StoryType> = getStoryTypes()
            val bondTypes: List<BondType> = getBondTypes()

            database.transactionDao().upsertStatic(
                publishers = saveTimeIfNotEmpty(publishers, UPDATED_PUBLISHERS),
                roles = saveTimeIfNotEmpty(roles, UPDATED_ROLES),
                storyTypes = saveTimeIfNotEmpty(storyTypes, UPDATED_STORY_TYPES),
                bondTypes = saveTimeIfNotEmpty(bondTypes, UPDATED_BOND_TYPE),
            )
        }.let {
            Log.d(TAG, "Updating Series, Creators, Characters")
            refreshSeries()
            refreshSeriesBonds()
            refreshCreators()
            refreshCharacters()
        }


    private suspend fun getPublishers(): List<Publisher> = supervisorScope {
        if (Companion.checkIfStale(UPDATED_PUBLISHERS, MONTHLY, prefs) && !DEBUG) {
            Log.d(TAG, "getPublishers STALE")
            runSafely("update: Getting Publishers", null) {
                async { webservice.getPublishers() }
            }?.models ?: emptyList()
        } else {
            Log.d(TAG, "getPublishers NOT STALE")
            emptyList()
        }
    }

    private suspend fun getRoles(): List<Role> = supervisorScope {
        if (Companion.checkIfStale(UPDATED_ROLES, MONTHLY, prefs) && !DEBUG) {
            Log.d(TAG, "getRoles STALE")
            runSafely("getRoles", null) {
                async { webservice.getRoles() }
            }?.models ?: emptyList()
        } else {
            Log.d(TAG, "getRoles NOT STALE")
            emptyList()
        }
    }

    private suspend fun getStoryTypes(): List<StoryType> = supervisorScope {
        if (Companion.checkIfStale(UPDATED_STORY_TYPES, MONTHLY, prefs) && !DEBUG) {
            Log.d(TAG, "getStoryTypes STALE")
            runSafely("getStoryTypes", null) {
                async { webservice.getStoryTypes() }
            }?.models ?: emptyList()
        } else {
            Log.d(TAG, "getStoryTypes NOT STALE")
            emptyList()
        }
    }

    private suspend fun getBondTypes(): List<BondType> = supervisorScope {
        if (Companion.checkIfStale(UPDATED_BOND_TYPE, MONTHLY, prefs) && !DEBUG) {
            Log.d(TAG, "getBondTypes STALE")
            runSafely("getBondTypes", null) {
                async { webservice.getBondTypes() }
            }?.models ?: emptyList()
        } else {
            Log.d(TAG, "getBondTypes NOT STALE")
            emptyList()
        }
    }

    private suspend fun refreshSeries() {
        if (Companion.checkIfStale(UPDATED_SERIES, WEEKLY, prefs) && !DEBUG) {
            Log.d(TAG, "SERIES LIST STALE, UPDATING")
            var page = prefs.getInt(UPDATED_CHARACTERS_PAGE, 0)
            var stop = false
            var success = true

            do {
                val seriesPage: List<Series>? = getSeriesByPage(page)

                if (seriesPage != null && seriesPage.isNotEmpty()) {
                    database.seriesDao().upsertSus(seriesPage)
                    Repository.savePrefValue(prefs, UPDATED_SERIES_PAGE, page)
                } else if (seriesPage != null) {
                    stop = true
                } else {
                    stop = true
                    success = false
                }

                page += 1
            } while (!stop)

            if (success) {
                Repository.savePrefValue(prefs, UPDATED_SERIES_PAGE, 0)
                Repository.saveTime(prefs, UPDATED_SERIES)
            }
        } else {
            Log.d(TAG, "SERIES LIST NOT STALE")
        }
    }

    private suspend fun refreshSeriesBonds() = coroutineScope {
        if (Companion.checkIfStale(UPDATED_BONDS, WEEKLY, prefs) && !DEBUG) {
            Log.d(TAG, "SERIES BONDS STALE, UPDATING")
            val seriesBonds: List<SeriesBond>? = getSeriesBonds()
            Log.d(TAG, "SERIES BONDS COMPLETE!")
            val originIssueIds: List<Int> =
                seriesBonds?.mapNotNull { it.originIssueId } ?: emptyList()
            val targetIssueIds: List<Int> =
                seriesBonds?.mapNotNull { it.targetIssueId } ?: emptyList()

            val issueIds = originIssueIds + targetIssueIds

            Log.d(TAG, "ISSUES BY IDS")
            val issues: List<Issue>? = getIssuesByIds(issueIds)

            database.transactionDao().upsert(
                seriesBonds = seriesBonds,
                issues = issues
            )
        } else {
            Log.d(TAG, "SERIES BONDS NOT STALE")
        }
    }

    private suspend fun refreshCreators() {
        if (Companion.checkIfStale(UPDATED_CREATORS, WEEKLY, prefs) && !DEBUG) {
            var page = prefs.getInt(UPDATED_CREATORS_PAGE, 0)
            var stop = false
            var success = true

            do {
                val creators: List<Creator>? = getCreatorsByPage(page)

                if (creators != null && creators.isNotEmpty()) {
                    val nameDetails: List<NameDetail>? = getNameDetailsByCreatorIds(creators.ids)

                    database.transactionDao().upsert(
                        creators = creators,
                        nameDetails = nameDetails
                    )

                    Repository.savePrefValue(prefs, UPDATED_CREATORS_PAGE, page)
                } else if (creators != null) {
                    stop = true
                } else {
                    stop = true
                    success = false
                }

                page += 1
            } while (!stop)

            if (success) {
                Repository.savePrefValue(prefs, UPDATED_CREATORS_PAGE, 0)
                Repository.saveTime(prefs, UPDATED_CREATORS)
            }
        }
    }

    private suspend fun refreshCharacters() {
        if (Companion.checkIfStale(UPDATED_CHARACTERS, WEEKLY, prefs) && !DEBUG) {
            var page = prefs.getInt(UPDATED_CHARACTERS_PAGE, 0)
            var stop = false
            var success = true

            do {
                val characterPage: List<Character>? = getCharactersByPage(page)

                if (characterPage != null && characterPage.isNotEmpty()) {
                    database.characterDao().upsertSus(characterPage)
                    Repository.savePrefValue(prefs, UPDATED_CHARACTERS_PAGE, page)
                } else if (characterPage != null) {
                    stop = true
                } else {
                    stop = true
                    success = false
                }

                page += 1
            } while (!stop)

            if (success) {
                Repository.savePrefValue(prefs, UPDATED_CHARACTERS_PAGE, 0)
                Repository.saveTime(prefs, UPDATED_CHARACTERS)
            }
        }
    }

    private suspend fun getCharactersByPage(page: Int): List<Character>? = supervisorScope {
        runSafely("getCharactersByPage", page) {
            async { webservice.getCharacters(it) }
        }?.models
    }

    private suspend fun getSeriesBonds(): List<SeriesBond>? = supervisorScope {
        Log.d(TAG, "getSeriesBonds")
        runSafely("getSeriesBonds", null, {
            async { webservice.getSeriesBonds() }
        })?.models
    }

    private suspend fun getIssuesByIds(issueIds: List<Int>): List<Issue>? = supervisorScope {
        runSafely("getIssuesByIds", issueIds) {
            async { webservice.getIssues(issueIds) }
        }?.models
    }

    private suspend fun getSeriesByPage(page: Int): List<Series>? = supervisorScope {
        runSafely("refreshSeriesByPage", page) {
            async { webservice.getSeries(it) }
        }?.models
    }

    private suspend fun getNameDetailsByCreatorIds(creatorIds: List<Int>): List<NameDetail>? =
        supervisorScope {
            runSafely("getNameDetailsByCreatorIds", creatorIds) {
                async {
                    webservice.getNameDetailsByCreatorIds(it)
                }
            }?.models
        }

    private suspend fun getCreatorsByPage(page: Int): List<Creator>? = supervisorScope {
        runSafely("updateCreators", page) {
            async { webservice.getCreators(page) }
        }?.models
    }

    @ExperimentalCoroutinesApi
    private fun <M : DataModel> saveTimeIfNotEmpty(items: List<M>, saveTag: String): List<M> =
        items.also {
            if (it.isNotEmpty()) {
                Repository.saveTime(prefs, saveTag)
            }
        }
}