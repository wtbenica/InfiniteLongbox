package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.repository.Updater.PriorityDispatcher.Companion.nowDispatcher
import com.wtb.comiccollector.views.ProgressUpdateCard
import com.wtb.comiccollector.views.ProgressUpdateCard.ProgressWrapper
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
     *  UpdateAsync - Updates publisher, series, role, and storytype tables
     */
    internal suspend fun updateStaticAsync(progressUpdate: ProgressUpdateCard) {
//        getAllPublishers(progressUpdate.publisherWrapper)
        database.transactionDao().upsertStatic(
            roles = getRoles(),
            storyTypes = getStoryTypes(),
            bondTypes = getBondTypes(),
        )

//        if (!DEBUG) {
//            getAllSeries(progressUpdate.seriesWrapper)
//            getAllNameDetails(progressUpdate.creatorWrapper)
//            getAllCharacters(progressUpdate.characterWrapper)
//            getAllSeriesBonds()
//        }
    }

    private suspend fun getAllPublishers(updateProgress: ProgressWrapper) {
        refreshAllPaged<Publisher>(
            prefs = prefs,
            savePageTag = UPDATED_PUBLISHERS_PAGE,
            saveTag = UPDATED_PUBLISHERS,
            getItemsByPage = ::getPublishersByPage,
            dao = database.publisherDao(),
            getNumPages = webservice::getNumPublisherPages,
            progressWrapper = updateProgress
        )
    }

    private suspend fun getAllSeries(updateProgress: ProgressWrapper) {
        refreshAllPaged<Series>(
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
        refreshAllPaged<NameDetail>(
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
        refreshAllPaged<Character>(
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
        refreshAll<SeriesBond>(
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
    internal fun expandIssueAsync(issueId: Int, markedDelete: Boolean = true): Deferred<Unit> =
        CoroutineScope(nowDispatcher).async {
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

