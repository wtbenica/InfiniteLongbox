/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("RemoveExplicitTypeArguments")

package dev.benica.infinite_longbox.repository

//import android.util.Log
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.MainActivity
import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.database.IssueDatabase
import dev.benica.infinite_longbox.database.daos.REQUEST_LIMIT
import dev.benica.infinite_longbox.database.models.*
import dev.benica.infinite_longbox.views.ProgressUpdateCard
import dev.benica.infinite_longbox.views.hide
import dev.benica.infinite_longbox.views.show
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.util.Collections.sort
import java.util.concurrent.Executors


const val DUMMY_ID = Int.MAX_VALUE

private const val TAG = APP + "Repository"
const val DEBUG = false

internal const val SHARED_PREFS = "CCPrefs"


const val LOCAL_SERVER = "http://192.168.12.167:8000/"
const val REMOTE_SERVER = "https://infinite-longbox.uc.r.appspot.com/"
const val BASE_URL = LOCAL_SERVER

internal const val UPDATED_ROLES = "updated_roles"
internal const val UPDATED_STORY_TYPES = "updated_story_types"
internal const val UPDATED_PUBLISHERS = "updated_publishers"
internal const val UPDATED_BOND_TYPE = "update_bond_type"
internal const val UPDATED_BONDS = "update_series_bonds"
internal const val UPDATED_CREATORS = "updated_creators"
internal const val UPDATED_CREATORS_PAGE = "updated_creators_page"
internal const val UPDATED_SERIES = "updated_series"
internal const val UPDATED_SERIES_PAGE = "updated_series_page"
internal const val UPDATED_PUBLISHERS_PAGE = "updated_publishers_page"
internal const val UPDATED_CHARACTERS = "update_characters"
internal const val UPDATED_CHARACTERS_PAGE = "update_characters_page"
internal const val UPDATED_ISSUES = "updated_issues"
internal const val UPDATED_ISSUES_PAGE = "updated_issues_page"
internal const val UPDATED_STORIES = "updated_stories"
internal const val UPDATED_STORIES_PAGE = "updated_stories_page"
internal const val UPDATED_CREDITS = "updated_credits"
internal const val UPDATED_CREDITS_PAGE = "update_credits_page"
internal const val UPDATED_EXCREDITS = "updated_excredits"
internal const val UPDATED_EXCREDITS_PAGE = "updated_excredits_page"
internal const val UPDATED_APPEARANCES = "updated_appearances"
internal const val UPDATED_APPEARANCES_PAGE = "updated_appearances_page"
internal const val UPDATED_NAME_DETAILS = "update_name_details"
internal const val UPDATED_NAME_DETAILS_PAGE = "update_name_details_page"

internal const val MONTHLY: Long = 30
internal const val WEEKLY: Long = 7
internal const val DAILY: Long = 1

internal fun getUpdatedTag(id: Int, type: String): String = "$type${id}_UPDATED"

internal fun seriesTag(id: Int): String = getUpdatedTag(id, "SERIES_")
internal fun issueTag(id: Int) = getUpdatedTag(id, "ISSUE_")
internal fun creatorTag(id: Int): String = getUpdatedTag(id, "CREATOR_")
internal fun characterTag(id: Int): String = getUpdatedTag(id, "CHARACTER_")

@ExperimentalCoroutinesApi
class Repository private constructor(val context: Context) {

    var saveIssueListState: Parcelable? = null
        get() {
            val res = field
            Log.d(TAG, "Issue FIELD BEFORE = $field")
            field = null
            Log.d(TAG, "Issue FIELD AFTER = $field")
            return res
        }

    private val executor = Executors.newSingleThreadExecutor()
    private val issueDatabase: IssueDatabase
        get() = IssueDatabase.getInstance(context)
    private var hasConnection: Boolean = false
    private var hasUnmeteredConnection: Boolean = true

    private val seriesDao
        get() = issueDatabase.seriesDao()
    private val issueDao
        get() = issueDatabase.issueDao()
    private val creatorDao
        get() = issueDatabase.creatorDao()
    private val publisherDao
        get() = issueDatabase.publisherDao()
    private val roleDao
        get() = issueDatabase.roleDao()
    private val storyDao
        get() = issueDatabase.storyDao()
    private val creditDao
        get() = issueDatabase.creditDao()
    private val exCreditDao
        get() = issueDatabase.exCreditDao()
    private val storyTypeDao
        get() = issueDatabase.storyTypeDao()
    private val nameDetailDao
        get() = issueDatabase.nameDetailDao()
    private val characterDao
        get() = issueDatabase.characterDao()
    private val appearanceDao
        get() = issueDatabase.appearanceDao()
    private val collectionItemDao
        get() = issueDatabase.collectionItemDao()
    private val coverDao
        get() = issueDatabase.coverDao()

    private val updater: StaticUpdater
        get() = StaticUpdater.get()

    private fun checkConnectionStatus() =
        this.hasConnection && this.hasUnmeteredConnection

    internal fun beginStaticUpdate(progressUpdate: ProgressUpdateCard, mainActivity: MainActivity) {
        MainActivity.hasConnection.observeForever {
            this.hasConnection = it
            if (checkConnectionStatus()) {
                // TODO: A lint inspection pointed out that update returns a Deferred, which
                //  means that this is async async await. Look into
                MainActivity.activeJob =
                    CoroutineScope(Dispatchers.IO).async {
                        withContext(Dispatchers.Default) {
                            Log.d(TAG, "STARTING UPDATE")
                            mainActivity.runOnUiThread {
                                progressUpdate.show()
                            }
                            updater.updateStaticAsync(progressUpdate)
                        }.let {
                            Log.d(TAG, "Static update done")
                            mainActivity.runOnUiThread {
                                progressUpdate.hide()
                            }
                        }
                    }
            }
        }
    }

    // Static Items
    val allPublishers: Flow<List<Publisher>> = publisherDao.getAll()

    // FILTER OPTIONS
    fun getFilterOptionsSeries(filter: SearchFilter): Flow<List<FullSeries>> {
        return if (filter.mSeries == null) {
            seriesDao.getSeriesByFilter(filter)
        } else {
            emptyFlow()
        }
    }

    fun getFilterOptionsCharacter(filter: SearchFilter): Flow<List<Character>> =
        if (!filter.hasCharacter()) {
            characterDao.getCharacterFilterOptions(filter)
        } else {
            emptyFlow()
        }

    // TODO: This looks incorrect. it should filter to creators who have shared credits
    fun getFilterOptionsCreator(filter: SearchFilter): Flow<List<Creator>> =
        if (filter.mCreators.isEmpty()) {
            creatorDao.getCreatorsByFilter(filter)
        } else {
            flow { emit(emptyList<Creator>()) }
        }

    // SERIES METHODS
    fun getSeries(seriesId: Int): Flow<FullSeries?> = seriesDao.getSeries(seriesId)

    fun getSeriesByFilterPaged(filter: SearchFilter): Flow<PagingData<FullSeries>> {
        val newFilter = SearchFilter(filter)

        return Pager(
            config = PagingConfig(
                pageSize = REQUEST_LIMIT,
                enablePlaceholders = true
            ),
            pagingSourceFactory = {
                seriesDao.getSeriesByFilterPagingSource(newFilter)
            }
        ).flow
    }

    // CHARACTER METHODS
    fun getCharactersByFilterPaged(filter: SearchFilter): Flow<PagingData<FullCharacter>> {
        val newFilter = SearchFilter(filter)

        return Pager(
            config = PagingConfig(
                pageSize = REQUEST_LIMIT * 4,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { characterDao.getCharactersByFilterPagingSource(newFilter) }
        ).flow
    }

    fun getAppearancesByIssue(issueId: Int): Flow<List<FullAppearance>> =
        appearanceDao.getAppearancesByIssueId(issueId)

    // CREATOR METHODS
    fun getCreatorsByFilterPaged(filter: SearchFilter): Flow<PagingData<FullCreator>> {
        val newFilter = SearchFilter(filter)

        return Pager(
            config = PagingConfig(
                pageSize = REQUEST_LIMIT * 4,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { creatorDao.getCreatorsByFilterPagingSource(newFilter) }
        ).flow
    }

    // ISSUE METHODS
    fun getIssue(issueId: Int, markedDelete: Boolean = true): Flow<FullIssue?> {
        if (issueId != AUTO_ID) {
            CoroutineScope(Dispatchers.Default).launch {
                updater.expandIssueAsync(issueId, markedDelete)
            }
        }

        return issueDao.getFullIssue(issueId)
    }

    fun getIssuesByFilter(filter: SearchFilter): Flow<List<FullIssue>> {
        return issueDao.getIssuesByFilter(filter = filter)
    }

    fun getIssuesByFilterPaged(filter: SearchFilter): Flow<PagingData<FullIssue>> {
        return Pager(
            config = PagingConfig(
                pageSize = REQUEST_LIMIT,
                enablePlaceholders = true
            ),
            pagingSourceFactory = {
                issueDao.getIssuesByFilterPagingSource(filter = filter)
            }
        ).flow
    }

    // VARIANT METHODS
    fun getVariants(issueId: Int): Flow<List<Issue>> = issueDao.getVariants(issueId)

    // STORY METHODS
    fun getStoriesByIssue(issueId: Int): Flow<List<Story>> = storyDao.getStoriesFlow(issueId)

    // CREDIT METHODS``
    fun getCreditsByIssue(issueId: Int): Flow<List<FullCredit>> = combine(
        creditDao.getIssueCredits(issueId),
        exCreditDao.getIssueExtractedCredits(issueId)
    ) { credits1: List<FullCredit>?, credits2: List<FullCredit>? ->
        val res = (credits1 ?: emptyList()) + (credits2 ?: emptyList())
        sort(res)
        res
    }

    // PUBLISHER METHODS
    fun getFilterOptionsPublisher(filter: SearchFilter): Flow<List<Publisher>> =
        if (filter.mPublishers.isEmpty()) {
            publisherDao.getPublishersByFilter(filter)
        } else {
            flow { emit(emptyList<Publisher>()) }
        }

    fun updateIssueCover(issueId: Int) {
        if (hasConnection) {
            UpdateIssueCover.get().update(issueId)
        }
    }

    fun addToCollection(issue: FullIssue, collId: Int) {
        executor.execute {
            collectionItemDao.insert(
                CollectionItem(
                    issue = issue.issue.issueId,
                    series = issue.series.seriesId,
                    userCollection = collId
                )
            )
        }
    }

    fun removeFromCollection(issueId: Int, collId: Int) {
        executor.execute {
            collectionItemDao.deleteById(issueId, collId)
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: Repository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = Repository(context)
            }
        }

        fun get(): Repository {
            return INSTANCE
                ?: throw IllegalStateException("IssueRepository must be initialized")
        }

        fun removePrefs(prefs: SharedPreferences, keyPattern: String) {
            val editor = prefs.edit()
            val allPrefs = prefs.all
            for (kv in allPrefs) {
                if (Regex(keyPattern).matches(kv.key)) {
                    editor.remove(kv.key)
                }
            }
            editor.apply()
        }

        fun savePrefValue(prefs: SharedPreferences, key: String, value: Any) {
            val editor = prefs.edit()
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
                else -> throw IllegalArgumentException(
                    "savePrefValue: Yeah, it says Any, but it really wants String, Int, Boolean, Float, or Long"
                )
            }
            editor.apply()
        }

        fun saveTime(prefs: SharedPreferences, key: String) {
            savePrefValue(prefs, key, LocalDate.now().toString())
        }
    }

    fun saveSeries(vararg series: Series) {
        executor.execute {
            try {
                seriesDao.upsert(series.asList())
            } catch (e: SQLiteConstraintException) {
                Log.d(TAG, "addSeries: $e")
            }
        }
    }

    fun saveIssue(vararg issue: Issue) {
        executor.execute {
            try {
                issueDao.upsert(issue.asList())
            } catch (e: SQLiteConstraintException) {
                Log.d(TAG, "addIssue: $e")
            }
        }
    }

    fun saveCredit(vararg credit: Credit) {
        executor.execute {
            try {
                creditDao.upsert(credit.asList())
            } catch (e: SQLiteConstraintException) {
                Log.d(TAG, "addCredit: $e")
            }
        }
    }

    fun saveStory(vararg story: Story) {
        executor.execute {
            try {
                storyDao.upsert(story.asList())
            } catch (e: SQLiteConstraintException) {
                Log.d(TAG, "addStory: $e")
            }
        }
    }

    fun saveCreator(vararg creator: Creator) {
        executor.execute {
            try {
                creatorDao.upsert(creator.asList())
            } catch (e: SQLiteConstraintException) {
                Log.d(TAG, "addCreator: $e")
            }
        }
    }

    fun saveRole(vararg role: Role) {
        executor.execute {
            try {
                roleDao.upsert(role.asList())
            } catch (e: SQLiteConstraintException) {
                Log.d(TAG, "addRole: $e")
            }
        }
    }

    fun deleteSeries(series: Series) {
        executor.execute {
            seriesDao.delete(series)
        }
    }

    fun deleteIssue(issue: Issue) {
        executor.execute {
            issueDao.delete(issue)
        }
    }

    fun deleteCredit(credit: Credit) {
        executor.execute {
            creditDao.delete(credit)
        }
    }

    fun updateCharacterAsync(character: Character): Deferred<Unit> =
        Expander.get().expandCharacterAsync(listOf(character))

    fun updateSeriesAsync(series: Series): Deferred<Unit> = Expander.get().expandSeriesAsync(series)

    fun updateCreatorsAsync(creators: List<Creator>): Deferred<Unit> =
        Expander.get().expandCreatorsAsync(creators)

    fun cleanUpImages(seriesId: Int? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            val covers: List<Cover> = coverDao.getAll()
            Log.d(TAG, "There are ${covers.size} covers to cleanup")
            covers.forEach { cover ->
                if (cover.markedDelete) {
                    Log.d("${APP}CLEANUP", "STARTING")
                    val f = getFileHandle(context, coverFileName(cover.issue))
                    if (f.exists()) {
                        Log.d("${APP}CLEANUP", "Deleting")
                        f.delete()
                    }
                    coverDao.delete(cover)
                } else {
                    Log.d("${APP}CLEANUP", "Saving")
                }
            }
        }
    }

    fun markCoverSave(cid: Int) {
        markCover(cid, false)
    }

    fun markCoverDelete(cid: Int) {
        markCover(cid, true)
    }

    private fun markCover(cid: Int, isMarked: Boolean) =
        CoroutineScope(Dispatchers.Default).launch {
            val oc = coverDao.get(cid)
            if (oc != null) {
                val newCover = Cover(oc.coverId, oc.issue, oc.coverUri, isMarked)
                coverDao.upsert(newCover)
            }
        }


    class DuplicateFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.setTitle("Duplicate Issue")
                    .setMessage("This is a duplicate issue and will not be saved")
                    .setPositiveButton("OK") { dialogInterface: DialogInterface, i: Int -> }
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
    }

/*
    FUTURE IMPLEMENTATION
    fun getCoverImage(issue: Issue): File = File(filesDir, issue.coverFileName)
*/
}