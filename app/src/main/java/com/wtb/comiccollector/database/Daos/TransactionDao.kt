package com.wtb.comiccollector.database.Daos

import android.util.Log
import androidx.room.Dao
import androidx.room.Transaction
import com.wtb.comiccollector.*
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.*

@Dao
abstract class TransactionDao(private val database: IssueDatabase) {

    @Transaction
    open suspend fun upsertStatic(
        publishers: List<Publisher>? =null,
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
        Log.d("Trans", "1----------------------------------------------------------1")
        issues?.let { database.issueDao().upsert(it) }
        Log.d("Trans", "2----------------------------------------------------------1")
        stories?.let { database.storyDao().upsert(it) }
        Log.d("Trans", "3----------------------------------------------------------1")
        creators?.let { database.creatorDao().upsert(it) }
        Log.d("Trans", "4----------------------------------------------------------1")
        nameDetails?.let { database.nameDetailDao().upsert(it) }
        Log.d("Trans", "5----------------------------------------------------------1")
        credits?.let { database.creditDao().upsert(it) }
    }

    @Transaction
    open suspend fun upsertSus(
        stories: List<Story>? = null,
        creators: List<Creator>? = null,
        nameDetails: List<NameDetail>? = null,
        credits: List<Credit>? = null,
        issues: List<Issue>? = null
    ) {
        Log.d("Trans", "1----------------------------------------------------------1")
        issues?.let { database.issueDao().upsertSus(it) }
        Log.d("Trans", "2----------------------------------------------------------1")
        stories?.let { database.storyDao().upsertSus(it) }
        Log.d("Trans", "3----------------------------------------------------------1")
        creators?.let { database.creatorDao().upsertSus(it) }
        Log.d("Trans", "4----------------------------------------------------------1")
        nameDetails?.let { database.nameDetailDao().upsertSus(it) }
        Log.d("Trans", "5----------------------------------------------------------1")
        credits?.let { database.creditDao().upsertSus(it) }
    }
}