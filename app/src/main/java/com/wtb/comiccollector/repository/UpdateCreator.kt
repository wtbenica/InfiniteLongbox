package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.NameDetailAndCreator
import kotlinx.coroutines.*

private const val TAG = APP + "CreatorUpdater"

@ExperimentalCoroutinesApi
class UpdateCreator(
    val apiService: Webservice,
    val database: IssueDatabase,
    val prefs: SharedPreferences
) : Updater() {

    internal fun updateAll(creatorIds: List<Int>) {
        creatorIds.forEach { update(it) }
    }

    private fun update(creatorId: Int) {
        if (checkIfStale(CREATOR_TAG(creatorId), CREATOR_LIFETIME, prefs)) {
            Log.d(TAG, "update $creatorId")
            refreshCredits(creatorId)
        }
    }

    private fun refreshCredits(creatorId: Int) {
        val nameDetailCall = CoroutineScope(Dispatchers.IO).async {
            database.nameDetailDao().getNameDetailsByCreatorId(creatorId)
        }

        val creditsCall = CoroutineScope(Dispatchers.IO).async {
            nameDetailCall.await().let { nameDetails: List<NameDetailAndCreator> ->
                Log.d(
                    TAG,
                    "refreshCredits getCreditsByNameDetail - $creatorId gcdNameDetails: ${nameDetails.size}"
                )
                apiService.getCreditsByNameDetail(nameDetails.map { it.nameDetail.nameDetailId })
            }
        }

        val storiesCall = CoroutineScope(Dispatchers.IO).async {
            creditsCall.await().let { gcdCredits ->
                Log.d(
                    TAG,
                    "refreshCredits getCreditsByNameDetail - $creatorId gcdCredits: ${gcdCredits.size}"
                )
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
                Log.d(
                    TAG,
                    "refreshCredits getCreditsByNameDetail - $creatorId gcdStories: ${gcdStories.size}"
                )
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
                Log.d(
                    TAG,
                    "refreshCredits getCreditsByNameDetail - $creatorId varGcdIssues: ${it.size}"
                )
                val issueIds = it.mapNotNull { item -> item.toRoomModel().variantOf }
                if (issueIds.isNotEmpty()) {
                    apiService.getIssues(issueIds)
                } else {
                    null
                }
            }
        }

        val exCreditsCall = CoroutineScope(Dispatchers.IO).async {
            nameDetailCall.await().let { gcdNameDetails ->
                Log.d(
                    TAG,
                    "refreshCredits getCreditsByNameDetail - $creatorId exGcdNameDetails: ${gcdNameDetails.size}"
                )
                apiService.getExtractedCreditsByNameDetail(gcdNameDetails.map {
                    it.nameDetail.nameDetailId
                })
            }
        }


        val exStoriesCall = CoroutineScope(Dispatchers.IO).async {
            exCreditsCall.await().let {
                Log.d(
                    TAG,
                    "refreshCredits getCreditsByNameDetail - $creatorId exGcdCredits: ${
                        it
                            .size
                    }"
                )
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
                Log.d(
                    TAG,
                    "refreshCredits getCreditsByNameDetail - $creatorId exGcdStory: ${
                        it
                            .size
                    }"
                )
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
                    Log.d(
                        TAG,
                        "refreshCredits getCreditsByNameDetail - $creatorId exVarGcdIssue: ${
                            it
                                .size
                        }"
                    )
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
                    val allStories = stories + exStories

                    val variants =
                        variantsCall.await()?.map { it.toRoomModel() } ?: emptyList() ?: emptyList()
                    val exVariants =
                        exVariantsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val issues = issuesCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val exIssues =
                        exIssuesCall.await()?.map { it.toRoomModel() } ?: emptyList()

                    val allIssues = variants + exVariants + issues + exIssues

                    val credits = creditsCall.await().map { it.toRoomModel() } ?: emptyList()

                    val exCredits =
                        exCreditsCall.await().map { it.toRoomModel() } ?: emptyList()

//                    val nameDetails: List<NameDetail> =
//                        nameDetailCall.await().map { it.toRoomModel() }
//

                    val series = CoroutineScope(Dispatchers.IO).async {
                        apiService.getSeriesByIds(allIssues.map { it.seriesId })
                    }.await().map { it.toRoomModel() }

                    database.transactionDao().upsertSus(
                        stories = allStories,
                        issues = allIssues,
                        credits = credits,
                        exCredits = exCredits,
                        series = series
                    )

                    Log.d(TAG, "UPDATED ENDING")
                }.let {
                    Repository.saveTime(prefs, CREATOR_TAG(creatorId))
                }
            }
        }
    }
}