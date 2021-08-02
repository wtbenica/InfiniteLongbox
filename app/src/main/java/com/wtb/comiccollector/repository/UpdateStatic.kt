package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.*
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
    database: IssueDatabase,
    prefs: SharedPreferences,
) : Updater(webservice, database, prefs) {

    internal val issueUpdater: IssueUpdater by lazy { IssueUpdater() }
    internal val characterUpdater: CharacterUpdater by lazy { CharacterUpdater() }
    internal val seriesUpdater: SeriesUpdater by lazy { SeriesUpdater() }
    internal val creatorUpdater: CreatorUpdater by lazy {
        CreatorUpdater(webservice,
                       database,
                       prefs)
    }

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

    internal suspend fun getPublishers(): List<Publisher>? =
        getItems(prefs, webservice::getPublishers, UPDATED_PUBLISHERS)

    internal suspend fun getRoles(): List<Role>? =
        getItems(prefs, webservice::getRoles, UPDATED_ROLES)

    internal suspend fun getStoryTypes(): List<StoryType>? =
        getItems(prefs, webservice::getStoryTypes, UPDATED_STORY_TYPES)

    internal suspend fun getBondTypes(): List<BondType>? =
        getItems(prefs, webservice::getBondTypes, UPDATED_BOND_TYPE)

    internal suspend fun getSeriesBonds(): List<SeriesBond>? =
        getItems(prefs, webservice::getSeriesBonds, UPDATED_BONDS)

    internal suspend fun getCharactersByPage(page: Int): List<Character>? =
        getItemsByArgument(page, webservice::getCharactersByPage)

    internal suspend fun getSeriesByPage(page: Int): List<Series>? =
        getItemsByArgument(page, webservice::getSeriesByPage)

    internal suspend fun getCreatorsByPage(page: Int): List<Creator>? =
        getItemsByArgument(page, webservice::getCreatorsByPage)

    internal suspend fun getNameDetailsByPage(page: Int): List<NameDetail>? =
        getItemsByArgument(page, webservice::getNameDetailsByPage)

    internal suspend fun getSeriesByIds(seriesIds: List<Int>): List<Series>? =
        getItemsByArgument(seriesIds, webservice::getSeriesByIds)

    //       internal suspend fun getIssuesByPage(page: Int): List<Issue>? =
//        getItemsByArgument(page, webservice::getIssuesByPage, Issue::class)
//
//    internal suspend fun getStoriesByPage(page: Int): List<Story>? =
//        getItemsByArgument(page, webservice::getStoriesByPage, Story::class)
//
//    internal suspend fun getAppearancesByPage(page: Int): List<Appearance>? =
//        getItemsByArgument(page, webservice::getAppearancesByPage, Appearance::class)
//
//    internal suspend fun getCreditsByPage(page: Int): List<Credit>? =
//        getItemsByArgument(page, webservice::getCreditsByPage, Credit::class)
//
//    internal suspend fun getExCreditsByPage(page: Int): List<ExCredit>? =
//        getItemsByArgument(page, webservice::getExCreditsByPage, ExCredit::class)
//

    fun updateIssue(issueId: Int) = issueUpdater.updateIssue(issueId)
    fun updateCharacter(characterId: Int) = characterUpdater.update(characterId)

    fun updateSeries(seriesId: Int) = seriesUpdater.update(seriesId)
    fun updateCreators(creatorIds: List<Int>) = creatorUpdater.update_new(creatorIds)

    inner class SeriesUpdater : Updater(webservice, database, prefs) {
        fun update(seriesId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                refreshById<Issue>(
                    prefs,
                    SERIES_TAG(seriesId),
                    this@SeriesUpdater::getIssuesBySeriesId,
                    this@StaticUpdater::checkFKeysIssue,
                    database.issueDao(),
                    seriesId
                )
            }
        }

        private suspend fun getIssuesBySeriesId(seriesId: Int): List<Issue>? =
            getItemsByArgument(seriesId, webservice::getIssuesBySeries)
    }

    /**
     * UpdateCharacter
     *
     * @property webservice
     * @property database
     * @property prefs
     * @constructor Create empty UpdateSeries
     */
    @ExperimentalCoroutinesApi
    inner class CharacterUpdater : Updater(webservice, database, prefs) {

        internal fun update(characterId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                refreshById<Appearance>(
                    prefs,
                    CHARACTER_TAG(characterId),
                    this@CharacterUpdater::getAppearancesByCharacterId,
                    this@StaticUpdater::checkFKeysAppearance,
                    database.appearanceDao(),
                    characterId
                )
            }
        }

        private suspend fun getAppearancesByCharacterId(characterId: Int): List<Appearance>? =
            getItemsByArgument(characterId, webservice::getAppearances)

//        private suspend fun getIssuesBySeries(seriesId: Int) =
//            supervisorScope {
//                runSafely("getIssuesBySeries: $seriesId", seriesId) {
//                    async { webservice.getIssuesBySeries(it).models }
//                } ?: emptyList()
//            }
//
//        private suspend fun getVariantsOf(issues: List<Issue>): List<Issue> =
//            supervisorScope {
//                val variantOfIds = issues.mapNotNull { it.variantOf }
//
//                runSafely("getIssues: variantsOf", variantOfIds) {
//                    async { webservice.getIssuesByIds(it).models }
//                } ?: emptyList()
//            }
//
//        private suspend fun getAppearancesByStory(stories: List<Story>) =
//            supervisorScope {
//                runSafely("getAppearancesByStory", stories.ids) {
//                    async { webservice.getAppearancesByStory(it).models }
//                } ?: emptyList()
//            }
//
//        private suspend fun getStoriesByIssues(issues: List<Issue>) =
//            supervisorScope {
//                runSafely("getStoriesByIssues", issues.ids) {
//                    async { webservice.getStoriesByIssues(it).models }
//                } ?: emptyList()
//            }
//
//        private suspend fun getSeriesIssuesAndVariantsOf(seriesId: Int) =
//            withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
//                val issues: List<Issue> = getIssuesBySeries(seriesId)
//                val variantsOf: List<Issue> = getVariantsOf(issues)
//
//                database.issueDao().upsert(variantsOf + issues)
//
//                issues
//            }
//
//        private suspend fun getStories(appearances: List<Appearance>): List<Story>? =
//            getItemsByArgument(appearances.map { it.story }, webservice::getStoriesByIds)
//
//        private suspend fun getIssuesByCharacterId(stories: List<Story>): List<Issue> =
//            supervisorScope {
//                val issueIds = stories.map { it.issueId }
//
//                runSafely("getIssues", issueIds) {
//                    async { webservice.getIssuesByIds(it).models }
//                } ?: emptyList()
//            }
//
//        private suspend fun getVariants(issues: List<Issue>): List<Issue> =
//            supervisorScope {
//                val variantOfIds = issues.mapNotNull { it.variantOf }
//
//                runSafely("getIssuesVariants", variantOfIds) {
//                    async { webservice.getIssuesByIds(it).models }
//                } ?: emptyList()
//            }
//
//        private suspend fun getCredits(stories: List<Story>): List<Credit> =
//            supervisorScope {
//                runSafely("getCreditsByStories", stories.ids) {
//                    async { webservice.getCreditsByStoryIds(it).models }
//                } ?: emptyList()
//            }
//
//        private suspend fun getNameDetails(credits: List<Credit>): List<NameDetail> =
//            supervisorScope {
//                val nameDetailIds = credits.map { it.nameDetailId }
//
//                runSafely("getNameDetailsByIds", nameDetailIds) {
//                    async { webservice.getNameDetailsByIds(nameDetailIds).models }
//                } ?: emptyList()
//            }
//
//        private suspend fun getExCredits(stories: List<Story>) =
//            supervisorScope {
//                runSafely("getCreditsByStories", stories.ids) {
//                    async { webservice.getExtractedCreditsByStories(it).models }
//                } ?: emptyList()
//            }
//
//        private suspend fun getExNameDetails(exCredits: List<ExCredit>): List<NameDetail> =
//            supervisorScope {
//                val exNameDetailIds = exCredits.map { it.nameDetailId }
//
//                runSafely(
//                    "getExNameDetailsByIds",
//                    exNameDetailIds
//                ) {
//                    async { webservice.getNameDetailsByIds(exNameDetailIds).models }
//                } ?: emptyList()
//            }
    }

    inner class IssueUpdater {
        fun updateIssue(issueId: Int) {
            Log.d(TAG, "updateIssue: $issueId")
            updateStoryDetails(issueId)
            updateCover(issueId)
        }

        internal fun updateStoryDetails(issueId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
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
                        getItemsByArgument(stories.ids, webservice::getCreditsByStoryIds)
                            ?: emptyList()
                    checkFKeysCredit(credits)
                    database.creditDao().upsert(credits)
                }

                var excredits: List<ExCredit> =
                    database.exCreditDao().getExCreditsByStoryIds(stories.ids)
                if (excredits.isEmpty()) {
                    excredits =
                        getItemsByArgument(stories.ids,
                                           webservice::getExtractedCreditsByStories) ?: emptyList()

                    checkFKeysCredit(excredits)
                    database.exCreditDao().upsert(excredits)
                }

                var appearances: List<Appearance> =
                    database.appearanceDao().getAppearancesByStoryIds(stories.ids)
                if (appearances.isEmpty()) {
                    appearances =
                        getItemsByArgument(stories.ids, webservice::getAppearancesByStory)
                            ?: emptyList()
                    checkFKeysAppearance(appearances)
                    database.appearanceDao().upsert(appearances)
                }
            }
        }

        internal fun updateCover(issueId: Int) {
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
        internal const val TAG = APP + "UpdateStatic"

//        /**
//         * Get items - checks if stale and retrieves items
//         * @return null on connection error
//         */
//        internal suspend fun <GcdType : GcdJson<ModelType>, ModelType : DataModel> getItems(
//            prefs: SharedPreferences,
//            apiCall: KSuspendFunction0<List<Item<GcdType, ModelType>>>,
//            saveTag: String,
//        ): List<ModelType>? =
//            supervisorScope {
//                if (checkIfStale(saveTag, WEEKLY, prefs)) {
//                    runSafely("getItems: ${apiCall.name}") {
//                        async { apiCall() }
//                    }?.models
//                } else {
//                    emptyList()
//                }
//            }
//
//        /**
//         * Get all paged - calls getItemsByPage , performs verifyForeignKeys on each page, then
//         * saves each page to dao
//         */
//        internal suspend fun <ModelType : DataModel> refreshAllPaged(
//            prefs: SharedPreferences,
//            savePageTag: String,
//            saveTag: String,
//            getItemsByPage: suspend (Int) -> List<ModelType>?,
//            verifyForeignKeys: suspend (List<ModelType>) -> Unit = {},
//            dao: BaseDao<ModelType>,
//        ) {
//            if (checkIfStale(saveTag, WEEKLY, prefs)) {
//                var page = prefs.getInt(savePageTag, 0)
//                var stop = false
//                var success = true
//
//                do {
//                    coroutineScope {
//                        val itemPage: List<ModelType>? = getItemsByPage(page)
//
//                        if (itemPage != null && itemPage.isNotEmpty()) {
//                            verifyForeignKeys(itemPage)
//                            dao.upsertSus(itemPage)
//                            Repository.savePrefValue(prefs, savePageTag, page)
//                        } else if (itemPage != null) {
//                            stop = true
//                        } else {
//                            stop = true
//                            success = false
//                        }
//                    }
//
//                    page += 1
//                } while (!stop)
//
//                if (success) {
//                    Repository.savePrefValue(prefs, savePageTag, 0)
//                    Repository.saveTime(prefs, saveTag)
//                }
//            }
//        }
//
//        /**
//         * Refresh all - gets items, performs followup, then saves using dao
//         */
//        internal suspend fun <ModelType : DataModel> refreshAll(
//            prefs: SharedPreferences,
//            saveTag: String,
//            getItems: suspend () -> List<ModelType>?,
//            followup: suspend (List<ModelType>) -> Unit = {},
//            dao: BaseDao<ModelType>,
//        ) {
//            if (checkIfStale(saveTag, WEEKLY, prefs)) {
//                coroutineScope {
//                    val items: List<ModelType>? = getItems()
//
//                    if (items != null && items.isNotEmpty()) {
//                        followup(items)
//                        dao.upsertSus(items)
//                        Repository.saveTime(prefs, saveTag)
//                    }
//                }
//            }
//        }
//
//        @ExperimentalCoroutinesApi
//        internal fun <ModelType : DataModel> saveTimeIfNotEmpty(
//            prefs: SharedPreferences,
//            items: List<ModelType>?,
//            saveTag: String,
//        ): List<ModelType>? =
//            items.also {
//                if (it != null && it.isNotEmpty()) {
//                    Repository.saveTime(prefs, saveTag)
//                }
//            }
//
//        /**
//         * Get missing foreign key models
//         */
//        internal suspend fun <M : DataModel, FG : GcdJson<FM>, FM : DataModel>
//                checkForMissingForeignKeyModels(
//            itemsToCheck: List<M>,
//            getForeignKey: (M) -> Int?,
//            foreignKeyDao: BaseDao<FM>,
//            getFkItems: KSuspendFunction1<List<Int>, List<Item<FG, FM>>>,
//            fkFollowup: (suspend (List<FM>) -> Unit)? = null,
//        ) {
//            val fkIds: List<Int> = itemsToCheck.mapNotNull { getForeignKey(it) }
//            val missingIds: List<Int> = fkIds.mapNotNull {
//                if (foreignKeyDao.get(it) == null) it else null
//            }
//
//            val missingItems = getItemsByArgument(missingIds, getFkItems)
//
//            if (missingItems != null && missingItems.isNotEmpty()) {
//                fkFollowup?.let { it(missingItems) }
//                foreignKeyDao.upsert(missingItems)
//            }
//        }
    }
}

