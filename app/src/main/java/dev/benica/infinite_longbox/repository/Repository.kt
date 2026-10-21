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

package dev.benica.infinite_longbox.repository

//import android.util.Log
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.LetterIndex
import dev.benica.infinite_longbox.MainActivity
import dev.benica.infinite_longbox.SearchFilter
import dev.benica.infinite_longbox.database.IssueDatabase
import dev.benica.infinite_longbox.database.daos.REQUEST_LIMIT
import dev.benica.infinite_longbox.database.models.AUTO_ID
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.BackupCollection
import dev.benica.infinite_longbox.BackupEntry
import dev.benica.infinite_longbox.CollectionBackup
import dev.benica.infinite_longbox.ImportResult
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.database.models.CollectionItem
import dev.benica.infinite_longbox.database.models.Cover
import dev.benica.infinite_longbox.database.models.Creator
import dev.benica.infinite_longbox.database.models.Credit
import dev.benica.infinite_longbox.database.models.FullAppearance
import dev.benica.infinite_longbox.database.models.FullCharacter
import dev.benica.infinite_longbox.database.models.FullCreator
import dev.benica.infinite_longbox.database.models.FullCredit
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.database.models.Issue
import dev.benica.infinite_longbox.database.models.Publisher
import dev.benica.infinite_longbox.database.models.Role
import dev.benica.infinite_longbox.database.models.Series
import dev.benica.infinite_longbox.database.models.Story
import dev.benica.infinite_longbox.views.ProgressUpdateCard
import dev.benica.infinite_longbox.views.hide
import dev.benica.infinite_longbox.views.show
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Collections.sort
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


const val DUMMY_ID = Int.MAX_VALUE

private const val TAG = APP + "Repository"
const val DEBUG = false

internal const val SHARED_PREFS = "CCPrefs"


const val LOCAL_SERVER = "http://192.168.12.167:8000/"
const val REMOTE_SERVER = "https://infinite-longbox.uc.r.appspot.com/"
const val ADB_REVERSE_SERVER = "http://localhost:8000/" // adb reverse tcp:8000 tcp:8000
const val TAILNET_SERVER = "https://signal.tail86e26f.ts.net:8443/" // Caddy on Signal
const val BASE_URL = TAILNET_SERVER

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
internal fun issueDetailsTag(id: Int) = getUpdatedTag(id, "ISSUE_DETAILS_")
/**
 * How the long lists page. Placeholders give the scroll handle a whole-list length to work with;
 * a jump (dragging that handle, a big fling) then reloads at the new position instead of paging
 * through everything in between, and only a handful of pages are kept in memory, not every page
 * the user has ever scrolled past.
 */
private fun pagingConfig(pageSize: Int) = PagingConfig(
    pageSize = pageSize,
    enablePlaceholders = true,
    jumpThreshold = pageSize * 3,
    maxSize = pageSize * 10,
)

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
    private val userCollectionDao
        get() = issueDatabase.userCollectionDao()
    private val coverDao
        get() = issueDatabase.coverDao()

    private val updater: StaticUpdater
        get() = StaticUpdater.get()

    private fun checkConnectionStatus() =
        this.hasConnection && this.hasUnmeteredConnection

    // Only one static update may run at a time. Without this, every connectivity callback (and
    // every activity re-creation) started another loop that raced the others on saved progress.
    private val staticUpdateRunning = AtomicBoolean(false)

    internal fun beginStaticUpdate(progressUpdate: ProgressUpdateCard, mainActivity: MainActivity) {
        MainActivity.hasConnection.observe(mainActivity) {
            this.hasConnection = it
            if (checkConnectionStatus() && staticUpdateRunning.compareAndSet(false, true)) {
                MainActivity.activeJob =
                    CoroutineScope(Dispatchers.IO).async {
                        try {
                            withContext(Dispatchers.Default) {
                                Log.d(TAG, "STARTING UPDATE")
                                mainActivity.runOnUiThread {
                                    progressUpdate.show()
                                }
                                updater.updateStaticAsync(progressUpdate)
                            }
                            Log.d(TAG, "Static update done")
                        } finally {
                            // Hide the card however the update ended (done, failed, or the
                            // network was lost and the job canceled).
                            staticUpdateRunning.set(false)
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

    // Bounded, one-shot lookups for the filter-box popup. The Flow versions above return every
    // matching row, which is far too many (hundreds of thousands of characters) to hand to a popup.
    suspend fun getFilterOptionsSeriesLimited(filter: SearchFilter, limit: Int): List<FullSeries> =
        if (filter.mSeries == null) seriesDao.getSeriesOptions(filter, limit) else emptyList()

    suspend fun getFilterOptionsCharacterLimited(filter: SearchFilter, limit: Int): List<Character> =
        if (!filter.hasCharacter()) characterDao.getCharacterOptions(filter, limit) else emptyList()

    suspend fun getFilterOptionsCreatorLimited(filter: SearchFilter, limit: Int): List<Creator> =
        if (filter.mCreators.isEmpty()) creatorDao.getCreatorOptions(filter, limit) else emptyList()

    suspend fun getFilterOptionsPublisherLimited(filter: SearchFilter, limit: Int): List<Publisher> =
        if (filter.mPublishers.isEmpty()) publisherDao.getPublisherOptions(filter, limit) else emptyList()

    // SERIES METHODS
    fun getSeries(seriesId: Int): Flow<FullSeries?> = seriesDao.getSeries(seriesId)

    // For rebuilding a saved filter (see FilterSnapshot)
    suspend fun getCreatorById(id: Int): Creator? = withContext(Dispatchers.IO) { creatorDao.get(id) }
    suspend fun getPublisherById(id: Int): Publisher? =
        withContext(Dispatchers.IO) { publisherDao.get(id) }
    suspend fun getCharacterById(id: Int): Character? =
        withContext(Dispatchers.IO) { characterDao.get(id) }

    suspend fun getCharacterInfo(id: Int): CharacterInfo? = updater.characterInfo(id)

    suspend fun getCreatorInfo(id: Int): CreatorInfo? = updater.creatorInfo(id)

    fun getSeriesByFilterPaged(filter: SearchFilter): Flow<PagingData<FullSeries>> {
        val newFilter = SearchFilter(filter)

        return Pager(
            config = pagingConfig(REQUEST_LIMIT),
            pagingSourceFactory = {
                seriesDao.getSeriesByFilterPagingSource(newFilter)
            }
        ).flow
    }

    suspend fun getSeriesLetterIndex(filter: SearchFilter): LetterIndex =
        seriesDao.getLetterIndex(SearchFilter(filter))

    // CHARACTER METHODS
    fun getCharactersByFilterPaged(filter: SearchFilter): Flow<PagingData<FullCharacter>> {
        val newFilter = SearchFilter(filter)

        return Pager(
            config = pagingConfig(REQUEST_LIMIT * 4),
            pagingSourceFactory = { characterDao.getCharactersByFilterPagingSource(newFilter) }
        ).flow
    }

    fun getAppearancesByIssue(issueId: Int): Flow<List<FullAppearance>> =
        appearanceDao.getAppearancesByIssueId(issueId)

    // CREATOR METHODS
    fun getCreatorsByFilterPaged(filter: SearchFilter): Flow<PagingData<FullCreator>> {
        val newFilter = SearchFilter(filter)

        return Pager(
            config = pagingConfig(REQUEST_LIMIT * 4),
            pagingSourceFactory = { creatorDao.getCreatorsByFilterPagingSource(newFilter) }
        ).flow
    }

    // ISSUE METHODS
    /** The issues matching a scanned barcode, or null if the lookup failed. */
    suspend fun getIssuesByUpc(
        upc: String,
        number: String? = null,
        limit: Int? = null,
    ): List<FullIssue>? =
        updater.importIssuesByUpc(upc, number, limit)?.mapNotNull { issueDao.getIssueSus(it) }

    fun updateIssue(issueId: Int, markedDelete: Boolean = true) {
        if (issueId != AUTO_ID) {
            CoroutineScope(Dispatchers.Default).launch {
                updater.expandIssueAsync(issueId, markedDelete)
            }
        }
    }

    /** A list card's issue and cover: not the issue's stories and credits, which the page needs. */
    fun requestIssue(issueId: Int, markedDelete: Boolean = true) {
        if (issueId != AUTO_ID) updater.requestIssueAndCover(issueId, markedDelete)
    }

    /**
     * Fetches a cover for a series whose first issue has none (see [StaticUpdater.fetchSeriesCover]).
     * Asked once per series while the app runs, unless the server could not be reached: a card
     * binds again each time it scrolls into view.
     */
    fun fetchSeriesCover(seriesId: Int) {
        if (hasConnection) updater.requestSeriesCover(seriesId)
    }

    fun getIssue(issueId: Int, markedDelete: Boolean = true): Flow<FullIssue?> {
        updateIssue(issueId, markedDelete)
        return issueDao.getFullIssue(issueId)
    }

    fun getIssuesByFilter(filter: SearchFilter): Flow<List<FullIssue>> {
        return issueDao.getIssuesByFilter(filter = filter)
    }

    fun getIssuesByFilterPaged(filter: SearchFilter): Flow<PagingData<FullIssue>> {
        return Pager(
            config = pagingConfig(REQUEST_LIMIT),
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

    /** My Collection and the Wish List as the text of a backup file. */
    suspend fun exportCollections(exportedAt: String): String = withContext(Dispatchers.IO) {
        val rows = collectionItemDao.exportRows()
        val names = userCollectionDao.names().associate { it.userCollectionId to it.name }
        CollectionBackup.toJson(
            collections = listOf(BaseCollection.MY_COLL.id, BaseCollection.WISH_LIST.id).map { id ->
                BackupCollection(
                    id = id,
                    name = names[id] ?: if (id == BaseCollection.MY_COLL.id) "My Collection" else "Wish List",
                    issues = rows.filter { it.collectionId == id }
                        .map { BackupEntry(it.issueId, it.seriesId, it.series, it.number) }
                )
            },
            exportedAt = exportedAt
        )
    }

    /**
     * Puts the issues in [collections] into the collections they came from. Issues the phone does
     * not have are fetched from the server first; the series they belong to must have been
     * synced or can be fetched along with them. Adding is the only thing it does, so importing
     * the same file twice changes nothing.
     */
    suspend fun importCollections(collections: List<BackupCollection>): ImportResult =
        withContext(Dispatchers.IO) {
            val wanted = collections.flatMap { collection -> collection.issues.map { collection.id to it } }
            val ids = wanted.map { it.second.issueId }.distinct()

            suspend fun present(): Map<Int, Int> = ids.chunked(IMPORT_CHUNK).flatMap {
                issueDao.issueSeries(it)
            }.associate { it.issueId to it.series }

            var have = present()
            val missing = ids.filter { it !in have }
            if (missing.isNotEmpty()) {
                updater.importIssuesById(missing)
                have = present()
            }

            var added = 0
            var alreadyThere = 0
            val unavailable = mutableListOf<BackupEntry>()
            wanted.forEach { (collectionId, entry) ->
                val series = have[entry.issueId]
                if (series == null) {
                    if (unavailable.none { it.issueId == entry.issueId }) unavailable.add(entry)
                } else if (collectionItemDao.insertSus(
                        CollectionItem(issue = entry.issueId, series = series, userCollection = collectionId)
                    ) == -1L
                ) {
                    alreadyThere++
                } else {
                    added++
                }
            }
            ImportResult(added, alreadyThere, unavailable)
        }

    fun removeFromCollection(issueId: Int, collId: Int) {
        executor.execute {
            collectionItemDao.deleteById(issueId, collId)
        }
    }

    companion object {
        /** Issues looked up at a time on import: SQLite allows 999 variables in a query. */
        private const val IMPORT_CHUNK = 500

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
            prefs.edit {
                val allPrefs = prefs.all
                for ((key) in allPrefs) {
                    if (Regex(keyPattern).matches(key)) {
                        remove(key)
                    }
                }
            }
        }

        fun savePrefValue(prefs: SharedPreferences, key: String, value: Any) {
            prefs.edit {
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                    is Long -> putLong(key, value)
                    else -> throw IllegalArgumentException(
                        "savePrefValue: Yeah, it says Any, but it really wants String, Int, Boolean, Float, or Long"
                    )
                }
            }
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

    fun deleteIssue(issue: Issue) {
        executor.execute {
            issueDao.delete(issue)
        }
    }

    fun updateCharacterAsync(character: Character): Deferred<Unit> =
        Expander.get().expandCharacterAsync(listOf(character))

    fun updateSeriesAsync(series: Series): Deferred<Unit> = Expander.get().expandSeriesAsync(series)

    fun updateCreatorsAsync(creators: List<Creator>): Deferred<Unit> =
        Expander.get().expandCreatorsAsync(creators)

    /**
     * Keeps the covers of what was only browsed to the newest [CoverPlan.KEEP_BROWSED]: they
     * used to all go every time the app stopped, to be fetched again on the next visit. Covers
     * of the collection stay, and so does the record that an issue has no cover.
     */
    fun cleanUpImages(seriesId: Int? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            val browsed = coverDao.getAll().filter { it.markedDelete && it.coverUri != null }
            val trim = CoverPlan.oldestBeyond(browsed, CoverPlan.KEEP_BROWSED) { it.lastUpdated }
            Log.d(TAG, "There are ${browsed.size} browsed covers; removing ${trim.size}")
            trim.forEach { cover ->
                val f = getFileHandle(context, coverFileName(cover.issue))
                if (f.exists()) f.delete()
                coverDao.delete(cover)
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
*/
}