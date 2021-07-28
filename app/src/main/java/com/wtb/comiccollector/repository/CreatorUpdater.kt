package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.*

private const val TAG = APP + "CreatorUpdater"

/**
 * Update creator
 *
 * Exposes a single function 'updateAll' to update creator information
 *
 * @property webservice
 * @property database
 * @property prefs
 * @constructor Create empty Update creator
 */
@ExperimentalCoroutinesApi
class CreatorUpdater(
    val webservice: Webservice,
    val database: IssueDatabase,
    val prefs: SharedPreferences
) : Updater() {

    /**
     * UpdateAll
     *
     * Updates issues, stories, credits, and character appearances for each creator id
     *
     * @param creatorIds
     */
    internal suspend fun updateAll(creatorIds: List<Int>) {
//        creatorIds.forEach { update(it) }
        val allStories: MutableList<Story> = mutableListOf()
        val allIssues = mutableListOf<Issue>()
        val allCredits = mutableListOf<Credit>()
        val allExCredits = mutableListOf<ExCredit>()
        val allAppearances = mutableListOf<Appearance>()

        creatorIds.forEach {
            val meta: CreatorMeta = getCreditsByNameDetails(it)
            allStories.addAll(meta.stories)
            allIssues.addAll(meta.issues)
            allCredits.addAll(meta.credits)
            allExCredits.addAll(meta.exCredits)
            allAppearances.addAll(meta.appearances)
        }

        CoroutineScope(Dispatchers.IO).launch {
            database.transactionDao().upsert(
                stories = allStories,
                issues = allIssues,
                credits = allCredits,
                exCredits = allExCredits,
                appearances = allAppearances
            )
        }

    }

    private suspend fun getCreditsByNameDetails(creatorId: Int): CreatorMeta {
        val nameDetails: List<NameDetailAndCreator> = getNameDetailsByCreatorId(creatorId)
        val credits: List<Credit> = getCreditsByNameDetails(nameDetails)
        val stories: List<Story> = getStoriesByCredits(credits)
        val issues: List<Issue> = getIssuesByStories(stories)
        val variants: List<Issue> = getVariantsOfByIssues(issues)
        val exCredits: List<ExCredit> = getExCreditsByNameDetail(nameDetails)
        val exStories: List<Story> = getStoriesByCredits(exCredits)
        val exIssues: List<Issue> = getIssuesByStories(exStories)
        val exVariants = getVariantsOfByIssues(exIssues)

        val allStories = stories + exStories
        val allIssues = variants + exVariants + issues + exIssues

        val appearances: List<Appearance> = getAppearancesByStories(allStories)

        return CreatorMeta(allStories, allIssues, credits, exCredits, appearances)
    }

    data class CreatorMeta(
        val stories: List<Story>,
        val issues: List<Issue>,
        val credits: List<Credit>,
        val exCredits: List<ExCredit>,
        val appearances: List<Appearance>
    )

    private suspend fun getAppearancesByStories(stories: List<Story>): List<Appearance> =
        coroutineScope {
            runSafely("refreshAppearances", stories.ids) {
                async { webservice.getAppearancesByStory(it) }
            }?.models ?: emptyList()
        }

    private suspend fun getNameDetailsByCreatorId(creatorId: Int): List<NameDetailAndCreator> =
        CoroutineScope(Dispatchers.IO).async {
            database.nameDetailDao().getNameDetailsByCreatorId(creatorId)
        }.await()

    private suspend fun getCreditsByNameDetails(nameDetails: List<NameDetailAndCreator>): List<Credit> =
        coroutineScope {
            val nameDetailIds = nameDetails.map { it.nameDetail.nameDetailId }

            runSafely("update: getCreditsByNameDetail", nameDetailIds) {
                async { webservice.getCreditsByNameDetail(it) }
            }?.models ?: emptyList()
        }


    private suspend fun getStoriesByCredits(credits: List<CreditX>): List<Story> =
        coroutineScope {
            val storyIds = credits.map { it.storyId }

            runSafely("getStoriesByIds", storyIds) {
                async { webservice.getStories(it) }
            }?.models ?: emptyList()
        }

    private suspend fun getIssuesByStories(storiesCall: List<Story>): List<Issue> =
        coroutineScope {
            val issueIds = storiesCall.map { it.issueId }
            runSafely("getIssuesByIds", issueIds) {
                async { webservice.getIssues(issueIds) }
            }?.models ?: emptyList()
        }

    private suspend fun getVariantsOfByIssues(issuesCall: List<Issue>): List<Issue> =
        coroutineScope {
            val variantsOfIds = issuesCall.mapNotNull { it.variantOf }

            runSafely("getIssuesByIds Variants", variantsOfIds) {
                async { webservice.getIssues(it) }
            }?.models ?: emptyList()
        }

    private suspend fun getExCreditsByNameDetail(nameDetailCall: List<NameDetailAndCreator>) =
        coroutineScope {
            val nameDetailIds = nameDetailCall.map { it.nameDetail.nameDetailId }

            runSafely("getExCreditsByNameDetails", nameDetailIds) {
                async { webservice.getExtractedCreditsByNameDetail(it) }
            }?.models ?: emptyList()
        }
}
