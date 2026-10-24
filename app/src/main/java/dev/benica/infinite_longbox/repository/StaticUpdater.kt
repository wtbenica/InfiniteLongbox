/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.infinite_longbox.repository

import android.content.SharedPreferences
import android.util.Log
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.InfiniteLongboxApplication.Companion.context
import dev.benica.infinite_longbox.network.Webservice
import dev.benica.infinite_longbox.database.models.*
import dev.benica.infinite_longbox.repository.Updater.PriorityDispatcher.Companion.nowDispatcher
import dev.benica.infinite_longbox.ui.update.UpdateProgress
import dev.benica.infinite_longbox.ui.update.UpdateProgress.ProgressWrapper
import kotlinx.coroutines.*

/**
 * Static updater - deprecated
 * might still be needed to pull updates later, so keeping it around ftm
 * @property webservice
 * @property database
 * @property prefs
 * @constructor Create empty Static updater
 */
@ExperimentalCoroutinesApi
class StaticUpdater private constructor(
    webservice: Webservice,
    prefs: SharedPreferences,
) : Updater(webservice, prefs) {
    /**
     * Update static async - Checks remote database for updates to publishers, series, name details,
     * characters, and series bonds.  If updates are found, they are downloaded and saved to the
     * local database.
     */
    internal suspend fun updateStaticAsync(progressUpdate: UpdateProgress) {
        Log.d(TAG, "updateStaticAsync Begin")
        installSeedIfNeeded(progressUpdate)
        getAllPublishers(progressUpdate.publisherWrapper)
        database.transactionDao().upsertStatic(
            roles = getRoles(),
            storyTypes = getStoryTypes(),
            bondTypes = getBondTypes(),
        )
        Log.d(TAG, "updateStaticAsync Part II")

        if (!DEBUG) {
            getAllSeries(progressUpdate.seriesWrapper)
            getAllNameDetails(progressUpdate.creatorWrapper)
            getAllCharacters(progressUpdate.characterWrapper)
            getAllSeriesBonds()
        }
    }

    /** On a fresh install, imports the starter database before the paged sync, so that has little left to do. */
    private suspend fun installSeedIfNeeded(progressUpdate: UpdateProgress) {
        SeedInstaller(webservice, prefs, database, context!!.cacheDir).installIfNeeded(
            onStart = { progressUpdate.setTitle("Downloading starter database") },
            onProgress = { done, total -> progressUpdate.setSeedProgress(done, total) },
        )
        progressUpdate.setTitle("Updating database")
    }

    /**
     * Checks for updates to publishers
     */
    private suspend fun getAllPublishers(updateProgress: ProgressWrapper) {
        refreshAllPaged(
            prefs = prefs,
            savePageTag = UPDATED_PUBLISHERS_PAGE,
            saveTag = UPDATED_PUBLISHERS,
            getItemsByPage = ::getPublishersByPage,
            dao = database.publisherDao(),
            getNumPages = webservice::getNumPublisherPages,
            progressWrapper = updateProgress,
            manifestKey = "publisher"
        )
    }

    /**
     * Checks for updates to series
     */
    private suspend fun getAllSeries(updateProgress: ProgressWrapper) {
        refreshAllPaged(
            prefs = prefs,
            savePageTag = UPDATED_SERIES_PAGE,
            saveTag = UPDATED_SERIES,
            getItemsByPage = ::getSeriesByPage,
            verifyForeignKeys = fKeyChecker::checkFKeysSeries,
            dao = database.seriesDao(),
            getNumPages = webservice::getNumSeriesPages,
            progressWrapper = updateProgress,
            manifestKey = "series"
        )
    }

    private suspend fun getAllNameDetails(updateProgress: ProgressWrapper) {
        refreshAllPaged(
            prefs = prefs,
            savePageTag = UPDATED_NAME_DETAILS_PAGE,
            saveTag = UPDATED_NAME_DETAILS,
            getItemsByPage = ::getNameDetailsByPage,
            verifyForeignKeys = fKeyChecker::checkFKeysNameDetail,
            dao = database.nameDetailDao(),
            getNumPages = webservice::getNumNameDetailPages,
            progressWrapper = updateProgress,
            manifestKey = "name_detail"
        )
    }

    private suspend fun getAllCharacters(updateProgress: ProgressWrapper) {
        refreshAllPaged(
            prefs = prefs,
            savePageTag = UPDATED_CHARACTERS_PAGE,
            saveTag = UPDATED_CHARACTERS,
            getItemsByPage = ::getCharactersByPage,
            dao = database.characterDao(),
            getNumPages = webservice::getNumCharacterPages,
            progressWrapper = updateProgress,
            manifestKey = "character"
        )
    }

    private suspend fun getAllSeriesBonds() {
        refreshAll(
            prefs = prefs,
            saveTag = UPDATED_BONDS,
            getItems = this::getSeriesBonds,
            followup = fKeyChecker::checkFKeysSeriesBond,
            dao = database.seriesBondDao()
        )
    }

    private suspend fun getRoles(): List<Role>? =
        getItems(prefs, webservice::getRoles, UPDATED_ROLES)

    private suspend fun getStoryTypes(): List<StoryType>? =
        getItems(prefs, webservice::getStoryTypes, UPDATED_STORY_TYPES)

    private suspend fun getBondTypes(): List<BondType>? =
        getItems(prefs, webservice::getBondTypes, UPDATED_BOND_TYPE)

    private suspend fun getSeriesBonds(): List<SeriesBond>? =
        getItems(prefs, webservice::getSeriesBonds, UPDATED_BONDS)

    private suspend fun getPublishersByPage(page: Int): List<Publisher>? =
        retrieveItemsByArgument(page, webservice::getPublisherByPage)

    private suspend fun getSeriesByPage(page: Int): List<Series>? =
        retrieveItemsByArgument(page, webservice::getSeriesByPage)

    private suspend fun getNameDetailsByPage(page: Int): List<NameDetail>? =
        retrieveItemsByArgument(page, webservice::getNameDetailsByPage)

    private suspend fun getCharactersByPage(page: Int): List<Character>? =
        retrieveItemsByArgument(page, webservice::getCharactersByPage)


    /**
     * Updates issue model, then stories, credits, appearances, and cover
     */
    internal fun expandIssueAsync(issueId: Int, markedDelete: Boolean = true): Job =
        CoroutineScope(nowDispatcher).launch {
            withContext(Dispatchers.Default) {
                updateIssueFromGcd(issueId)
                // Stories, credits and appearances are four requests; once a week is often enough
                if (checkIfStale(issueDetailsTag(issueId), WEEKLY, prefs)) {
                    if (updateIssueStoryDetails(issueId).isNotEmpty()) {
                        Repository.saveTime(prefs, issueDetailsTag(issueId))
                    }
                }
                UpdateIssueCover.get().update(issueId, markedDelete)
            }.let {
                Repository.saveTime(prefs, issueTag(issueId))
            }
        }

    private val cardIssueMarkedDelete = java.util.concurrent.ConcurrentHashMap<Int, Boolean>()

    private val cardIssues = RequestBatcher<Int>(CoroutineScope(Dispatchers.IO), CARD_BATCH_MS) { ids ->
        fetchCardIssues(ids)
    }

    /** A card on a list needs its issue and cover, not the issue's stories, credits and appearances. */
    internal fun requestIssueAndCover(issueId: Int, markedDelete: Boolean = true) {
        cardIssueMarkedDelete[issueId] = markedDelete
        cardIssues.request(issueId)
    }

    private suspend fun fetchCardIssues(ids: Set<Int>) {
        val have = ids.chunked(500).flatMap { database.issueDao().issueSeries(it) }.map { it.issueId }.toSet()
        // issues the phone lacks, and ones not refreshed for a week
        val fetch = ids.filter { it !in have || checkIfStale(issueTag(it), WEEKLY, prefs) }
        if (fetch.isNotEmpty()) {
            importIssuesById(fetch)
            val now = fetch.chunked(500).flatMap { database.issueDao().issueSeries(it) }.map { it.issueId }
            now.forEach { Repository.saveTime(prefs, issueTag(it)) }
        }
        ids.forEach { UpdateIssueCover.get().update(it, cardIssueMarkedDelete.remove(it) ?: true) }
    }

    /**
     * Updates stories, credits, and appearances for this issue
     */
    private suspend fun updateIssueStoryDetails(issueId: Int): List<Story> =
        updateIssuesStoryDetails(listOf(issueId))

    private suspend fun updateIssuesStoryDetails(issueIds: List<Int>): List<Story> {
        val stories: List<Story> = updateIssuesStories(issueIds)
        updateStoriesCredits(stories.ids)
        updateStoriesAppearances(stories.ids)
        return stories
    }

    private suspend fun updateStoriesAppearances(storyIds: List<Int>): List<Appearance> =
        updateById(
            prefs,
            null,
            ::getAppearancesByStoryIds,
            storyIds,
            fKeyChecker::checkFKeysAppearance,
            database.appearanceDao()
        )

    private suspend fun updateStoriesCredits(storyIds: List<Int>): List<CreditX> =
        updateById(
            prefs,
            null,
            ::getCreditsByStoryIds,
            storyIds,
            fKeyChecker::checkFKeysCredit,
            database.creditDao()
        ) +
                updateById(
                    prefs,
                    null,
                    ::getExCreditsByStoryIds,
                    storyIds,
                    fKeyChecker::checkFKeysCredit,
                    database.exCreditDao()
                )

    private suspend fun updateIssuesStories(issueIds: List<Int>): List<Story> =
        updateById(
            prefs,
            null,
            ::getStoriesByIssueIds,
            issueIds,
            fKeyChecker::checkFKeysStory,
            database.storyDao()
        )

    private suspend fun getAppearancesByStoryIds(storyIds: List<Int>): List<Appearance> =
        retrieveItemsByList(storyIds, webservice::getAppearancesByStoryIds)

    private suspend fun getCreditsByStoryIds(storyIds: List<Int>): List<Credit> =
        retrieveItemsByList(storyIds, webservice::getCreditsByStoryIds)

    private suspend fun getExCreditsByStoryIds(storyIds: List<Int>): List<ExCredit> =
        retrieveItemsByList(storyIds, webservice::getExCreditsByStoryIds)

    private suspend fun getStoriesByIssueIds(issueIds: List<Int>): List<Story> =
        retrieveItemsByList(issueIds, webservice::getStoriesByIssueIds)

    /** Looks up the issues for a scanned barcode on the server and saves them locally. */
    internal suspend fun importIssuesByUpc(
        upc: String,
        number: String? = null,
        limit: Int? = null,
    ): List<Int>? {
        val issues = try {
            webservice.getIssuesByUpc(upc, number, limit).models
        } catch (e: java.io.IOException) {
            Log.d(TAG, "importIssuesByUpc $e")
            return null
        } catch (e: retrofit2.HttpException) {
            Log.d(TAG, "importIssuesByUpc $e")
            return null
        }
        fKeyChecker.checkFKeysIssue(issues).await()
        database.issueDao().upsertSus(issues)
        return issues.map { it.issueId }
    }

    /** Fetches these issues from the server, with the series and variant parents they need, and saves them. */
    internal suspend fun importIssuesById(issueIds: List<Int>) {
        val issues = retrieveItemsByList(issueIds, webservice::getIssuesByIds)
        if (issues.isEmpty()) return
        fKeyChecker.checkFKeysIssue(issues).await()
        database.issueDao().upsertSus(issues)
    }

    /** A series card shows its first issue's cover. */
    /** A character's page data, or null when the server can't be reached. */
    internal suspend fun characterInfo(characterId: Int): CharacterInfo? = try {
        webservice.getCharacterInfo(characterId)
    } catch (e: java.io.IOException) {
        Log.d(TAG, "characterInfo $e")
        null
    } catch (e: retrofit2.HttpException) {
        Log.d(TAG, "characterInfo $e")
        null
    } catch (e: kotlinx.serialization.SerializationException) {
        Log.w(TAG, "characterInfo: the server's answer was not what was expected: $e")
        null
    }

    /** A creator's page data, or null when the server can't be reached. */
    internal suspend fun creatorInfo(creatorId: Int): CreatorInfo? = try {
        webservice.getCreatorInfo(creatorId)
    } catch (e: java.io.IOException) {
        Log.d(TAG, "creatorInfo $e")
        null
    } catch (e: retrofit2.HttpException) {
        Log.d(TAG, "creatorInfo $e")
        null
    } catch (e: kotlinx.serialization.SerializationException) {
        Log.w(TAG, "creatorInfo: the server's answer was not what was expected: $e")
        null
    }

    /** Series already asked about; one that could not be asked (offline, an error) is let go to be asked again. */
    private val seriesCoverAsked = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    internal fun requestSeriesCover(seriesId: Int) {
        if (seriesCoverAsked.add(seriesId)) seriesCovers.request(seriesId)
    }

    private val seriesCovers = RequestBatcher<Int>(CoroutineScope(Dispatchers.IO), CARD_BATCH_MS) { ids ->
        fetchSeriesCovers(ids)
    }

    private suspend fun fetchSeriesCovers(seriesIds: Set<Int>) {
        val covers = try {
            seriesIds.chunked(100).map { webservice.getSeriesCovers(it) }.fold(emptyMap<String, List<Int>>()) { a, m -> a + m }
        } catch (e: java.io.IOException) {
            Log.d(TAG, "fetchSeriesCovers $e")
            seriesCoverAsked.removeAll(seriesIds)
            return
        } catch (e: retrofit2.HttpException) {
            Log.d(TAG, "fetchSeriesCovers $e")
            seriesCoverAsked.removeAll(seriesIds)
            return
        } catch (e: kotlinx.serialization.SerializationException) {
            Log.w(TAG, "fetchSeriesCovers: the server's answer was not what was expected: $e")
            seriesCoverAsked.removeAll(seriesIds)
            return
        }
        val issueIds = covers.values.mapNotNull { it.firstOrNull() }
        val missing = issueIds.filter { database.issueDao().getIssueSus(it) == null }
        if (missing.isNotEmpty()) importIssuesById(missing)
        issueIds.forEach { UpdateIssueCover.get().update(it) }
    }

    private suspend fun updateIssueFromGcd(issueId: Int) {
        if (checkIfStale(issueTag(issueId), WEEKLY, prefs)) {
            importIssuesById(listOf(issueId))
        }
    }

    companion object {
        internal const val TAG = APP + "StaticUpdater"

        /** How long cards that bind together are gathered before their issues are fetched. */
        private const val CARD_BATCH_MS = 150L

        private var INSTANCE: StaticUpdater? = null

        fun get(): StaticUpdater {
            return INSTANCE
                ?: throw IllegalStateException("StaticUpdater must be initialized")
        }

        fun initialize(
            webservice: Webservice,
            prefs: SharedPreferences,
        ) {
            if (INSTANCE == null) {
                INSTANCE = StaticUpdater(webservice, prefs)
            }
        }
    }
}

