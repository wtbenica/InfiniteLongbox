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

private const val TAG = APP + "UpdateIssueCredit"

/***
 * Updates an issues credits
 */
@ExperimentalCoroutinesApi
class UpdateIssueCredit(
    webservice: Webservice,
    database: IssueDatabase,
    prefs: SharedPreferences,
) : Updater(webservice, database, prefs) {
    internal fun update(issueId: Int) {
        if (Companion.checkIfStale(ISSUE_TAG(issueId), ISSUE_LIFETIME, prefs)) {
            val storyItemsCall = CoroutineScope(Dispatchers.IO).async stories@{
                var result = emptyList<Item<GcdStory, Story>>()

                try {
                    result = webservice.getStoriesByIssue(issueId)
                } catch (e: SocketTimeoutException) {
                    Log.d(TAG, "update: Getting Stories: $e")
                } catch (e: ConnectException) {
                    Log.d(TAG, "update: Getting Stories: $e")
                }

                return@stories result
            }

            val creditItemsCall = CoroutineScope(Dispatchers.IO).async credits@{
                var result: List<Item<GcdCredit, Credit>>? = null

                storyItemsCall.await().let { storyItems ->
                    if (storyItems.isNotEmpty()) {
                        try {
                            Log.d(TAG, "CREDIT_UPDATE Refreshing issue credits getCredits $issueId")
                            result =
                                webservice.getCreditsByStoryIds(storyItems.map { item -> item.pk })
                        } catch (e: SocketTimeoutException) {
                            Log.d(TAG, "update: Getting Credits: $e")
                        } catch (e: ConnectException) {
                            Log.d(TAG, "update: Getting Credits: $e")
                        }
                    }
                }

                return@credits result
            }

            val extractedCreditItemsCall = CoroutineScope(Dispatchers.IO).async exCredits@{
                var result: List<Item<GcdExCredit, ExCredit>>? = null

                storyItemsCall.await().let { storyItems ->
                    if (storyItems.isNotEmpty()) {
                        try {
                            Log.d(
                                TAG,
                                "CREDIT_UPDATE Refreshing issue credits getExCredits $issueId"
                            )
                            result =
                                webservice.getExtractedCreditsByStories(storyItems.map { item -> item.pk })
                        } catch (e: SocketTimeoutException) {
                            Log.d(TAG, "update: Getting exCredits: $e")
                        } catch (e: ConnectException) {
                            Log.d(TAG, "update: Getting exCredits: $e")
                        }
                    }

                    return@exCredits result
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                val stories = storyItemsCall.await().map { it.toRoomModel() }
                val credits = creditItemsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                val extracts =
                    extractedCreditItemsCall.await()?.map { it.toRoomModel() } ?: emptyList()

                database.transactionDao().upsert(
                    stories = stories,
                    credits = credits,
                    exCredits = extracts,
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