package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.daos.BaseDao
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.net.URL
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1

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
    private val database: IssueDatabase,
    private val prefs: SharedPreferences,
) : Updater() {

    private val issueUpdater: IssueUpdater by lazy { IssueUpdater() }

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

        refreshAllPaged<Series>(
            prefs = prefs,
            savePageTag = UPDATED_SERIES_PAGE,
            saveTag = UPDATED_SERIES,
            getItemsByPage = this::getSeriesByPage,
            verifyForeignKeys = this::checkFKeysSeries,
            dao = database.seriesDao()
        )

        refreshAllPaged<Creator>(
            prefs = prefs,
            savePageTag = UPDATED_CREATORS_PAGE,
            saveTag = UPDATED_CREATORS,
            getItemsByPage = this::getCreatorsByPage,
            dao = database.creatorDao()
        )

        refreshAllPaged<NameDetail>(
            prefs = prefs,
            savePageTag = UPDATED_NAME_DETAILS_PAGE,
            saveTag = UPDATED_NAME_DETAILS,
            getItemsByPage = this::getNameDetailsByPage,
            verifyForeignKeys = this::checkFKeysNameDetail,
            dao = database.nameDetailDao()
        )

        refreshAllPaged<Character>(
            prefs = prefs,
            savePageTag = UPDATED_CHARACTERS_PAGE,
            saveTag = UPDATED_CHARACTERS,
            getItemsByPage = this::getCharactersByPage,
            dao = database.characterDao()
        )

        refreshAll<SeriesBond>(
            prefs = prefs,
            saveTag = UPDATED_BONDS,
            getItems = this::getSeriesBonds,
            followup = this::checkFKeysSeriesBond,
            dao = database.seriesBondDao()
        )

//        refreshPaged<Issue>(
//            prefs = prefs, savePageTag = UPDATED_ISSUES_PAGE,
//            saveTag = UPDATED_ISSUES,
//            getItemsByPage = this::getIssuesByPage,
//            dao = database.issueDao(),
//            followup = this::checkFKeysIssue
//        )
//
//        refreshPaged<Story>(
//            prefs = prefs, savePageTag = UPDATED_STORIES_PAGE,
//            saveTag = UPDATED_STORIES,
//            getItemsByPage = this::getStoriesByPage,
//            dao = database.storyDao(),
//            followup = this::checkFKeysStory
//        )
//
//        refreshPaged<Appearance>(
//            prefs = prefs, savePageTag = UPDATED_APPEARANCES_PAGE,
//            saveTag = UPDATED_APPEARANCES,
//            getItemsByPage = this::getAppearancesByPage,
//            dao = database.appearanceDao(),
//            followup = this::checkFKeysAppearance
//        )
//
//        Log.d(TAG, "Updating Credit")
//        refreshPaged<Credit>(
//            prefs = prefs, savePageTag = UPDATED_CREDITS_PAGE,
//            saveTag = UPDATED_CREDITS,
//            getItemsByPage = this::getCreditsByPage,
//            dao = database.creditDao(),
//            followup = this::checkFKeysCredit
//        )
//
//        refreshPaged<ExCredit>(
//            prefs = prefs, savePageTag = UPDATED_EXCREDITS_PAGE,
//            saveTag = UPDATED_EXCREDITS,
//            getItemsByPage = this::getExCreditsByPage,
//            dao = database.exCreditDao()
//        )
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

    private suspend fun getSeriesByIds(seriesIds: List<Int>): List<Series>? =
        getItemsByArgument(seriesIds, webservice::getSeriesByIds)

    //       private suspend fun getIssuesByPage(page: Int): List<Issue>? =
//        getItemsByArgument(page, webservice::getIssuesByPage, Issue::class)
//
//    private suspend fun getStoriesByPage(page: Int): List<Story>? =
//        getItemsByArgument(page, webservice::getStoriesByPage, Story::class)
//
//    private suspend fun getAppearancesByPage(page: Int): List<Appearance>? =
//        getItemsByArgument(page, webservice::getAppearancesByPage, Appearance::class)
//
//    private suspend fun getCreditsByPage(page: Int): List<Credit>? =
//        getItemsByArgument(page, webservice::getCreditsByPage, Credit::class)
//
//    private suspend fun getExCreditsByPage(page: Int): List<ExCredit>? =
//        getItemsByArgument(page, webservice::getExCreditsByPage, ExCredit::class)
//

    private suspend fun checkFKeysSeries(series: List<Series>) {
        checkSeriesFkPublisher(series)
    }

    private suspend fun checkSeriesFkPublisher(series: List<Series>) =
        updateMissingForeignKeyModels(series,
                                      Series::publisherId,
                                      database.publisherDao(),
                                      webservice::getPublishersByIds)

    private suspend fun checkFKeysNameDetail(nameDetails: List<NameDetail>) {
        checkNameDetailFkCreator(nameDetails)
    }

    private suspend fun checkNameDetailFkCreator(nameDetails: List<NameDetail>) =
        updateMissingForeignKeyModels(nameDetails,
                                      NameDetail::creatorId,
                                      database.creatorDao(),
                                      webservice::getCreatorsByIds)


    private suspend fun checkFKeysIssue(issues: List<Issue>) {
        checkIssueFkSeries(issues)
        checkIssueFkVariantOf(issues)
    }

    private suspend fun checkIssueFkSeries(issues: List<Issue>) =
        updateMissingForeignKeyModels(issues,
                                      Issue::seriesId,
                                      database.seriesDao(),
                                      webservice::getSeriesByIds,
                                      this@StaticUpdater::checkFKeysSeries)

    private suspend fun checkIssueFkVariantOf(issues: List<Issue>) =
        updateMissingForeignKeyModels(issues,
                                      Issue::variantOf,
                                      database.issueDao(),
                                      webservice::getIssuesByIds,
                                      this@StaticUpdater::checkFKeysIssue)

    private suspend fun checkSeriesBondFkOriginIssue(seriesBonds: List<SeriesBond>) =
        updateMissingForeignKeyModels(seriesBonds,
                                      SeriesBond::originIssueId,
                                      database.issueDao(),
                                      webservice::getIssuesByIds,
                                      this@StaticUpdater::checkFKeysIssue)

    private suspend fun checkSeriesBondFkOriginSeries(seriesBonds: List<SeriesBond>) =
        updateMissingForeignKeyModels(seriesBonds,
                                      SeriesBond::originId,
                                      database.seriesDao(),
                                      webservice::getSeriesByIds,
                                      this@StaticUpdater::checkFKeysSeries)

    private suspend fun checkSeriesBondFkTargetSeries(seriesBonds: List<SeriesBond>) =
        updateMissingForeignKeyModels(seriesBonds,
                                      SeriesBond::targetId,
                                      database.seriesDao(),
                                      webservice::getSeriesByIds,
                                      this@StaticUpdater::checkFKeysSeries)

    private suspend fun checkSeriesBondFkTargetIssue(seriesBonds: List<SeriesBond>) =
        updateMissingForeignKeyModels(seriesBonds,
                                      SeriesBond::targetIssueId,
                                      database.issueDao(),
                                      webservice::getIssuesByIds,
                                      this@StaticUpdater::checkFKeysIssue)


    private suspend fun checkFKeysSeriesBond(bonds: List<SeriesBond>) {
        checkSeriesBondFkOriginIssue(bonds)
        checkSeriesBondFkOriginSeries(bonds)
        checkSeriesBondFkTargetIssue(bonds)
        checkSeriesBondFkTargetSeries(bonds)
    }

    private suspend fun checkFKeysStory(stories: List<Story>) {
        checkStoryFkIssue(stories)
    }

    private suspend fun checkStoryFkIssue(stories: List<Story>) =
        updateMissingForeignKeyModels(stories,
                                      Story::issueId,
                                      database.issueDao(),
                                      webservice::getIssuesByIds,
                                      this@StaticUpdater::checkFKeysIssue)

    private suspend fun checkFKeysAppearance(appearances: List<Appearance>) {
        checkAppearanceFkCharacter(appearances)
        checkAppearanceFkStory(appearances)
    }

    private suspend fun checkAppearanceFkCharacter(appearances: List<Appearance>) =
        updateMissingForeignKeyModels(appearances,
                                      Appearance::character,
                                      database.characterDao(),
                                      webservice::getCharactersByIds)

    private suspend fun checkAppearanceFkStory(appearances: List<Appearance>) =
        updateMissingForeignKeyModels(appearances,
                                      Appearance::story,
                                      database.storyDao(),
                                      webservice::getStoriesByIds,
                                      this@StaticUpdater::checkFKeysStory)

    private suspend fun <T : CreditX> checkCreditFkNameDetail(credits: List<T>) =
        updateMissingForeignKeyModels(credits,
                                      CreditX::nameDetailId,
                                      database.nameDetailDao(),
                                      webservice::getNameDetailsByIds,
                                      this@StaticUpdater::checkFKeysNameDetail)

    private suspend fun <T : CreditX> checkCreditFkStory(credits: List<T>) =
        updateMissingForeignKeyModels(credits,
                                      CreditX::storyId,
                                      database.storyDao(),
                                      webservice::getStoriesByIds,
                                      this@StaticUpdater::checkFKeysStory)

    private suspend fun <T : CreditX> checkFKeysCredit(credits: List<T>) {
        checkCreditFkNameDetail(credits)
        checkCreditFkStory(credits)
    }

    fun updateIssue(issueId: Int) = issueUpdater.updateIssue(issueId)

    inner class IssueUpdater {
        fun updateIssue(issueId: Int) {
            Log.d(TAG, "updateIssue: $issueId")
            updateStoryDetails(issueId)
            updateCover(issueId)
        }

        private fun updateStoryDetails(issueId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                Log.d(TAG, "This is BASE_URL: $BASE_URL")
                var stories: List<Story> = database.storyDao().getStories(issueId)
                if (stories.isEmpty()) {
                    stories =
                        getItemsByArgument(issueId, webservice::getStoriesByIssue) ?: emptyList()
                    checkFKeysStory(stories)
                    database.storyDao().upsert(stories)
                }

                var credits: List<Credit> = database.creditDao().getCreditsByStoryIds(stories.ids)
                if (credits.isEmpty()) {
                    credits =
                        getItemsByArgument(listOf(issueId), webservice::getCreditsByStoryIds)
                            ?: emptyList()
                    checkFKeysCredit(credits)
                    database.creditDao().upsert(credits)
                }

                var excredits: List<ExCredit> =
                    database.exCreditDao().getExCreditsByStoryIds(stories.ids)
                if (excredits.isEmpty()) {
                    excredits =
                        getItemsByArgument(listOf(issueId),
                                           webservice::getExtractedCreditsByStories) ?: emptyList()

                    checkFKeysCredit(excredits)
                    database.exCreditDao().upsert(excredits)
                }

                var appearances: List<Appearance> =
                    database.appearanceDao().getAppearancesByStoryIds(stories.ids)
                if (appearances.isEmpty()) {
                    appearances =
                        getItemsByArgument(listOf(issueId), webservice::getAppearancesByStory)
                            ?: emptyList()
                    checkFKeysAppearance(appearances)
                    database.appearanceDao().upsert(appearances)
                }
            }
        }

        private fun updateCover(issueId: Int) {
            if (checkIfStale(ISSUE_TAG(issueId), ISSUE_LIFETIME, prefs)) {
                CoroutineScope(Dispatchers.IO).launch {
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
                                                Cover(issueId = issueId, coverUri = savedUri)
                                            database.coverDao().upsertSus(listOf(cover))
                                        }
                                    }
                                } else if (noCover) {
                                    val cover = Cover(issueId = issueId, coverUri = null)
                                    database.coverDao().upsertSus(cover)
                                } else {
                                    Log.d(TAG, "COVER UPDATER No Cover Found")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Get items by argument
         *
         * @return null on connection error
         */
        private suspend fun <GcdType : GcdJson<ModelType>, ModelType : DataModel, ArgType : Any> getItemsByArgument(
            arg: ArgType,
            apiCall: KSuspendFunction1<ArgType, List<Item<GcdType, ModelType>>>,
        ): List<ModelType>? =
            supervisorScope {
                runSafely("getItemsByArgument: ${apiCall.name}", arg) {
                    async { apiCall(it) }
                }?.models
            }

        /**
         * Get items - checks if stale and retrieves items
         * @return null on connection error
         */
        private suspend fun <GcdType : GcdJson<ModelType>, ModelType : DataModel> getItems(
            prefs: SharedPreferences,
            apiCall: KSuspendFunction0<List<Item<GcdType, ModelType>>>,
            saveTag: String,
        ): List<ModelType>? =
            supervisorScope {
                if (checkIfStale(saveTag, WEEKLY, prefs)) {
                    runSafely("getItems: ${apiCall.name}") {
                        async { apiCall() }
                    }?.models
                } else {
                    emptyList()
                }
            }

        /**
         * Get all paged - calls getItemsByPage , performs verifyForeignKeys on each page, then
         * saves each page to dao
         */
        private suspend fun <ModelType : DataModel> refreshAllPaged(
            prefs: SharedPreferences,
            savePageTag: String,
            saveTag: String,
            getItemsByPage: suspend (Int) -> List<ModelType>?,
            verifyForeignKeys: suspend (List<ModelType>) -> Unit = {},
            dao: BaseDao<ModelType>,
        ) {
            if (checkIfStale(saveTag, WEEKLY, prefs)) {
                var page = prefs.getInt(savePageTag, 0)
                var stop = false
                var success = true

                do {
                    coroutineScope {
                        val itemPage: List<ModelType>? = getItemsByPage(page)

                        if (itemPage != null && itemPage.isNotEmpty()) {
                            verifyForeignKeys(itemPage)
                            dao.upsertSus(itemPage)
                            Repository.savePrefValue(prefs, savePageTag, page)
                        } else if (itemPage != null) {
                            stop = true
                        } else {
                            stop = true
                            success = false
                        }
                    }

                    page += 1
                } while (!stop)

                if (success) {
                    Repository.savePrefValue(prefs, savePageTag, 0)
                    Repository.saveTime(prefs, saveTag)
                }
            }
        }

        /**
         * Refresh all - gets items, performs followup, then saves using dao
         */
        private suspend fun <ModelType : DataModel> refreshAll(
            prefs: SharedPreferences,
            saveTag: String,
            getItems: suspend () -> List<ModelType>?,
            followup: suspend (List<ModelType>) -> Unit = {},
            dao: BaseDao<ModelType>,
        ) {
            if (checkIfStale(saveTag, WEEKLY, prefs)) {
                coroutineScope {
                    val items: List<ModelType>? = getItems()

                    if (items != null && items.isNotEmpty()) {
                        followup(items)
                        dao.upsertSus(items)
                        Repository.saveTime(prefs, saveTag)
                    }
                }
            }
        }

        @ExperimentalCoroutinesApi
        private fun <ModelType : DataModel> saveTimeIfNotEmpty(
            prefs: SharedPreferences,
            items: List<ModelType>?,
            saveTag: String,
        ): List<ModelType>? =
            items.also {
                if (it != null && it.isNotEmpty()) {
                    Repository.saveTime(prefs, saveTag)
                }
            }

        /**
         * Get missing foreign key models
         */
        private suspend fun <
                ModelType : DataModel,
                GcdFkType : GcdJson<FKModel>,
                FKModel : DataModel,
                > updateMissingForeignKeyModels(
            items: List<ModelType>,
            getFkId: (ModelType) -> Int?,
            fkDao: BaseDao<FKModel>,
            getItems: KSuspendFunction1<List<Int>, List<Item<GcdFkType, FKModel>>>,
            followup: (suspend (List<FKModel>) -> Unit)? = null,
        ) {
            val fkIds: List<Int> = items.mapNotNull { getFkId(it) }
            val missingIds: List<Int> = fkIds.mapNotNull {
                if (fkDao.get(it) == null) it else null
            }

            val missingItems = getItemsByArgument(missingIds, getItems)

            if (missingItems != null && missingItems.isNotEmpty()) {
                followup?.let { it(missingItems) }
                fkDao.upsert(missingItems)
            }
        }
    }
}