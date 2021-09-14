package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.daos.Count
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.repository.Updater.Companion.Collector
import com.wtb.comiccollector.repository.Updater.PriorityDispatcher.Companion.highPriorityDispatcher
import com.wtb.comiccollector.repository.Updater.PriorityDispatcher.Companion.lowPriorityDispatcher
import com.wtb.comiccollector.repository.Updater.PriorityDispatcher.Companion.nowDispatcher
import kotlinx.coroutines.*

/**
 * Static updater
 *
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
    @ExperimentalCoroutinesApi
    internal suspend fun updateAsync() {
        getAllPublishers()
        database.transactionDao().upsertStatic(
            roles = getRoles(),
            storyTypes = getStoryTypes(),
            bondTypes = getBondTypes(),
        )

        if (!DEBUG) {
            getAllSeries()
            getAllCreators()
            getAllNameDetails()
            getAllCharacters()
            getAllSeriesBonds()
        }
    }

    fun updateSeries(seriesId: Int) = CoroutineScope(highPriorityDispatcher).launch {
        withContext(Dispatchers.Default) {
            if (checkIfStale(seriesTag(seriesId), 1L, prefs)) {
                val issues: List<Issue> = updateSeriesIssues(seriesId)
                updateIssues(issues.ids)
            }
        }.let {
            Repository.saveTime(prefs, seriesTag(seriesId))
        }
    }

    fun updateCharacter(characterId: Int) = CoroutineScope(lowPriorityDispatcher).launch {
        withContext(Dispatchers.Default) {
            if (checkIfStale(characterTag(characterId), 1L, prefs)) {
                val appearances: List<Appearance> = updateCharacterAppearances(characterId)
                checkFKeysAppearance(appearances)
                val storyIds = appearances.map { it.story }
                updateStoriesCredits(storyIds)
                updateStoriesAppearances(storyIds)
            }
        }.let {
            Repository.saveTime(prefs, characterTag(characterId))
        }
    }

    /**
     * Updates issue model, then stories, credits, appearances, and cover
     */
    internal fun updateIssue(issueId: Int, markedDelete: Boolean = true) =
        CoroutineScope(nowDispatcher).launch {
            async {
                updateIssueFromGcd(issueId)
            }.await().let {
                updateIssueStoryDetails(issueId)
                UpdateIssueCover.get().update(issueId, markedDelete)
            }
        }.let {
            Repository.saveTime(prefs, issueTag(issueId))
        }

    /**
     * Updates issue story details and covers
     *
     * @param issueIds
     */
    private fun updateIssues(issueIds: List<Int>) {
        CoroutineScope(highPriorityDispatcher).launch {
            issueIds.forEach { updateIssue(it) }
        }
    }

    fun updateCreators(creatorIds: List<Int>) = creatorIds.forEach(this::updateCreator)

    /**
     * Gets nameDetails and credits for each creator
     */
    private fun updateCreator(creatorId: Int) = CoroutineScope(lowPriorityDispatcher).launch {
        withContext(Dispatchers.Default) {
            if (checkIfStale(creatorTag(creatorId), 1L, prefs)) {
                val nameDetails: List<NameDetail> =
                    database.nameDetailDao().getNameDetailsByCreatorId(creatorId)
                val credits: List<CreditX> = updateNameDetailsCredits(nameDetails.ids) +
                        updateNameDetailsExCredits(nameDetails.ids)
                checkFKeysCredit(credits)
                val storyIds = credits.map { it.story }
                updateStoriesCredits(storyIds)
                updateStoriesAppearances(storyIds)
            }
        }.let {
            Repository.saveTime(prefs, creatorTag(creatorId))
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

    private val appearanceCollector = AppearanceCollector()
    private val creditCollector = CreditCollector()
    private val exCreditCollector = ExCreditCollector()
    private val storyCollector = StoryCollector()
    private val issueCollector = IssueCollector()


    private suspend fun updateStoriesAppearances(storyIds: List<Int>): List<Appearance> =
        updateById(prefs,
                   null,
                   ::getAppearancesByStoryIds,
                   storyIds,
                   ::checkFKeysAppearance,
                   appearanceCollector)

    private suspend fun updateStoriesCredits(storyIds: List<Int>): List<CreditX> =
        updateById(prefs,
                   null,
                   ::getCreditsByStoryIds,
                   storyIds,
                   ::checkFKeysCredit,
                   creditCollector) +
                updateById(prefs,
                           null,
                           ::getExCreditsByStoryIds,
                           storyIds,
                           ::checkFKeysCredit,
                           exCreditCollector)

    private suspend fun updateIssuesStories(issueIds: List<Int>): List<Story> =
        updateById(prefs,
                   null,
                   ::getStoriesByIssueIds,
                   issueIds,
                   ::checkFKeysStory,
                   storyCollector)

    /**
     * Gets series issues, adds missing foreign key models
     */
    private suspend fun updateSeriesIssues(seriesId: Int) =
        updateById(prefs,
                   ::seriesTag,
                   ::getIssuesBySeriesId,
                   seriesId,
                   ::checkFKeysIssue,
                   issueCollector)

    /**
     * Gets character appearances, adds missing foreign key models
     */
    private suspend fun updateCharacterAppearances(characterId: Int) =
        updateById(prefs,
                   ::characterTag,
                   ::getAppearancesByCharacterId,
                   characterId,
                   ::checkFKeysAppearance,
                   appearanceCollector)

    /**
     * Gets name detail credits, adds missing foreign key models
     */
    private suspend fun updateNameDetailCredits(nameDetailId: Int) =
        updateById(prefs,
                   null,
                   ::getCreditsByNameDetailId,
                   nameDetailId,
                   ::checkFKeysCredit,
                   creditCollector)

    private suspend fun updateNameDetailsCredits(nameDetailIds: List<Int>) =
        updateById(prefs,
                   null,
                   ::getCreditsByNameDetailIds,
                   nameDetailIds,
                   ::checkFKeysCredit,
                   creditCollector)

    private suspend fun updateNameDetailsExCredits(nameDetailIds: List<Int>) =
        updateById(prefs,
                   null,
                   ::getExCreditsByNameDetailIds,
                   nameDetailIds,
                   ::checkFKeysCredit,
                   exCreditCollector)

    private suspend fun getAppearancesByStoryIds(storyIds: List<Int>): List<Appearance> =
        getItemsByList(storyIds, webservice::getAppearancesByStoryIds)

    private suspend fun getCreditsByStoryIds(storyIds: List<Int>): List<Credit> =
        getItemsByList(storyIds, webservice::getCreditsByStoryIds)

    private suspend fun getExCreditsByStoryIds(storyIds: List<Int>): List<ExCredit> =
        getItemsByList(storyIds, webservice::getExCreditsByStoryIds)

    private suspend fun getStoriesByIssueIds(issueIds: List<Int>): List<Story> =
        getItemsByList(issueIds, webservice::getStoriesByIssueIds)

    private suspend fun getIssuesBySeriesId(seriesId: Int): List<Issue>? =
        getItemsByArgument(seriesId, webservice::getIssuesBySeries)

    private suspend fun getAppearancesByCharacterId(characterId: Int): List<Appearance>? =
        getItemsByArgument(characterId, webservice::getAppearancesByCharacterIds)

    private suspend fun getCreditsByNameDetailId(nameDetailId: Int): List<Credit>? =
        getItemsByArgument(listOf(nameDetailId), webservice::getCreditsByNameDetail)

    private suspend fun getCreditsByNameDetailIds(nameDetailIds: List<Int>): List<Credit> =
        getItemsByList(nameDetailIds, webservice::getCreditsByNameDetail)

    private suspend fun getExCreditsByNameDetailIds(nameDetailIds: List<Int>): List<ExCredit> =
        getItemsByList(nameDetailIds, webservice::getExCreditsByNameDetail)

    private suspend fun getAllSeriesBonds() {
        refreshAll<SeriesBond>(
            prefs = prefs,
            saveTag = UPDATED_BONDS,
            getItems = this::getSeriesBonds,
            followup = this::checkFKeysSeriesBond,
            dao = database.seriesBondDao()
        )
    }

    private suspend fun getNumCreatorPages(): Count = webservice.getNumCreatorPages()
    private suspend fun getNumNameDetailPages(): Count = webservice.getNumNameDetailPages()
    private suspend fun getNumSeriesPages(): Count = webservice.getNumSeriesPages()
    private suspend fun getNumCharacterPages(): Count = webservice.getNumCharacterPages()
    private suspend fun getNumPublisherPages(): Count = webservice.getNumPublisherPages()

    private suspend fun getAllCharacters() {
        refreshAllPaged<Character>(
            prefs = prefs,
            savePageTag = UPDATED_CHARACTERS_PAGE,
            saveTag = UPDATED_CHARACTERS,
            getItemsByPage = ::getCharactersByPage,
            dao = database.characterDao(),
            getNumPages = ::getNumCharacterPages
        )
    }

    private suspend fun getAllNameDetails() {
        refreshAllPaged<NameDetail>(
            prefs = prefs,
            savePageTag = UPDATED_NAME_DETAILS_PAGE,
            saveTag = UPDATED_NAME_DETAILS,
            getItemsByPage = ::getNameDetailsByPage,
            verifyForeignKeys = ::checkFKeysNameDetail,
            dao = database.nameDetailDao(),
            getNumPages = ::getNumNameDetailPages
        )
    }

    private suspend fun getAllCreators() {
        refreshAllPaged<Creator>(
            prefs = prefs,
            savePageTag = UPDATED_CREATORS_PAGE,
            saveTag = UPDATED_CREATORS,
            getItemsByPage = ::getCreatorsByPage,
            dao = database.creatorDao(),
            getNumPages = ::getNumCreatorPages
        )
    }

    private suspend fun getAllSeries() {
        refreshAllPaged<Series>(
            prefs = prefs,
            savePageTag = UPDATED_SERIES_PAGE,
            saveTag = UPDATED_SERIES,
            getItemsByPage = this::getSeriesByPage,
            verifyForeignKeys = this::checkFKeysSeries,
            dao = database.seriesDao(),
            getNumPages = ::getNumSeriesPages
        )
    }

    private suspend fun getAllPublishers() {
        refreshAllPaged<Publisher>(
            prefs = prefs,
            savePageTag = UPDATED_PUBLISHERS_PAGE,
            saveTag = UPDATED_PUBLISHERS,
            getItemsByPage = this::getPublishersByPage,
            dao = database.publisherDao(),
            getNumPages = ::getNumPublisherPages
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

    internal suspend fun updateIssueFromGcd(issueId: Int) {
        if (checkIfStale(issueTag(issueId), WEEKLY, prefs)) {
            getItemsByArgument(listOf(issueId), webservice::getIssuesByIds)
        }
    }

    private suspend fun getCharactersByPage(page: Int): List<Character>? =
        getItemsByArgument(page, webservice::getCharactersByPage)

    private suspend fun getSeriesByPage(page: Int): List<Series>? =
        getItemsByArgument(page, webservice::getSeriesByPage)

    private suspend fun getPublishersByPage(page: Int): List<Publisher>? =
        getItemsByArgument(page, webservice::getPublisherByPage)

    private suspend fun getCreatorsByPage(page: Int): List<Creator>? =
        getItemsByArgument(page, webservice::getCreatorsByPage)

    private suspend fun getNameDetailsByPage(page: Int): List<NameDetail>? =
        getItemsByArgument(page, webservice::getNameDetailsByPage)

    inner class AppearanceCollector : Collector<Appearance>(database.appearanceDao())
    inner class CreditCollector : Collector<Credit>(database.creditDao())
    inner class ExCreditCollector : Collector<ExCredit>(database.exCreditDao())
    inner class StoryCollector : Collector<Story>(database.storyDao())
    inner class IssueCollector : Collector<Issue>(database.issueDao())

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

