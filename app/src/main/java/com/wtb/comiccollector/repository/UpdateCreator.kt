package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.NameDetail
import kotlinx.coroutines.*

private const val TAG = APP + "CreatorUpdater"

class CreatorUpdater(
    val apiService: Webservice,
    val database: IssueDatabase,
    val prefs: SharedPreferences
) {

    internal fun updateAll(creatorIds: List<Int>) {
        creatorIds.forEach { update(it) }
    }

    private fun update(creatorId: Int) {
        if (IssueRepository.checkIfStale(CREATOR_TAG(creatorId), CREATOR_LIFETIME, prefs)) {
            Log.d(TAG, "CreatorUpdater update $creatorId")
            refreshCredits(creatorId)
        }
    }

    private fun refreshCredits(creatorId: Int) {
        val nameDetailCall = CoroutineScope(Dispatchers.IO).async {
            database.nameDetailDao().getNameDetailByCreatorId(creatorId)
        }

        val creditsCall = CoroutineScope(Dispatchers.IO).async {
            nameDetailCall.await()?.let { nameDetails ->
                apiService.getCreditsByNameDetail(nameDetails.map { it.nameDetailId })
            }
        }

        val storiesCall = CoroutineScope(Dispatchers.IO).async {
            creditsCall.await()?.let { gcdCredits ->
                val storyIds = gcdCredits.map { item -> item.toRoomModel().storyId }
                if (storyIds.isNotEmpty()) {
                    Log.d(TAG, "Found stories")
                    apiService.getStories(storyIds)
                } else {
                    Log.d(TAG, "No find stories?")
                    null
                }
            }
        }

        val issuesCall = CoroutineScope(Dispatchers.IO).async {
            storiesCall.await()?.let { gcdStories ->
                val issueIds = gcdStories.map { item -> item.toRoomModel().issueId }
                if (issueIds.isNotEmpty()) {
                    apiService.getIssues(issueIds)
                } else {
                    null
                }
            }
        }

        val variantsCall = CoroutineScope(Dispatchers.IO).async {
            issuesCall.await()?.let {
                val issueIds = it.mapNotNull { item -> item.toRoomModel().variantOf }
                if (issueIds.isNotEmpty()) {
                    apiService.getIssues(issueIds)
                } else {
                    null
                }
            }
        }

        val extractedCreditsCall = CoroutineScope(Dispatchers.IO).async {
            nameDetailCall.await()?.let { nameDetails ->
                apiService.getExtractedCreditsByNameDetail(nameDetails.map {
                    Log.d(TAG, "Refreshing extracts by name detail ${it.name}")
                    it.nameDetailId
                })
            }
        }


        val extractedStoriesCall = CoroutineScope(Dispatchers.IO).async {
            extractedCreditsCall.await()?.let {
                val credits = it.map { item -> item.toRoomModel() }
                val storyIds = credits.map { credit -> credit.storyId }
                if (storyIds.isNotEmpty()) {
                    Log.d(TAG, "Found extracts")
                    apiService.getStories(storyIds)
                } else {
                    Log.d(TAG, "No ex stories found")
                    null
                }
            }
        }

        val extractedIssuesCall = CoroutineScope(Dispatchers.IO).async {
            extractedStoriesCall.await()?.let {
                val issueIds = it.map { item -> item.toRoomModel().issueId }
                if (issueIds.isNotEmpty()) {
                    Log.d(TAG, "Extract Issues FOUND ${issueIds.size}")
                    apiService.getIssues(issueIds)
                } else {
                    Log.d(TAG, "Extract Issues EMPTY")
                    null
                }
            }
        }

        val extractedVariantsCall =
            CoroutineScope(Dispatchers.IO).async {
                extractedIssuesCall.await()?.let {
                    val issueIds = it.mapNotNull { item -> item.toRoomModel().variantOf }
                    if (issueIds.isNotEmpty()) {
                        Log.d(TAG, "Extract Variants FOUND ${issueIds.size}")
                        apiService.getIssues(issueIds)
                    } else {
                        Log.d(TAG, "Extract Variants EMPTY")
                        null
                    }
                }
            }

        CoroutineScope(Dispatchers.IO).launch {
            coroutineScope {
                withContext(Dispatchers.IO) {
                    val stories = storiesCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val exStories =
                        extractedStoriesCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val variants =
                        variantsCall.await()?.map { it.toRoomModel() } ?: emptyList() ?: emptyList()
                    val exVariants =
                        extractedVariantsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val issues = issuesCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val exIssues =
                        extractedIssuesCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val credits = creditsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val exCredits =
                        extractedCreditsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val nameDetails: List<NameDetail>? = nameDetailCall.await()

                    val allIssues = variants + exVariants + issues + exIssues
                    val allStories = stories + exStories

                    val series = CoroutineScope(Dispatchers.IO).async {
                        apiService.getSeriesByIds(allIssues.map { it.seriesId })
                    }.await().map { it.toRoomModel() }

                    database.transactionDao().upsertSus(
                        stories = allStories,
                        issues = allIssues,
                        nameDetails = nameDetails,
                        credits = credits,
                        exCredits = exCredits,
                        series = series
                    )

                    Log.d(TAG, "FINISHING $creatorId ${nameDetails?.get(0)?.name}")
                }.let {
                    IssueRepository.saveTime(prefs, CREATOR_TAG(creatorId))
                    Log.d(TAG, "DONE UPDATING CREATOR $creatorId")
                }
            }
        }
    }
}