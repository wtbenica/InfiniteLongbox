package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.repository.StaticUpdater.PriorityDispatcher.Companion.highPriorityDispatcher
import com.wtb.comiccollector.repository.StaticUpdater.PriorityDispatcher.Companion.lowPriorityDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import org.jsoup.Jsoup
import java.net.URL

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
    fun updateSeries(seriesId: Int) = CoroutineScope(highPriorityDispatcher).launch {
        async {
            if (checkIfStale(seriesTag(seriesId), 1L, prefs)) {
                val issues: List<Issue> = updateSeriesIssues(seriesId)
                issues.ids.forEach { updateIssue(it) }
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
                appearances.forEach {
                    updateStoryCredits(it.story)
                    updateStoryAppearances(it.story)
                }
            }
        }.await().let {
            Repository.saveTime(prefs, characterTag(characterId))
        }
    }


    class PriorityDispatcher {
        companion object {
            private var HP_INSTANCE: CoroutineDispatcher? = null
            private var LP_INSTANCE: CoroutineDispatcher? = null

            val highPriorityDispatcher: CoroutineDispatcher
                get() {
                    val thread = HandlerThread("highPriorityThread",
                                               android.os.Process.THREAD_PRIORITY_FOREGROUND).also {
                        it.start()
                    }
                    val res = HP_INSTANCE
                        ?: Handler(thread.looper).asCoroutineDispatcher("highPriorityDisptcher")
                    if (HP_INSTANCE == null) {
                        HP_INSTANCE = res
                    }
                    return HP_INSTANCE!!
                }

            val lowPriorityDispatcher: CoroutineDispatcher
                get() {
                    val thread = HandlerThread("lowPriorityThread",
                                               android.os.Process.THREAD_PRIORITY_BACKGROUND).also {
                        it.start()
                    }
                    val res = LP_INSTANCE
                        ?: Handler(thread.looper).asCoroutineDispatcher("lowPriorityDisptcher")
                    if (LP_INSTANCE == null) {
                        LP_INSTANCE = res
                    }
                    return LP_INSTANCE!!
                }
        }
    }


    /**
     * Update issue - updates stories, credits, appearances, and cover
     */
    private fun updateIssue(issueId: Int) = CoroutineScope(highPriorityDispatcher).launch {
        async {
            if (checkIfStale(issueTag(issueId), 1L, prefs)) {
                Log.d(TAG, "Updating issue: $issueId")
                updateIssueStoryDetails(issueId)
                updateIssueCover(issueId)
            }
        }.await().let {
            Repository.saveTime(prefs, issueTag(issueId))
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
                val credits: List<CreditX> = nameDetails.flatMap { nameDetail ->
                    updateNameDetailCredits(nameDetail.id) +
                            updateNameDetailExCredits(nameDetail.id)
                }
                checkFKeysCredit(credits)
                for (credit in credits) {
                    updateStoryCredits(credit.story)
                    updateStoryAppearances(credit.story)
                }
            }
        }.await().let {
            Repository.saveTime(prefs, creatorTag(creatorId))
        }
    }


    /**
     * Update issue story details - updates stories, credits, and appearances for this issue
     */
    private suspend fun updateIssueStoryDetails(issueId: Int) {
        val stories: List<Story> = updateIssueStories(issueId)
        stories.ids.map { updateStoryCredits(it) }
        stories.ids.map { updateStoryAppearances(it) }
    }

    private suspend fun updateStoryAppearances(storyId: Int): List<Appearance> =
        updateById<Appearance>(prefs,
                               null,
                               ::getAppearancesByStoryId,
                               storyId,
                               ::checkFKeysAppearance,
                               database.appearanceDao())

    private suspend fun updateStoryCredits(storyId: Int): List<CreditX> =
        updateById<Credit>(prefs,
                           null,
                           ::getCreditsByStoryId,
                           storyId,
                           ::checkFKeysCredit,
                           database.creditDao()) +
                updateById<ExCredit>(prefs,
                                     null,
                                     ::getExCreditsByStoryId,
                                     storyId,
                                     ::checkFKeysCredit,
                                     database.exCreditDao())

    private suspend fun updateIssueStories(issueId: Int): List<Story> =
        updateById<Story>(prefs,
                          null,
                          ::getStoriesByIssueId,
                          issueId,
                          ::checkFKeysStory,
                          database.storyDao())

    private suspend fun updateSeriesIssues(seriesId: Int) =
        updateById<Issue>(prefs,
                          ::seriesTag,
                          ::getIssuesBySeriesId,
                          seriesId,
                          ::checkFKeysIssue,
                          database.issueDao())

    private suspend fun updateCharacterAppearances(characterId: Int) =
        updateById<Appearance>(prefs,
                               ::characterTag,
                               ::getAppearancesByCharacterId,
                               characterId,
                               ::checkFKeysAppearance,
                               database.appearanceDao())

    private suspend fun updateNameDetailCredits(nameDetailId: Int) =
        updateById<Credit>(
            prefs,
            null,
            ::getCreditsByNameDetailId,
            nameDetailId,
            ::checkFKeysCredit,
            database.creditDao())

    private suspend fun updateNameDetailExCredits(nameDetailId: Int) =
        updateById<ExCredit>(
            prefs,
            null,
            ::getExCreditsByNameDetailId,
            nameDetailId,
            ::checkFKeysCredit,
            database.exCreditDao())

    private suspend fun getAppearancesByStoryId(storyId: Int): List<Appearance>? =
        getItemsByArgument(storyId, webservice::getAppearancesByStoryId)

    private suspend fun getCreditsByStoryId(storyId: Int): List<Credit>? =
        getItemsByArgument(storyId, webservice::getCreditsByStoryId)

    private suspend fun getExCreditsByStoryId(storyId: Int): List<ExCredit>? =
        getItemsByArgument(storyId, webservice::getExCreditsByStoryId)

    private suspend fun getStoriesByIssueId(issueId: Int): List<Story>? =
        getItemsByArgument(issueId, webservice::getStoriesByIssue)

    private suspend fun getIssuesBySeriesId(seriesId: Int): List<Issue>? =
        getItemsByArgument(seriesId, webservice::getIssuesBySeries)

    private suspend fun getAppearancesByCharacterId(characterId: Int): List<Appearance>? =
        getItemsByArgument(characterId, webservice::getAppearances)

    private suspend fun getCreditsByNameDetailId(nameDetailId: Int): List<Credit>? =
        getItemsByArgument(listOf(nameDetailId), webservice::getCreditsByNameDetail)

    private suspend fun getExCreditsByNameDetailId(nameDetailId: Int): List<ExCredit>? =
        getItemsByArgument(listOf(nameDetailId), webservice::getExCreditsByNameDetail)

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

        getAllSeries()
        getAllCreators()
        getAllNameDetails()
        getAllCharacters()
        getAllSeriesBonds()
    }

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

    companion object {
        internal const val TAG = APP + "UpdateStatic"
    }
}

