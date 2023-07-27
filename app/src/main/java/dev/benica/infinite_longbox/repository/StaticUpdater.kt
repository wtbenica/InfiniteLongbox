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
import dev.benica.infinite_longbox.Webservice
import dev.benica.infinite_longbox.database.models.*
import dev.benica.infinite_longbox.repository.Updater.PriorityDispatcher.Companion.nowDispatcher
import dev.benica.infinite_longbox.views.ProgressUpdateCard
import dev.benica.infinite_longbox.views.ProgressUpdateCard.ProgressWrapper
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
    internal suspend fun updateStaticAsync(progressUpdate: ProgressUpdateCard) {
        Log.d(TAG, "updateStaticAsync Begin")
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
            progressWrapper = updateProgress
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
            progressWrapper = updateProgress
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
            progressWrapper = updateProgress
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
            progressWrapper = updateProgress
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
                updateIssueStoryDetails(issueId)
                UpdateIssueCover.get().update(issueId, markedDelete)
            }.let {
                Repository.saveTime(prefs, issueTag(issueId))
            }
        }


    /**
     * Updates stories, credits, and appearances for this issue
     */
    private suspend fun updateIssueStoryDetails(issueId: Int) =
        updateIssuesStoryDetails(listOf(issueId))

    private suspend fun updateIssuesStoryDetails(issueIds: List<Int>) {
        val stories: List<Story> = updateIssuesStories(issueIds)
        updateStoriesCredits(stories.ids)
        updateStoriesAppearances(stories.ids)
    }

    private val appearanceCollector = Collector.appearanceCollector()
    private val creditCollector = Collector.creditCollector()
    private val exCreditCollector = Collector.exCreditCollector()
    private val storyCollector = Collector.storyCollector()

    private suspend fun updateStoriesAppearances(storyIds: List<Int>): List<Appearance> =
        updateById(
            prefs,
            null,
            ::getAppearancesByStoryIds,
            storyIds,
            fKeyChecker::checkFKeysAppearance,
            appearanceCollector
        )

    private suspend fun updateStoriesCredits(storyIds: List<Int>): List<CreditX> =
        updateById(
            prefs,
            null,
            ::getCreditsByStoryIds,
            storyIds,
            fKeyChecker::checkFKeysCredit,
            creditCollector
        ) +
                updateById(
                    prefs,
                    null,
                    ::getExCreditsByStoryIds,
                    storyIds,
                    fKeyChecker::checkFKeysCredit,
                    exCreditCollector
                )

    private suspend fun updateIssuesStories(issueIds: List<Int>): List<Story> =
        updateById(
            prefs,
            null,
            ::getStoriesByIssueIds,
            issueIds,
            fKeyChecker::checkFKeysStory,
            storyCollector
        )

    private suspend fun getAppearancesByStoryIds(storyIds: List<Int>): List<Appearance> =
        retrieveItemsByList(storyIds, webservice::getAppearancesByStoryIds)

    private suspend fun getCreditsByStoryIds(storyIds: List<Int>): List<Credit> =
        retrieveItemsByList(storyIds, webservice::getCreditsByStoryIds)

    private suspend fun getExCreditsByStoryIds(storyIds: List<Int>): List<ExCredit> =
        retrieveItemsByList(storyIds, webservice::getExCreditsByStoryIds)

    private suspend fun getStoriesByIssueIds(issueIds: List<Int>): List<Story> =
        retrieveItemsByList(issueIds, webservice::getStoriesByIssueIds)

    private suspend fun updateIssueFromGcd(issueId: Int) {
        if (checkIfStale(issueTag(issueId), WEEKLY, prefs)) {
            retrieveItemsByArgument(listOf(issueId), webservice::getIssuesByIds)
        }
    }

    companion object {
        internal const val TAG = APP + "StaticUpdater"

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

