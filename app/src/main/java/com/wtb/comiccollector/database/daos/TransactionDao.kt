package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Transaction
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.ComicCollectorApplication.Companion.context
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.BondType
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Role
import com.wtb.comiccollector.database.models.StoryType
import kotlinx.coroutines.ExperimentalCoroutinesApi

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

//    @Transaction
//    open suspend fun upsert(
//        stories: List<Story>? = null,
//        creators: List<Creator>? = null,
//        nameDetails: List<NameDetail>? = null,
//        credits: List<Credit>? = null,
//        exCredits: List<ExCredit>? = null,
//        issues: List<Issue>? = null,
//        series: List<Series>? = null,
//        seriesBonds: List<SeriesBond>? = null,
//        appearances: List<Appearance>? = null,
//        characters: List<Character>? = null,
//    ) {
//        series?.let { database.seriesDao().upsertSus(it) }
//        issues?.let { database.issueDao().upsertSus(it) }
//        stories?.let { database.storyDao().upsertSus(it) }
//        creators?.let { database.creatorDao().upsertSus(it) }
//        nameDetails?.let { database.nameDetailDao().upsertSus(it) }
//        credits?.let { database.creditDao().upsertSus(it) }
//        exCredits?.let { database.exCreditDao().upsertSus(it) }
//        seriesBonds?.let { database.seriesBondDao().upsertSus(it) }
//        appearances?.let { database.appearanceDao().upsertSus(it) }
//        characters?.let { database.characterDao().upsertSus(it) }
//    }
}
