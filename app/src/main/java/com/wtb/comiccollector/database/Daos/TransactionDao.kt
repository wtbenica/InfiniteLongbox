package com.wtb.comiccollector.database.Daos

import android.util.Log
import androidx.room.Dao
import androidx.room.Transaction
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.*

private const val TAG = APP + "TransactionDao"

@Dao
abstract class TransactionDao(private val database: IssueDatabase) {

    @Transaction
    open suspend fun upsertStatic(
        publishers: List<Publisher>? = null,
        roles: List<Role>? = null,
        storyTypes: List<StoryType>? = null,
    ) {
        publishers?.let { database.publisherDao().upsertSus(it) }
        roles?.let { database.roleDao().upsertSus(it) }
        storyTypes?.let { database.storyTypeDao().upsertSus(it) }
    }

    @Transaction
    open fun upsert(
        stories: List<Story>? = null,
        creators: List<Creator>? = null,
        nameDetails: List<NameDetail>? = null,
        credits: List<Credit>? = null,
        issues: List<Issue>? = null
    ) {
        Log.d(TAG, "1----------------------------------------------------------1")
        issues?.let { database.issueDao().upsert(it) }
        Log.d(TAG, "2----------------------------------------------------------1")
        stories?.let { database.storyDao().upsert(it) }
        Log.d(TAG, "3----------------------------------------------------------1")
        creators?.let { database.creatorDao().upsert(it) }
        Log.d(TAG, "4----------------------------------------------------------1")
        nameDetails?.let { database.nameDetailDao().upsert(it) }
        Log.d(TAG, "5----------------------------------------------------------1")
        credits?.let { database.creditDao().upsert(it) }
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
        Log.d(TAG, "upsert series $series")
        series?.let { database.seriesDao().upsertSus(it) }
        Log.d(TAG, "upsert issue $issues")
        issues?.let { database.issueDao().upsertSus(it) }
        Log.d(TAG, "upsert story")
        stories?.let { database.storyDao().upsertSus(it) }
        Log.d(TAG, "upsert creator")
        creators?.let { database.creatorDao().upsertSus(it) }
        Log.d(TAG, "upsert namedetail")
        nameDetails?.let { database.nameDetailDao().upsertSus(it) }
        Log.d(TAG, "upsert credits")
        credits?.let { database.creditDao().upsertSus(it) }
        Log.d(TAG, "upsert excredits")
        exCredits?.let { database.exCreditDao().upsertSus(it) }
    }
}