package com.wtb.comiccollector.database.daos

import android.content.Context
import androidx.room.Dao
import androidx.room.Transaction
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.BondType
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Role
import com.wtb.comiccollector.database.models.StoryType
import com.wtb.comiccollector.repository.Repository
import com.wtb.comiccollector.repository.SHARED_PREFS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

private const val TAG = APP + "TransactionDao"

@ExperimentalCoroutinesApi
@Dao
abstract class TransactionDao {

    private val database: IssueDatabase
        get() = IssueDatabase.getInstance(context!!)

    @Transaction
    open suspend fun upsertStatic(
        publishers: List<Publisher>? = null,
        roles: List<Role>? = null,
        storyTypes: List<StoryType>? = null,
        bondTypes: List<BondType>? = null,
    ) {
        publishers?.let { if (it.isNotEmpty()) database.publisherDao().upsertSus(it) }
        roles?.let { if (it.isNotEmpty()) database.roleDao().upsertSus(it) }
        storyTypes?.let { if (it.isNotEmpty()) database.storyTypeDao().upsertSus(it) }
        bondTypes?.let { if (it.isNotEmpty()) database.bondTypeDao().upsertSus(it) }
    }

    fun cleanDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            database.collectionItemDao().dropAll()
            database.userCollectionDao().dropAll()
            database.coverDao().dropAll()
            database.appearanceDao().dropAll()
            database.creditDao().dropAll()
            database.exCreditDao().dropAll()
            database.storyDao().dropAll()
            database.issueDao().dropAll()
            context?.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)?.let {
                Repository.removePrefs(
                    prefs = it,
                    keyPattern = "^((?!_page).)*\$"
                )
            }
        }
    }
}
