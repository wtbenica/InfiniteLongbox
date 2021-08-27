package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.repository.Updater.Companion.Collector
import com.wtb.comiccollector.repository.Updater.PriorityDispatcher.Companion.highPriorityDispatcher
import com.wtb.comiccollector.repository.Updater.PriorityDispatcher.Companion.lowPriorityDispatcher
import com.wtb.comiccollector.repository.Updater.PriorityDispatcher.Companion.nowDispatcher
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.net.URL
import java.time.Instant

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
    webservice: Webservice,
    prefs: SharedPreferences,
) : Updater(webservice, prefs) {
    /**
     * Updates issues, stories, appearances, and creators
     */
    /**
     *  UpdateAsync - Updates publisher, series, role, and storytype tables
     */
    @ExperimentalCoroutinesApi
    internal suspend fun updateAsync() {
        database.transactionDao().upsertStatic(
            publishers = getPublishers(),
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
        async {
            if (checkIfStale(seriesTag(seriesId), 1L, prefs)) {
                val issues: List<Issue> = updateSeriesIssues(seriesId)
                updateIssues(issues.ids)
            }
        }.await().let {
            Repository.saveTime(prefs, seriesTag(seriesId))
        }
    }

    fun updateCharacter(characterId: Int) = CoroutineScope(lowPriorityDispatcher).launch {
        async {
            if (checkIfStale(characterTag(characterId), 1L, prefs)) {
                val appearances: List<Appearance> = updateCharacterAppearances(characterId)
                checkFKeysAppearance(appearances)
                val storyIds = appearances.map { it.story }
                updateStoriesCredits(storyIds)
                updateStoriesAppearances(storyIds)
            }
        }.await().let {
            Repository.saveTime(prefs, characterTag(characterId))
        }
    }

    /**
     * Update issue - updates stories, credits, appearances, and cover
     */
    internal fun updateIssue(issueId: Int) =
        CoroutineScope(nowDispatcher).launch {
            async {
                if (checkIfStale(issueTag(issueId), 1L, prefs)) {
                    Repository.savePrefValue(prefs = prefs,
                                             key = "${issueTag(issueId)}_STARTED",
                                             value = true)
                    Repository.savePrefValue(prefs = prefs,
                                             key = "${issueTag(issueId)}_STARTED_TIME",
                                             value = Instant.now().epochSecond)
                    updateIssueStoryDetails(issueId)
                    updateIssueCover(issueId)
                }
            }.await().let {
                Repository.saveTime(prefs, issueTag(issueId))
                Repository.savePrefValue(prefs = prefs,
                                         key = "${issueTag(issueId)}_STARTED",
                                         value = false)
                Repository.savePrefValue(prefs = prefs,
                                         key = "${issueTag(issueId)}_STARTED_TIME",
                                         value = "${Instant.MIN.epochSecond}")
            }
        }


    internal fun updateIssues(issueIds: List<Int>) {
        CoroutineScope(highPriorityDispatcher).launch {
            updateIssuesStoryDetails(issueIds)
            issueIds.forEach { updateIssueCover(it) }
        }
    }

    fun updateCreators(creatorIds: List<Int>) = creatorIds.forEach(this::updateCreator)

    /**
     * Gets nameDetails and credits for each creator
     */
    private fun updateCreator(creatorId: Int) = CoroutineScope(lowPriorityDispatcher).launch {
        async {
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
        }.await().let {
            Repository.saveTime(prefs, creatorTag(creatorId))
        }
    }


    /**
     * Update issue story details - updates stories, credits, and appearances for this issue
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

    private suspend fun getAppearancesByStoryIds(storyIds: List<Int>): List<Appearance>? =
        getItemsByArgument(storyIds, webservice::getAppearancesByStoryIds)

    private suspend fun getCreditsByStoryIds(storyIds: List<Int>): List<Credit>? =
        getItemsByArgument(storyIds, webservice::getCreditsByStoryIds)

    private suspend fun getExCreditsByStoryIds(storyIds: List<Int>): List<ExCredit>? =
        getItemsByArgument(storyIds, webservice::getExCreditsByStoryIds)

    private suspend fun getStoriesByIssueIds(issueIds: List<Int>): List<Story>? =
        getItemsByArgument(issueIds, webservice::getStoriesByIssues)

    private suspend fun getIssuesBySeriesId(seriesId: Int): List<Issue>? =
        getItemsByArgument(seriesId, webservice::getIssuesBySeries)

    private suspend fun getAppearancesByCharacterId(characterId: Int): List<Appearance>? =
        getItemsByArgument(characterId, webservice::getAppearances)

    private suspend fun getCreditsByNameDetailId(nameDetailId: Int): List<Credit>? =
        getItemsByArgument(listOf(nameDetailId), webservice::getCreditsByNameDetail)

    private suspend fun getCreditsByNameDetailIds(nameDetailIds: List<Int>): List<Credit>? =
        getItemsByArgument(nameDetailIds, webservice::getCreditsByNameDetail)

    private suspend fun getExCreditsByNameDetailIds(nameDetailIds: List<Int>): List<ExCredit>? =
        getItemsByArgument(nameDetailIds, webservice::getExCreditsByNameDetail)

    private suspend fun getAllSeriesBonds() {
        refreshAll<SeriesBond>(
            prefs = prefs,
            saveTag = UPDATED_BONDS,
            getItems = this::getSeriesBonds,
            followup = this::checkFKeysSeriesBond,
            dao = database.seriesBondDao()
        )
    }

    private suspend fun getAllCharacters() {
        refreshAllPaged<Character>(
            prefs = prefs,
            savePageTag = UPDATED_CHARACTERS_PAGE,
            saveTag = UPDATED_CHARACTERS,
            getItemsByPage = this::getCharactersByPage,
            dao = database.characterDao()
        )
    }

    private suspend fun getAllNameDetails() {
        refreshAllPaged<NameDetail>(
            prefs = prefs,
            savePageTag = UPDATED_NAME_DETAILS_PAGE,
            saveTag = UPDATED_NAME_DETAILS,
            getItemsByPage = this::getNameDetailsByPage,
            verifyForeignKeys = this::checkFKeysNameDetail,
            dao = database.nameDetailDao()
        )
    }

    private suspend fun getAllCreators() {
        refreshAllPaged<Creator>(
            prefs = prefs,
            savePageTag = UPDATED_CREATORS_PAGE,
            saveTag = UPDATED_CREATORS,
            getItemsByPage = this::getCreatorsByPage,
            dao = database.creatorDao()
        )
    }

    private suspend fun getAllSeries() {
        refreshAllPaged<Series>(
            prefs = prefs,
            savePageTag = UPDATED_SERIES_PAGE,
            saveTag = UPDATED_SERIES,
            getItemsByPage = this::getSeriesByPage,
            verifyForeignKeys = this::checkFKeysSeries,
            dao = database.seriesDao()
        )
    }

    private suspend fun getPublishers(): List<Publisher>? =
        getItems(prefs, webservice::getPublishers, UPDATED_PUBLISHERS)

    private suspend fun getRoles(): List<Role>? =
        getItems(prefs, webservice::getRoles, UPDATED_ROLES)

    private suspend fun getStoryTypes(): List<StoryType>? =
        getItems(prefs, webservice::getStoryTypes, UPDATED_STORY_TYPES)

    private suspend fun getBondTypes(): List<BondType>? =
        getItems(prefs, webservice::getBondTypes, UPDATED_BOND_TYPE)

    private suspend fun getSeriesBonds(): List<SeriesBond>? =
        getItems(prefs, webservice::getSeriesBonds, UPDATED_BONDS)

    private suspend fun getCharactersByPage(page: Int): List<Character>? =
        getItemsByArgument(page, webservice::getCharactersByPage)

    private suspend fun getSeriesByPage(page: Int): List<Series>? =
        getItemsByArgument(page, webservice::getSeriesByPage)

    private suspend fun getCreatorsByPage(page: Int): List<Creator>? =
        getItemsByArgument(page, webservice::getCreatorsByPage)

    private suspend fun getNameDetailsByPage(page: Int): List<NameDetail>? =
        getItemsByArgument(page, webservice::getNameDetailsByPage)

    private suspend fun updateIssueCover(issueId: Int) {
        database.issueDao().getIssueSus(issueId)?.let { issue ->
            if (issue.coverUri == null) {
                kotlin.runCatching {
                    val doc = Jsoup.connect(issue.issue.url).get()

                    val noCover = doc.getElementsByClass("no_cover").size == 1

                    val coverImgElements = doc.getElementsByClass("cover_img")
                    val wraparoundElements =
                        doc.getElementsByClass("wraparound_cover_img")

                    val elements = when {
                        coverImgElements.size > 0   -> coverImgElements
                        wraparoundElements.size > 0 -> wraparoundElements
                        else                        -> null
                    }

                    val src = elements?.get(0)?.attr("src")

                    val url = src?.let { URL(it) }

                    if (!noCover && url != null) {
                        val image = CoroutineScope(Dispatchers.IO).async {
                            url.toBitmap()
                        }

                        CoroutineScope(Dispatchers.Default).launch {
                            val bitmap = image.await()

                            bitmap?.let {
                                val savedUri: Uri? =
                                    it.saveToInternalStorage(
                                        context!!,
                                        issue.issue.coverFileName
                                    )

                                val cover =
                                    Cover(issue = issueId, coverUri = savedUri)
                                database.coverDao().upsertSus(listOf(cover))
                            }
                        }
                    } else if (noCover) {
                        val cover = Cover(issue = issueId, coverUri = null)
                        database.coverDao().upsertSus(cover)
                    } else {
                        Log.d(TAG, "COVER UPDATER No Cover Found")
                    }
                }
            }
        }
    }

    inner class AppearanceCollector : Collector<Appearance>(database.appearanceDao())
    inner class CreditCollector : Collector<Credit>(database.creditDao())
    inner class ExCreditCollector : Collector<ExCredit>(database.exCreditDao())
    inner class StoryCollector : Collector<Story>(database.storyDao())
    inner class IssueCollector : Collector<Issue>(database.issueDao())

    companion object {
        internal const val TAG = APP + "UpdateStatic"
    }
}

