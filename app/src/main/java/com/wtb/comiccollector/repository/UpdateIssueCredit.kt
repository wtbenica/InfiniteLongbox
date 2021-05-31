package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.IssueDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val TAG = APP + "UpdateIssueCredit"

/***
 * Updates an issues credits
 */
class UpdateIssueCredit(
    val apiService: Webservice,
    val database: IssueDatabase,
    val prefs: SharedPreferences
) {
    internal fun updateAll(issueIds: List<Int>) {
        issueIds.forEach { update(it) }
    }

    internal fun update(issueId: Int) {
        val equis = Random.nextInt()
        if (Repository.checkIfStale(ISSUE_TAG(issueId), ISSUE_LIFETIME, prefs)) {
            Log.d(TAG, "CREDIT_UPDATE Refreshing issue credits getStory $issueId")
            val storyItemsCall = CoroutineScope(Dispatchers.IO).async {
                apiService.getStoriesByIssue(issueId)
            }

            val creditItemsCall = CoroutineScope(Dispatchers.IO).async {
                storyItemsCall.await().let { storyItems ->
                    if (storyItems.isNotEmpty()) {
                        Log.d(TAG, "CREDIT_UPDATE Refreshing issue credits getCredits $issueId")
                        apiService.getCreditsByStories(storyItems.map { item -> item.pk })
                    } else {
                        null
                    }
                }
            }

            val nameDetailItemsCall = CoroutineScope(Dispatchers.IO).async {
                creditItemsCall.await()?.let { creditItems ->
                    if (creditItems.isNotEmpty()) {
                        Log.d(TAG, "CREDIT_UPDATE Refreshing issue credits getNameDetails $issueId")
                        apiService.getNameDetailsByIds(creditItems.map { it.fields.nameDetailId })
                    } else {
                        null
                    }
                }
            }

            val creatorItemsCall = CoroutineScope(Dispatchers.IO).async {
                nameDetailItemsCall.await()?.let { nameDetailItems ->
                    if (nameDetailItems.isNotEmpty()) {
                        Log.d(TAG, "CREDIT_UPDATE Refreshing issue credits getCreator $issueId")
                        apiService.getCreator(nameDetailItems.map { it.fields.creatorId })
                    } else {
                        null
                    }
                }
            }

            val extractedCreditItemsCall = CoroutineScope(Dispatchers.IO).async {
                storyItemsCall.await().let { storyItems ->
                    if (storyItems.isNotEmpty()) {
                        Log.d(TAG, "CREDIT_UPDATE Refreshing issue credits getExCredits $issueId")
                        apiService.getExtractedCreditsByStories(storyItems.map { item -> item.pk })
                    } else {
                        null
                    }
                }
            }

            val extractedNameDetailItemsCall = CoroutineScope(Dispatchers.IO).async {
                extractedCreditItemsCall.await()?.let { creditItems ->
                    if (creditItems.isNotEmpty()) {
                        Log.d(
                            TAG,
                            "CREDIT_UPDATE Refreshing issue credits getExNameDetails $issueId"
                        )
                        apiService.getNameDetailsByIds(creditItems.map { it.fields.nameDetailId })
                    } else {
                        null
                    }
                }
            }

            val extractedCreatorItemsCall = CoroutineScope(Dispatchers.IO).async {
                extractedNameDetailItemsCall.await()?.let { nameDetailItems ->
                    if (nameDetailItems.isNotEmpty()) {
                        Log.d(TAG, "CREDIT_UPDATE Refreshing issue credits getExCreator $issueId")
                        apiService.getCreator(nameDetailItems.map { it.fields.creatorId })
                    } else {
                        null
                    }
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                Log.d(TAG, "CREDIT_UPDATE About to upsert $equis")
                val stories = storyItemsCall.await().map { it.toRoomModel() }
                val credits = creditItemsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                val nameDetails =
                    nameDetailItemsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                val creators = creatorItemsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                val extracts =
                    extractedCreditItemsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                val eNameDetails =
                    extractedNameDetailItemsCall.await()?.map { it.toRoomModel() }
                        ?: emptyList()
                val eCreators =
                    extractedCreatorItemsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                val allCreators = creators + eCreators
                val allNameDetails = nameDetails + eNameDetails

                database.transactionDao().upsertSus(
                    stories = stories,
                    credits = credits,
                    exCredits = extracts,
                    nameDetails = allNameDetails,
                    creators = allCreators,
                )
                    .let {
                        Log.d(
                            TAG,
                            "CREDIT_UPDATE Refreshing issue credits getExNameDetails $issueId"
                        )
                        //  CharacterExtractor().extractCharacters(storyItems.await())
                        //  CreditExtractor().extractCredits(storyItemsCall.await())
                    }
            }
        }
    }
}