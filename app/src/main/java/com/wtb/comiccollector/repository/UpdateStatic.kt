package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.daos.BaseDao
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlin.reflect.KClass

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
    /**
     *  Updates publisher, series, role, and storytype tables
     */
    @ExperimentalCoroutinesApi
    internal suspend fun updateAsync() {
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

        if (!DEBUG) {
            refresh<Series>(
                prefs = prefs, savePageTag = UPDATED_SERIES_PAGE,
                saveTag = UPDATED_SERIES,
                getItemsByPage = this@StaticUpdater::refreshSeriesByPage,
                dao = database.seriesDao()
            )

            refresh<Character>(
                prefs = prefs, savePageTag = UPDATED_CHARACTERS_PAGE,
                saveTag = UPDATED_CHARACTERS,
                getItemsByPage = this@StaticUpdater::refreshCharactersByPage,
                dao = database.characterDao()
            )

            refresh<Creator>(
                prefs = prefs, savePageTag = UPDATED_CREATORS_PAGE,
                saveTag = UPDATED_CREATORS,
                getItemsByPage = this@StaticUpdater::refreshCreatorsByPage,
                dao = database.creatorDao()
            )

            val bonds: List<SeriesBond>? = getSeriesBonds()
            if (bonds != null) {
                database.seriesBondDao().upsert(bonds)
                Repository.saveTime(prefs, UPDATED_BONDS)
            }

            refresh<NameDetail>(
                prefs = prefs, savePageTag = UPDATED_NAME_DETAILS_PAGE,
                saveTag = UPDATED_NAME_DETAILS,
                getItemsByPage = this@StaticUpdater::refreshNameDetailsByPage,
                dao = database.nameDetailDao(),
                followup = this@StaticUpdater::checkFKeysNameDetail
            )

            refresh<Issue>(
                prefs = prefs, savePageTag = UPDATED_ISSUES_PAGE,
                saveTag = UPDATED_ISSUES,
                getItemsByPage = this@StaticUpdater::refreshIssuesByPage,
                dao = database.issueDao(),
                followup = this@StaticUpdater::checkFKeysIssue
            )
        }

        refresh<Story>(
            prefs = prefs, savePageTag = UPDATED_STORIES_PAGE,
            saveTag = UPDATED_STORIES,
            getItemsByPage = this@StaticUpdater::refreshStoriesByPage,
            dao = database.storyDao(),
            followup = this@StaticUpdater::checkFKeysStory
        )

        refresh<Appearance>(
            prefs = prefs, savePageTag = UPDATED_APPEARANCES_PAGE,
            saveTag = UPDATED_APPEARANCES,
            getItemsByPage = this@StaticUpdater::refreshAppearancesByPage,
            dao = database.appearanceDao(),
            followup = this@StaticUpdater::checkFKeysAppearance
        )

        refresh<Credit>(
            prefs = prefs, savePageTag = UPDATED_CREDITS_PAGE,
            saveTag = UPDATED_CREDITS,
            getItemsByPage = this@StaticUpdater::refreshCreditsByPage,
            dao = database.creditDao(),
            followup = this@StaticUpdater::checkFKeysCredit
        )

        refresh<ExCredit>(
            prefs = prefs, savePageTag = UPDATED_EXCREDITS_PAGE,
            saveTag = UPDATED_EXCREDITS,
            getItemsByPage = this@StaticUpdater::refreshExCreditsByPage,
            dao = database.exCreditDao()
        )
    }

    private suspend fun getPublishers(): List<Publisher> = supervisorScope {
        if (checkIfStale(UPDATED_PUBLISHERS, MONTHLY, prefs) && !DEBUG) {
            Log.d(TAG, "getPublishers STALE")
            runSafely("update: Getting Publishers") {
                async { webservice.getPublishers() }
            }?.models ?: emptyList()
        } else {
            Log.d(TAG, "getPublishers NOT STALE")
            emptyList()
        }
    }

    private suspend fun getRoles(): List<Role> = supervisorScope {
        if (checkIfStale(UPDATED_ROLES, MONTHLY, prefs) && !DEBUG) {
            Log.d(TAG, "getRoles STALE")
            runSafely("getRoles") {
                async { webservice.getRoles() }
            }?.models ?: emptyList()
        } else {
            Log.d(TAG, "getRoles NOT STALE")
            emptyList()
        }
    }

    private suspend fun getStoryTypes(): List<StoryType> = supervisorScope {
        if (checkIfStale(UPDATED_STORY_TYPES, MONTHLY, prefs) && !DEBUG) {
            Log.d(TAG, "getStoryTypes STALE")
            runSafely("getStoryTypes") {
                async { webservice.getStoryTypes() }
            }?.models ?: emptyList()
        } else {
            Log.d(TAG, "getStoryTypes NOT STALE")
            emptyList()
        }
    }

    private suspend fun getBondTypes(): List<BondType> = supervisorScope {
        if (checkIfStale(UPDATED_BOND_TYPE, MONTHLY, prefs) && !DEBUG) {
            Log.d(TAG, "getBondTypes STALE")
            runSafely("getBondTypes") {
                async { webservice.getBondTypes() }
            }?.models ?: emptyList()
        } else {
            Log.d(TAG, "getBondTypes NOT STALE")
            emptyList()
        }
    }

    private suspend fun getSeriesBonds(): List<SeriesBond>? = supervisorScope {
        runSafely("getSeriesBonds") {
            async { webservice.getSeriesBonds() }
        }?.models
    }

    private suspend fun refreshIssuesByPage(page: Int): List<Issue>? =
        getItemsByPage(
            page,
            webservice::getIssuesByPage,
            Issue::class
        )

    private suspend fun refreshStoriesByPage(page: Int): List<Story>? =
        getItemsByPage(page, webservice::getStoriesByPage, Story::class)

    private suspend fun refreshCharactersByPage(page: Int): List<Character>? =
        getItemsByPage(page, webservice::getCharactersByPage, Character::class)

    private suspend fun refreshSeriesByPage(page: Int): List<Series>? =
        getItemsByPage(page, webservice::getSeriesByPage, Series::class)

    private suspend fun refreshCreatorsByPage(page: Int): List<Creator>? =
        getItemsByPage(page, webservice::getCreatorsByPage, Creator::class)

    private suspend fun refreshAppearancesByPage(page: Int): List<Appearance>? =
        getItemsByPage(page, webservice::getAppearancesByPage, Appearance::class)

    private suspend fun refreshCreditsByPage(page: Int): List<Credit>? =
        getItemsByPage(page, webservice::getCreditsByPage, Credit::class)

    private suspend fun refreshExCreditsByPage(page: Int): List<ExCredit>? =
        getItemsByPage(page, webservice::getExCreditsByPage, ExCredit::class)

    private suspend fun refreshNameDetailsByPage(page: Int): List<NameDetail>? =
        getItemsByPage(page, webservice::getNameDetailsByPage, NameDetail::class)

    private suspend fun checkFKeysNameDetail(nameDetails: List<NameDetail>) {
        val creatorIds: List<Int> = nameDetails.map { it.creatorId }
        val missingIds =
            creatorIds.mapNotNull { if (database.creatorDao().get(it) == null) it else null }
        if (missingIds.isNotEmpty()) {
            val creators = webservice.getCreatorsByIds(missingIds).models
            database.creatorDao().upsert(creators)
        }
    }

    private suspend fun checkFKeysIssue(issues: List<Issue>) {
        val variantOfIds: List<Int> = issues.mapNotNull { it.variantOf }
        val missingIssueIds =
            variantOfIds.mapNotNull { if (database.issueDao().get(it) == null) it else null }
        if (missingIssueIds.isNotEmpty()) {
            val missingIssues = webservice.getIssuesByIds(missingIssueIds).models
            checkFKeysIssue(missingIssues)
            database.issueDao().upsert(missingIssues)
        }
    }

    private suspend fun checkFKeysStory(stories: List<Story>) {
        val issueIds: List<Int> = stories.map { it.issueId }
        val missingIssueIds =
            issueIds.mapNotNull { if (database.issueDao().get(it) == null) it else null }
        if (missingIssueIds.isNotEmpty()) {
            val missingIssues = webservice.getIssuesByIds(missingIssueIds).models
            checkFKeysIssue(missingIssues)
            database.issueDao().upsert(missingIssues)
        }
    }

    private suspend fun checkFKeysAppearance(appearances: List<Appearance>) {
        val missingCharacterIds: List<Int> = appearances.mapNotNull {
            val id = it.character
            if (database.characterDao().get(id) == null) id else null
        }
        if (missingCharacterIds.isNotEmpty()) {
            val missingCharacters = webservice.getCharactersByIds(missingCharacterIds).models
            database.characterDao().upsert(missingCharacters)
        }

        val missingStoryIds = appearances.mapNotNull {
            val id = it.story
            if (database.storyDao().get(id) == null) id else null
        }
        if (missingStoryIds.isNotEmpty()) {
            val missingStories = webservice.getStoriesByIds(missingStoryIds).models
            checkFKeysStory(missingStories)
            database.storyDao().upsert(missingStories)
        }
    }

    private suspend fun <T : CreditX> checkFKeysCredit(credits: List<T>) {
        val missingNameDetailIds: List<Int> = credits.mapNotNull {
            val id = it.nameDetailId
            if (database.nameDetailDao().get(id) == null) id else null
        }
        if (missingNameDetailIds.isNotEmpty()) {
            val missingNameDetails = webservice.getNameDetailsByIds(missingNameDetailIds).models
            checkFKeysNameDetail(missingNameDetails)
            database.nameDetailDao().upsert(missingNameDetails)
        }

        val missingStoryIds = credits.mapNotNull {
            val id = it.storyId
            if (database.storyDao().get(id) == null) id else null
        }
        if (missingStoryIds.isNotEmpty()) {
            val missingStories = webservice.getStoriesByIds(missingStoryIds).models
            checkFKeysStory(missingStories)
            database.storyDao().upsert(missingStories)
        }
    }

    @ExperimentalCoroutinesApi
    private fun <M : DataModel> saveTimeIfNotEmpty(items: List<M>, saveTag: String): List<M> =
        items.also {
            if (it.isNotEmpty()) {
                Repository.saveTime(prefs, saveTag)
            }
        }

    companion object {
        private suspend fun <G : GcdJson<M>, M : DataModel> getItemsByPage(
            page: Int,
            apiCall: suspend (Int) -> List<Item<G, M>>,
            mclass: KClass<M>,
        ): List<M>? =
            supervisorScope {
                runSafely("getItemsByPage ${mclass::simpleName}", page) {
                    async { apiCall(it) }
                }?.models
            }

        private suspend fun <T : DataModel> refresh(
            prefs: SharedPreferences, savePageTag: String,
            saveTag: String,
            getItemsByPage: suspend (Int) -> List<T>?,
            dao: BaseDao<T>,
            followup: suspend (List<T>) -> Unit = {},
        ) {
            if (checkIfStale(saveTag, WEEKLY, prefs)) {
                var page = prefs.getInt(savePageTag, 0)
                var stop = false
                var success = true

                do {
                    coroutineScope {
                        val itemPage: List<T>? = getItemsByPage(page)

                        if (itemPage != null && itemPage.isNotEmpty()) {
                            followup(itemPage)
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
    }

    suspend fun updateStories(issueId: Int) {
        var stories: List<Story> = database.storyDao().getStories(issueId)

        if (stories.isEmpty()) {
            stories = supervisorScope {
                runSafely("getStoriesByIssue", issueId) {
                    async { webservice.getStoriesByIssue(it).models }
                } ?: emptyList()
            }
            checkFKeysStory(stories)
            database.storyDao().upsert(stories)
        }

        var credits: List<Credit> = database.creditDao().getCreditsByStoryIds(stories.ids)
        if (credits.isEmpty()) {
            credits = supervisorScope {
                runSafely("getCreditsByStoryIds", stories.ids) {
                    async { webservice.getCreditsByStoryIds(it).models }
                } ?: emptyList()
            }
            checkFKeysCredit(credits)
            database.creditDao().upsert(credits)
        }

        var excredits: List<ExCredit> = database.exCreditDao().getExCreditsByStoryIds(stories.ids)
        if (excredits.isEmpty()) {
            excredits = supervisorScope {
                runSafely("getExCreditsByStoryIds", stories.ids) {
                    async { webservice.getExtractedCreditsByStories(it).models }
                } ?: emptyList()
            }
            checkFKeysCredit(excredits)
            database.exCreditDao().upsert(excredits)
        }

        var appearances: List<Appearance> =
            database.appearanceDao().getAppearancesByStoryIds(stories.ids)
        if (appearances.isEmpty()) {
            appearances = supervisorScope {
                runSafely("getAppearancesByStory", stories.ids) {
                    async { webservice.getAppearancesByStory(it).models }
                } ?: emptyList()
            }
            checkFKeysAppearance(appearances)
            database.appearanceDao().upsert(appearances)
        }
    }
}