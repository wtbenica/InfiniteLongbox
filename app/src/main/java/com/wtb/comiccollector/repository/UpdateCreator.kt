package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.*
import java.net.ConnectException
import java.net.SocketTimeoutException

private const val TAG = APP + "CreatorUpdater"

/**
 * Update creator
 *
 * Exposes a single function 'updateAll' to update creator information
 *
 * @property apiService
 * @property database
 * @property prefs
 * @constructor Create empty Update creator
 */
@ExperimentalCoroutinesApi
class UpdateCreator(
    val apiService: Webservice,
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
        creatorIds.forEach { update(it) }
    }

    private suspend fun update(creatorId: Int) {
        if (checkIfStale(CREATOR_TAG(creatorId), CREATOR_LIFETIME, prefs)) {
            Log.d(TAG, "update $creatorId")
            refreshCredits(creatorId)
        }
    }

    private suspend fun refreshCredits(creatorId: Int) {
        val nameDetailCall = refreshNameDetailAsync(creatorId)
        val creditsCall = refreshCreditsAsync(nameDetailCall)
        val storiesCall = refreshStoriesAsync(creditsCall)
        val issuesCall = refreshIssuesAsync(storiesCall)
        val variantsCall = refreshVariantsAsync(issuesCall)
        val exCreditsCall = refreshExCreditsAsync(nameDetailCall)
        val exStoriesCall = refreshExStoriesAsync(exCreditsCall)
        val exIssuesCall = refreshExIssuesAsync(exStoriesCall)
        val exVariantsCall = refreshExVariantsAsync(exIssuesCall)
        val stories = storiesCall.await()?.map { it.toRoomModel() } ?: emptyList()
        val exStories = exStoriesCall.await()?.map { it.toRoomModel() } ?: emptyList()
        val allStories = stories + exStories

        val appearances = refreshAppearancesAsync(allStories).await()?.map { it.toRoomModel() }
            ?: emptyList()

        val variants = variantsCall.await()?.map { it.toRoomModel() } ?: emptyList()
        val exVariants = exVariantsCall.await()?.map { it.toRoomModel() } ?: emptyList()
        val issues = issuesCall.await()?.map { it.toRoomModel() } ?: emptyList()
        val exIssues = exIssuesCall.await()?.map { it.toRoomModel() } ?: emptyList()
        val allIssues = variants + exVariants + issues + exIssues

        val credits: List<Credit>? = creditsCall.await()?.map { it.toRoomModel() }
        val exCredits: List<ExCredit>? = exCreditsCall.await()?.map { it.toRoomModel() }

        CoroutineScope(Dispatchers.IO).async {
            database.transactionDao().upsertSus(
                stories = allStories,
                issues = allIssues,
                credits = credits,
                exCredits = exCredits,
                appearances = appearances
            )
        }.await().let {
            Repository.saveTime(prefs, CREATOR_TAG(creatorId))
        }
    }

    private fun refreshAppearancesAsync(allStories: List<Story>):
            Deferred<List<Item<GcdCharacterAppearance, Appearance>>?> =
        CoroutineScope(Dispatchers.IO).async {
            val storyIds = allStories.map { it.storyId }

            try {
                Log.d(TAG, "appearancesbystory: $storyIds")
                apiService.getAppearancesByStory(storyIds)
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "refreshAppearances: $e")
                null
            } catch (e: ConnectException) {
                Log.d(TAG, "refreshAppearances: $e")
                null
            }
        }

    private fun refreshNameDetailAsync(creatorId: Int): Deferred<List<NameDetailAndCreator>> =
        CoroutineScope(Dispatchers.IO).async {
            database.nameDetailDao().getNameDetailsByCreatorId(creatorId)
        }

    private fun refreshCreditsAsync(nameDetailCall: Deferred<List<NameDetailAndCreator>>) =
        CoroutineScope(Dispatchers.IO).async {
            nameDetailCall.await().let { nameDetails: List<NameDetailAndCreator> ->
                try {
                    apiService.getCreditsByNameDetail(nameDetails.map { it.nameDetail.nameDetailId })
                } catch (e: SocketTimeoutException) {
                    Log.d(TAG, "update: getCreditsByNameDetail: $e")
                    null
                } catch (e: ConnectException) {
                    Log.d(TAG, "update: getCreditsByNameDetail: $e")
                    null
                }
            }
        }

    private fun refreshStoriesAsync(creditsCall: Deferred<List<Item<GcdCredit, Credit>>?>) =
        CoroutineScope(Dispatchers.IO).async {
            creditsCall.await()?.let { gcdCredits ->
                val storyIds = gcdCredits.map { item -> item.toRoomModel().storyId }
                if (storyIds.isNotEmpty()) {
                    try {
                        apiService.getStories(storyIds)
                    } catch (e: SocketTimeoutException) {
                        Log.d(TAG, "update: getStories: $e")
                        null
                    } catch (e: ConnectException) {
                        Log.d(TAG, "update: getStories: $e")
                        null
                    }
                } else {
                    null
                }
            }
        }

    private fun refreshIssuesAsync(storiesCall: Deferred<List<Item<GcdStory, Story>>?>) =
        CoroutineScope(Dispatchers.IO).async {
            storiesCall.await()?.let { gcdStories ->
                val issueIds = gcdStories.map { item -> item.toRoomModel().issueId }
                if (issueIds.isNotEmpty()) {
                    try {
                        apiService.getIssues(issueIds)
                    } catch (e: SocketTimeoutException) {
                        Log.d(TAG, "update: getIssues: $e")
                        null
                    } catch (e: ConnectException) {
                        Log.d(TAG, "update: getIssues: $e")
                        null
                    }
                } else {
                    null
                }
            }
        }

    private fun refreshVariantsAsync(issuesCall: Deferred<List<Item<GcdIssue, Issue>>?>) =
        CoroutineScope(Dispatchers.IO).async {
            issuesCall.await()?.let {
                val issueIds = it.mapNotNull { item -> item.toRoomModel().variantOf }
                if (issueIds.isNotEmpty()) {
                    try {
                        apiService.getIssues(issueIds)
                    } catch (e: SocketTimeoutException) {
                        Log.d(TAG, "update: getVariants: $e")
                        null
                    } catch (e: ConnectException) {
                        Log.d(TAG, "update: getVariants: $e")
                        null
                    }
                } else {
                    null
                }
            }
        }

    private fun refreshExCreditsAsync(nameDetailCall: Deferred<List<NameDetailAndCreator>>) =
        CoroutineScope(Dispatchers.IO).async {
            nameDetailCall.await().let { gcdNameDetails ->
                try {
                    apiService.getExtractedCreditsByNameDetail(gcdNameDetails.map {
                        it.nameDetail.nameDetailId
                    })
                } catch (e: SocketTimeoutException) {
                    Log.d(TAG, "update: getExCredits: $e")
                    null
                } catch (e: ConnectException) {
                    Log.d(TAG, "update: getExCredits: $e")
                    null
                }
            }
        }

    private fun refreshExStoriesAsync(exCreditsCall: Deferred<List<Item<GcdExCredit, ExCredit>>?>) =
        CoroutineScope(Dispatchers.IO).async {
            exCreditsCall.await()?.let {
                val credits = it.map { item -> item.toRoomModel() }
                val storyIds = credits.map { credit -> credit.storyId }
                if (storyIds.isNotEmpty()) {
                    try {
                        apiService.getStories(storyIds)
                    } catch (e: SocketTimeoutException) {
                        Log.d(TAG, "update: getExStories: $e")
                        null
                    } catch (e: ConnectException) {
                        Log.d(TAG, "update: getExStories: $e")
                        null
                    }
                } else {
                    null
                }
            }
        }

    private fun refreshExIssuesAsync(exStoriesCall: Deferred<List<Item<GcdStory, Story>>?>) =
        CoroutineScope(Dispatchers.IO).async {
            exStoriesCall.await()?.let {
                val issueIds = it.map { item -> item.toRoomModel().issueId }
                if (issueIds.isNotEmpty()) {
                    try {
                        apiService.getIssues(issueIds)
                    } catch (e: SocketTimeoutException) {
                        Log.d(TAG, "update: getExIssues: $e")
                        null
                    } catch (e: ConnectException) {
                        Log.d(TAG, "update: getExIssues: $e")
                        null
                    }
                } else {
                    null
                }
            }
        }

    private fun refreshExVariantsAsync(exIssuesCall: Deferred<List<Item<GcdIssue, Issue>>?>) =
        CoroutineScope(Dispatchers.IO).async {
            exIssuesCall.await()?.let {
                val issueIds = it.mapNotNull { item -> item.toRoomModel().variantOf }
                if (issueIds.isNotEmpty()) {
                    try {
                        apiService.getIssues(issueIds)
                    } catch (e: SocketTimeoutException) {
                        Log.d(TAG, "update: getExVariants: $e")
                        null
                    } catch (e: ConnectException) {
                        Log.d(TAG, "update: getExVariants: $e")
                        null
                    }
                } else {
                    null
                }
            }
        }
}
