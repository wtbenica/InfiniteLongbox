package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
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
    webservice: Webservice,
     database: IssueDatabase,
     prefs: SharedPreferences
) : Updater(webservice, database, prefs) {

    init {
        Log.d(TAG, "CREATOR UPDATER INIT")
    }

    /**
     * UpdateAll
     *
     * Updates issues, stories, credits, and character appearances for each creator id
     *
     * @param creatorIds
     */
    internal fun update_new(creatorIds: List<Int>) {
        Log.d(TAG, "UPDATING cREAtoRS: $creatorIds")
        CoroutineScope(Dispatchers.IO).launch {
            val nameDetails: List<NameDetailAndCreator> = getLocalNameDetailsByCreatorId(creatorIds)

            for (name in nameDetails) {
                val id = name.nameDetail.nameDetailId
                refreshById(
                    prefs,
                    CREATOR_TAG(id),
                    this@CreatorUpdater::getCreditsByNameDetailId,
                    this@CreatorUpdater::checkFKeysCredit,
                    database.creditDao(),
                    id
                )
            }
        }
    }

    private suspend fun getCreditsByNameDetailId(creatorId: Int): List<Credit>? =
        getItemsByArgument(listOf(creatorId), webservice::getCreditsByNameDetail)

    internal fun update(creatorIds: List<Int>) {
        CoroutineScope(Dispatchers.IO).launch {
            val meta: CreatorMeta = getCreditsByCreatorIds(creatorIds)

            database.transactionDao().upsert(
                stories = meta.stories,
                issues = meta.issues,
                credits = meta.credits,
                exCredits = meta.exCredits,
                appearances = meta.appearances
            )
        }
    }

    private suspend fun getCreditsByCreatorIds(creatorIds: List<Int>): CreatorMeta {
        val nameDetails: List<NameDetailAndCreator> = getLocalNameDetailsByCreatorId(creatorIds)
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

    private suspend fun getLocalNameDetailsByCreatorId(creatorIds: List<Int>):
            List<NameDetailAndCreator> =
        CoroutineScope(Dispatchers.IO).async {
            database.nameDetailDao().getNameDetailsByCreatorIds(creatorIds)
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
            val storyIds = credits.map { it.story }

            runSafely("getStoriesByIds", storyIds) {
                async { webservice.getStoriesByIds(it) }
            }?.models ?: emptyList()
        }

    private suspend fun getIssuesByStories(storiesCall: List<Story>): List<Issue> =
        coroutineScope {
            val issueIds = storiesCall.map { it.issueId }
            runSafely("getIssuesByIds", issueIds) {
                async { webservice.getIssuesByIds(issueIds) }
            }?.models ?: emptyList()
        }

    private suspend fun getVariantsOfByIssues(issuesCall: List<Issue>): List<Issue> =
        coroutineScope {
            val variantsOfIds = issuesCall.mapNotNull { it.variantOf }

            runSafely("getIssuesByIds Variants", variantsOfIds) {
                async { webservice.getIssuesByIds(it) }
            }?.models ?: emptyList()
        }

    private suspend fun getExCreditsByNameDetail(nameDetailCall: List<NameDetailAndCreator>) =
        coroutineScope {
            val nameDetailIds = nameDetailCall.map { it.nameDetail.nameDetailId }

            runSafely("getExCreditsByNameDetails", nameDetailIds) {
                async { webservice.getExtractedCreditsByNameDetail(it) }
            }?.models ?: emptyList()
        }

    private suspend fun getAppearancesByStories(stories: List<Story>): List<Appearance> =
        coroutineScope {
            runSafely("refreshAppearances", stories.ids) {
                async { webservice.getAppearancesByStory(it) }
            }?.models ?: emptyList()
        }
}
