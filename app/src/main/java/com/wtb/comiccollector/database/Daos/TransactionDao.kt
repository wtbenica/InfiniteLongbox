package com.wtb.comiccollector.database.Daos

import android.util.Log
import androidx.room.Dao
import androidx.room.Transaction
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "TransactionDao"

@ExperimentalCoroutinesApi
@Dao
abstract class TransactionDao(private val database: IssueDatabase) {

    @Transaction
    open suspend fun upsertStatic(
        publishers: List<Publisher>? = null,
        roles: List<Role>? = null,
        storyTypes: List<StoryType>? = null,
    ) {
        Log.d(TAG, "upsertStatic: Starting")
        publishers?.let { database.publisherDao().upsertSus(it) }
        roles?.let { database.roleDao().upsertSus(it) }
        storyTypes?.let { database.storyTypeDao().upsertSus(it) }
    }

    @Transaction
    open suspend fun upsertSus(
        stories: List<Story>? = null,
        creators: List<Creator>? = null,
        nameDetails: List<NameDetail>? = null,
        credits: List<Credit>? = null,
        exCredits: List<ExCredit>? = null,
        issues: List<Issue>? = null,
        series: List<Series>? = null
    ) {
        series?.let { database.seriesDao().upsertSus(it) }
        issues?.let { database.issueDao().upsertSus(it) }
        stories?.let { database.storyDao().upsertSus(it) }
        creators?.let { database.creatorDao().upsertSus(it) }
        nameDetails?.let { database.nameDetailDao().upsertSus(it) }
        credits?.let { database.creditDao().upsertSus(it) }
        exCredits?.let { database.exCreditDao().upsertSus(it) }
    }
}
