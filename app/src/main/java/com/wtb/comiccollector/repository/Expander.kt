package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.repository.Updater.PriorityDispatcher.Companion.highPriorityDispatcher
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
class Expander private constructor(webservice: Webservice, prefs: SharedPreferences) : Updater(
    webservice,
    prefs
) {

    fun expandSeriesAsync(series: Series) = seriesExpander.expandSeriesAsync(series)
    fun expandCreatorsAsync(creators: List<Creator>) =
        creatorExpander.expandCreatorsAsync(creators)

    fun expandIssueAsync(issues: List<Issue>) = issueExpander.expandIssueAsync(issues)
    fun expandStoryAsync(stories: List<Story>) = storyExpander.expandStoryAsync(stories)
    fun expandCharacterAsync(characters: List<Character>) =
        characterExpander.expandCharacter2Async(characters)

    private val seriesExpander
        get() = SeriesExpander()
    private val issueExpander
        get() = IssueExpander()
    private val storyExpander
        get() = StoryExpander()
    private val characterExpander
        get() = CharacterExpander()
    private val creatorExpander
        get() = CreatorExpander()

    inner class SeriesExpander {
        fun expandSeriesAsync(series: Series): Deferred<Unit> =
            CoroutineScope(highPriorityDispatcher).async {
                withContext(highPriorityDispatcher) {
                    if (checkIfStale(seriesTag(series.id), 1L, prefs)) {
                        val issues = updateSeriesIssues(series.id)
                        expandIssueAsync(issues)
                    }
                }.let {
                    Repository.saveTime(prefs, seriesTag(series.id))
                }
            }


        /**
         * Gets series issues, adds missing foreign key models
         */
        private suspend fun updateSeriesIssues(seriesId: Int) =
            updateById(
                prefs = prefs,
                saveTag = ::seriesTag,
                getItems = ::getIssuesBySeriesId,
                id = seriesId,
                followup = fKeyChecker::checkFKeysIssue,
                collector = Collector.issueCollector()
            )

        private suspend fun getIssuesBySeriesId(seriesId: Int): List<Issue>? =
            retrieveItemsByArgument(seriesId, webservice::getIssuesBySeries)
    }

    inner class IssueExpander {
        /**
         * Updates issue model, then stories, credits, appearances, and cover
         */
        internal fun expandIssueAsync(issues: List<Issue>): Deferred<Unit> =
            CoroutineScope(highPriorityDispatcher).async {
                val stories: List<Story> = updateIssuesStories(issues.ids)
                expandStoryAsync(stories)
            }

        private suspend fun updateIssuesStories(issueIds: List<Int>): List<Story> =
            updateById(
                prefs = prefs,
                saveTag = null,
                getItems = ::getStoriesByIssueIds,
                id = issueIds,
                followup = fKeyChecker::checkFKeysStory,
                collector = Collector.storyCollector()
            )

        private suspend fun getStoriesByIssueIds(issueIds: List<Int>): List<Story> =
            retrieveItemsByList(issueIds, webservice::getStoriesByIssueIds)
    }

    inner class StoryExpander {
        internal fun expandStoryAsync(stories: List<Story>): Deferred<Unit> =
            CoroutineScope(highPriorityDispatcher).async {
                withContext(highPriorityDispatcher) {
                    updateStoriesCredits(stories.ids)
                    updateStoriesAppearances(stories.ids)
                }
            }

        private suspend fun updateStoriesCredits(storyIds: List<Int>): List<CreditX> =
            updateById(
                prefs = prefs,
                saveTag = null,
                getItems = ::getCreditsByStoryIds,
                id = storyIds,
                followup = fKeyChecker::checkFKeysCredit,
                collector = Collector.creditCollector()
            ) +
                    updateById(
                        prefs = prefs,
                        saveTag = null,
                        getItems = ::getExCreditsByStoryIds,
                        id = storyIds,
                        followup = fKeyChecker::checkFKeysCredit,
                        collector = Collector.exCreditCollector()
                    )

        private suspend fun updateStoriesAppearances(storyIds: List<Int>): List<Appearance> =
            updateById(
                prefs = prefs,
                saveTag = null,
                getItems = ::getAppearancesByStoryIds,
                id = storyIds,
                followup = fKeyChecker::checkFKeysAppearance,
                collector = Collector.appearanceCollector()
            )

        private suspend fun getCreditsByStoryIds(storyIds: List<Int>): List<Credit> =
            retrieveItemsByList(storyIds, webservice::getCreditsByStoryIds)

        private suspend fun getExCreditsByStoryIds(storyIds: List<Int>): List<ExCredit> =
            retrieveItemsByList(storyIds, webservice::getExCreditsByStoryIds)

        private suspend fun getAppearancesByStoryIds(storyIds: List<Int>): List<Appearance> =
            retrieveItemsByList(storyIds, webservice::getAppearancesByStoryIds)
    }

    inner class CharacterExpander {
        internal fun expandCharacter2Async(characters: List<Character>): Deferred<Unit> =
            CoroutineScope(highPriorityDispatcher).async {
                val appearances: List<Appearance> = retrieveItemsByList(
                    argList = characters.ids,
                    apiCall = webservice::getAppearancesByCharacterIds
                )
                val storyIds = appearances.map { it.story }

                val stories: List<Story> = retrieveItemsByList(
                    argList = storyIds,
                    apiCall = webservice::getStoriesByIds
                )
                val issueIds = stories.map { it.issue }

                val issues: List<Issue> = retrieveItemsByList(
                    argList = issueIds,
                    apiCall = webservice::getIssuesByIds
                )
                fKeyChecker.checkFKeysIssue(issues)

                issueDatabase.issueDao().upsert(issues)
                issueDatabase.storyDao().upsert(stories)
                issueDatabase.appearanceDao().upsert(appearances)
                expandStoryAsync(stories)
            }

//        internal fun expandCharacterAsync(characters: List<Character>): Deferred<Unit> =
//            CoroutineScope(highPriorityDispatcher).async {
//                withContext(highPriorityDispatcher) {
//                    characters.ids.forEach { updateCharacterAppearances(it) }
//                }
//            }
//
//        /**
//         * Gets character appearances, adds missing foreign key models
//         */
//        private suspend fun updateCharacterAppearances(characterId: Int) =
//            updateById(
//                prefs = prefs,
//                saveTag = ::characterTag,
//                getItems = ::getAppearancesByCharacterId,
//                id = characterId,
//                followup = fKeyChecker::checkFKeysAppearance,
//                collector = Collector.appearanceCollector()
//            )
//
//        private suspend fun getAppearancesByCharacterId(characterId: Int): List<Appearance>? =
//            retrieveItemsByArgument(characterId, webservice::getAppearancesByCharacterIds)
    }

    inner class CreatorExpander {
        private val TAG = APP + "CreatorExpander"

        internal fun expandCreatorsAsync(creators: List<Creator>): Deferred<Unit> =
            CoroutineScope(highPriorityDispatcher).async {
                val nameDetails: List<NameDetail> =
                    issueDatabase.nameDetailDao().getNameDetailsByCreatorIds(creators.ids)

                val credits: List<Credit> = retrieveItemsByList(
                    argList = nameDetails.ids,
                    apiCall = webservice::getCreditsByNameDetail
                )
                val extracts: List<ExCredit> = retrieveItemsByList(
                    argList = nameDetails.ids,
                    apiCall = webservice::getExCreditsByNameDetail
                )
                val creditXs: List<CreditX> = credits + extracts
                val storyIds = creditXs.map { it.story }

                val stories: List<Story> = retrieveItemsByList(
                    argList = storyIds,
                    apiCall = webservice::getStoriesByIds
                )
                val issueIds = stories.map { it.issue }

                val issues: List<Issue> = retrieveItemsByList(
                    argList = issueIds,
                    apiCall = webservice::getIssuesByIds
                )
                async {
                    fKeyChecker.checkFKeysIssue(issues)
                }.await().let {
                    issueDatabase.issueDao().upsert(issues)
                    issueDatabase.storyDao().upsert(stories)
                    issueDatabase.creditDao().upsert(credits)
                    issueDatabase.exCreditDao().upsert(extracts)
                    expandStoryAsync(stories)
                }
            }
    }

    companion object {
        private var sWebservice: Webservice? = null
        private var sPrefs: SharedPreferences? = null

        fun initialize(webservice: Webservice, prefs: SharedPreferences) {
            sWebservice = webservice
            sPrefs = prefs
        }

        fun get(): Expander {
            val tempWebServ = sWebservice
            val tempPrefs = sPrefs
            if (tempWebServ == null || tempPrefs == null) {
                throw IllegalStateException("Expander must be initialized")
            } else {
                return Expander(tempWebServ, tempPrefs)
            }
        }
    }
}