package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.NameDetail
import kotlinx.coroutines.*

private const val TAG = APP + "CreatorUpdater"

class UpdateCreator(
    val apiService: Webservice,
    val database: IssueDatabase,
    val prefs: SharedPreferences
) {

    internal fun updateAll(creatorIds: List<Int>) {
        creatorIds.forEach { update(it) }
    }

    private fun update(creatorId: Int) {
        if (Repository.checkIfStale(CREATOR_TAG(creatorId), CREATOR_LIFETIME, prefs)) {
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
                    apiService.getStories(storyIds)
                } else {
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

        val exCreditsCall = CoroutineScope(Dispatchers.IO).async {
            nameDetailCall.await()?.let { nameDetails ->
                apiService.getExtractedCreditsByNameDetail(nameDetails.map {
                    it.nameDetailId
                })
            }
        }


        val exStoriesCall = CoroutineScope(Dispatchers.IO).async {
            exCreditsCall.await()?.let {
                val credits = it.map { item -> item.toRoomModel() }
                val storyIds = credits.map { credit -> credit.storyId }
                if (storyIds.isNotEmpty()) {
                    apiService.getStories(storyIds)
                } else {
                    null
                }
            }
        }

        val exIssuesCall = CoroutineScope(Dispatchers.IO).async {
            exStoriesCall.await()?.let {
                val issueIds = it.map { item -> item.toRoomModel().issueId }
                if (issueIds.isNotEmpty()) {
                    apiService.getIssues(issueIds)
                } else {
                    null
                }
            }
        }

        val exVariantsCall =
            CoroutineScope(Dispatchers.IO).async {
                exIssuesCall.await()?.let {
                    val issueIds = it.mapNotNull { item -> item.toRoomModel().variantOf }
                    if (issueIds.isNotEmpty()) {
                        apiService.getIssues(issueIds)
                    } else {
                        null
                    }
                }
            }

        CoroutineScope(Dispatchers.IO).launch {
            coroutineScope {
                withContext(Dispatchers.IO) {
                    val stories = storiesCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val exStories =
                        exStoriesCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val variants =
                        variantsCall.await()?.map { it.toRoomModel() } ?: emptyList() ?: emptyList()
                    val exVariants =
                        exVariantsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val issues = issuesCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val exIssues =
                        exIssuesCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val credits = creditsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val exCredits =
                        exCreditsCall.await()?.map { it.toRoomModel() } ?: emptyList()
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
                }.let {
                    Repository.saveTime(prefs, CREATOR_TAG(creatorId))
                }
            }
        }
    }
}