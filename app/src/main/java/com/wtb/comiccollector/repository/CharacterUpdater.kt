package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.*

private const val TAG = APP + "UpdateSeries"

/**
 * UpdateSeries
 *
 * @property webservice
 * @property database
 * @property prefs
 * @constructor Create empty UpdateSeries
 */
@ExperimentalCoroutinesApi
class CharacterUpdater(
    private val webservice: Webservice,
    val database: IssueDatabase,
    val prefs: SharedPreferences
) : Updater() {

    /**
     * Update - refreshes series issues
     *
     * @param seriesId
     */
    internal fun update(filter: SearchFilter) {
        val character = filter.mCharacter
        if (character != null) {
            this.getIssuesByCharacterId(character.characterId)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val newFilter = SearchFilter(filter).apply { mSortType = null }
                val seriesList: List<FullSeries> =
                    database.seriesDao().getSeriesByFilterSus(newFilter)

                val storyList = mutableListOf<Story>()
                val appearanceList = mutableListOf<Appearance>()

                seriesList.forEach { fullSeries ->
                    getSeriesIssuesAndVariantsOf(fullSeries.series.seriesId).let { issues ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val stories = getStoriesByIssues(issues)
                            val appearances = getAppearancesByStory(stories)

                            storyList.addAll(stories)
                            appearanceList.addAll(appearances)
                        }
                    }
                }

                database.transactionDao().upsert(
                    stories = storyList,
                    appearances = appearanceList
                )
            }
        }
    }

    private suspend fun getAppearancesByStory(stories: List<Story>) =
        coroutineScope {
            runSafely("getAppearancesByStory", stories.ids) {
                async { webservice.getAppearancesByStory(it).models }
            } ?: emptyList()
        }

    private suspend fun getStoriesByIssues(issues: List<Issue>) =
        coroutineScope {
            runSafely("getStoriesByIssues", issues.ids) {
                async { webservice.getStoriesByIssues(it).models }
            } ?: emptyList()
        }

    private suspend fun getSeriesIssuesAndVariantsOf(seriesId: Int) =
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            val issues: List<Issue> = getIssuesBySeries(seriesId)
            val variantsOf: List<Issue> = getVariantsOf(issues)

            database.issueDao().upsert(variantsOf + issues)

            issues
        }

    private suspend fun getIssuesBySeries(seriesId: Int) =
        coroutineScope {
            runSafely("getIssuesBySeries: $seriesId", seriesId) {
                async { webservice.getIssuesBySeries(it).models }
            } ?: emptyList()
        }

    private suspend fun getVariantsOf(issues: List<Issue>): List<Issue> =
        coroutineScope {
            val variantOfIds = issues.mapNotNull { it.variantOf }

            runSafely("getIssues: variantsOf", variantOfIds) {
                async { webservice.getIssues(it).models }
            } ?: emptyList()
        }

    internal fun getIssuesByCharacterId(characterId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val appearances = getAppearances(characterId)
            val stories = getStories(appearances)
            val issues = getIssuesByCharacterId(stories)
            val variantsOf = getVariants(issues)
            val allIssues = variantsOf + issues
            val credits = getCredits(stories)
            val exCredits = getExCredits(stories)
            val nameDetail = getNameDetails(credits)
            val exNameDetail = getExNameDetails(exCredits)

            database.transactionDao().upsert(
                appearances = appearances,
                stories = stories,
                issues = allIssues,
                credits = credits,
                exCredits = exCredits,
                nameDetails = nameDetail + exNameDetail
            )
        }
    }

    private suspend fun getAppearances(characterId: Int): List<Appearance> =
        coroutineScope {
            runSafely("getAppearances", characterId) {
                async { webservice.getAppearances(characterId).models }
            } ?: emptyList()
        }

    private suspend fun getStories(appearances: List<Appearance>): List<Story> =
        coroutineScope {
            val storyIds = appearances.map { it.story }

            runSafely("getStories", storyIds) {
                async { webservice.getStories(it).models }
            } ?: emptyList()
        }

    private suspend fun getIssuesByCharacterId(stories: List<Story>): List<Issue> =
        coroutineScope {
            val issueIds = stories.map { it.issueId }

            runSafely("getIssues", issueIds) {
                async { webservice.getIssues(it).models }
            } ?: emptyList()
        }

    private suspend fun getVariants(issues: List<Issue>): List<Issue> =
        coroutineScope {
            val variantOfIds = issues.mapNotNull { it.variantOf }

            runSafely("getIssuesVariants", variantOfIds) {
                async { webservice.getIssues(it).models }
            } ?: emptyList()
        }

    private suspend fun getCredits(stories: List<Story>): List<Credit> =
        coroutineScope {
            runSafely("getCreditsByStories", stories.ids) {
                async { webservice.getCreditsByStories(it).models }
            } ?: emptyList()
        }

    private suspend fun getNameDetails(credits: List<Credit>): List<NameDetail> =
        coroutineScope {
            val nameDetailIds = credits.map { it.nameDetailId }

            runSafely("getNameDetailsByIds", nameDetailIds) {
                async { webservice.getNameDetailsByIds(nameDetailIds).models }
            } ?: emptyList()
        }

    private suspend fun getExCredits(stories: List<Story>) =
        coroutineScope {
            runSafely("getCreditsByStories", stories.ids) {
                async { webservice.getExtractedCreditsByStories(it).models }
            } ?: emptyList()
        }

    private suspend fun getExNameDetails(exCredits: List<ExCredit>): List<NameDetail> =
        coroutineScope {
            val exNameDetailIds = exCredits.map { it.nameDetailId }

            runSafely(
                "getExNameDetailsByIds",
                exNameDetailIds
            ) {
                async { webservice.getNameDetailsByIds(exNameDetailIds).models }
            } ?: emptyList()
        }
}

