package com.wtb.comiccollector.repository

//import android.util.Log
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.MainActivity
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.Daos.Count
import com.wtb.comiccollector.database.Daos.REQUEST_LIMIT
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.SimpleMigration
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.network.RetrofitAPIClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.Collections.sort
import java.util.concurrent.Executors


const val DUMMY_ID = Int.MAX_VALUE

private const val DATABASE_NAME = "issue-database"
private const val TAG = APP + "Repository"
const val DEBUG = false

internal const val SHARED_PREFS = "CCPrefs"

internal const val ISSUE_LIFETIME: Long = 30
internal const val CREATOR_LIFETIME: Long = 7


const val EXTERNAL = "http://24.176.172.169/"
const val NIGHTWING = "http://192.168.0.141:8000/"
const val ALFRED = "http://192.168.0.138:8000/"
const val BASE_URL = ALFRED

internal const val UPDATED_CREATORS = "updated_creators"
internal const val UPDATED_PUBLISHERS = "updated_publishers"
internal const val UPDATED_ROLES = "updated_roles"
internal const val UPDATED_STORY_TYPES = "updated_story_types"
internal const val UPDATED_SERIES = "updated_series"

internal const val STATIC_DATA_LIFETIME: Long = 30
internal const val SERIES_LIST_LIFETIME: Long = 7

internal fun UPDATED_TAG(id: Int, type: String): String = "$type${id}_UPDATED"

internal fun SERIES_TAG(id: Int): String = UPDATED_TAG(id, "SERIES_")
internal fun ISSUE_TAG(id: Int) = UPDATED_TAG(id, "ISSUE_")
internal fun PUBLISHER_TAG(id: Int): String = UPDATED_TAG(id, "PUBLISHER_")
internal fun CREATOR_TAG(id: Int): String = UPDATED_TAG(id, "CREATOR_")

@ExperimentalCoroutinesApi
class Repository private constructor(val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)

    private val executor = Executors.newSingleThreadExecutor()
    private val database: IssueDatabase = buildDatabase(context)
    private var hasConnection: Boolean = false
    private var hasUnmeteredConnection: Boolean = true
    private var isIdle = true

    private val seriesDao = database.seriesDao()
    private val issueDao = database.issueDao()
    private val creatorDao = database.creatorDao()
    private val publisherDao = database.publisherDao()
    private val roleDao = database.roleDao()
    private val storyDao = database.storyDao()
    private val creditDao = database.creditDao()
    private val exCreditDao = database.exCreditDao()
    private val storyTypeDao = database.storyTypeDao()
    private val nameDetailDao = database.nameDetailDao()
    private val characterDao = database.characterDao()
    private val appearanceDao = database.appearanceDao()
    private val collectionDao = database.collectionDao()

    private val filesDir = context.applicationContext.filesDir

    private val retrofit = RetrofitAPIClient.getRetrofitClient()

    private val apiService: Webservice by lazy {
        retrofit.create(Webservice::class.java)
    }

    init {
        MainActivity.hasConnection.observeForever {
            hasConnection = it
            if (checkConnectionStatus()) {
                isIdle = false
                // TODO: A lint inspection pointed out that update returns a Deferred, which
                //  means that this is async async await. Look into
                MainActivity.activeJob = CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Default) {
                        StaticUpdater(apiService, database, prefs).updateAsync()
                    }.let {
                        Log.d(TAG, "Static update done")
                        isIdle = true
                    }
                }
            }
        }

//        MainActivity.hasUnmeteredConnection.observeForever {
//            hasUnmeteredConnection = it
//            if (checkConnectionStatus()) {
//                StaticUpdater(apiService, database, prefs).update()
//            }
//        }
    }

    private fun checkConnectionStatus() = hasConnection && hasUnmeteredConnection && isIdle

    // Static Items
    val allSeries: Flow<List<Series>> = seriesDao.getAll()
    val allPublishers: Flow<List<Publisher>> = publisherDao.getPublishersList()
    val allCreators: Flow<List<Creator>> = creatorDao.getCreatorsList()
    val allRoles: Flow<List<Role>> = roleDao.getRoleList()

    // SERIES METHODS
    fun getSeries(seriesId: Int): Flow<Series?> = seriesDao.getSeries(seriesId)

    fun getSeriesByFilterPaged(filter: SearchFilter): Flow<PagingData<FullSeries>> {
        val newFilter = SearchFilter(filter)
        Log.d(TAG, "getSeriesByFilterPaged")
        val mSeries = newFilter.mSeries
        if (mSeries == null) {
            refreshFilterOptions(mSeries, newFilter.mCreators)
        } else {
            Log.d(TAG, "getSeriesByFilterPagingSource: Wasn't expecting to see a series here")
            newFilter.mSeries = null
        }

        return Pager(
            config = PagingConfig(
                pageSize = REQUEST_LIMIT,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { seriesDao.getSeriesByFilterPagingSource(newFilter) }
        ).flow
    }

    fun getSeriesByFilter(filter: SearchFilter): Flow<List<Series>> {
        Log.d(TAG, "getSeriesByFilter")
        val mSeries = filter.mSeries
        return if (mSeries == null) {
            refreshFilterOptions(mSeries, filter.mCreators)
            seriesDao.getSeriesByFilter(filter)
        } else {
            flow { emit(emptyList<Series>()) }
        }
    }

    // ISSUE METHODS
    fun getIssue(issueId: Int): Flow<FullIssue?> {
        if (hasConnection) {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Default) {
                    UpdateIssueCover(database, context, prefs).update(issueId = issueId)
                }.let {
                    UpdateIssueCredit(apiService, database, prefs).update(issueId = issueId)
                }
            }
        }

        return issueDao.getFullIssue(issueId = issueId)
    }

    fun getIssuesByFilter(filter: SearchFilter): Flow<List<FullIssue>> {
        val mSeries = filter.mSeries
        if (mSeries != null) {
            refreshFilterOptions(series = mSeries, creators = filter.mCreators)
        }
        return issueDao.getIssuesByFilter(filter = filter)
    }

    fun getIssuesByFilterPaged(filter: SearchFilter): Flow<PagingData<FullIssue>> {
        val mSeries = filter.mSeries

        if (mSeries != null) {
            refreshFilterOptions(series = mSeries, creators = filter.mCreators)
        }

        return Pager(
            config = PagingConfig(pageSize = REQUEST_LIMIT, enablePlaceholders = true),
            pagingSourceFactory = {
                issueDao.getIssuesByFilterPagingSource(filter = filter)
            }
        ).flow
    }

    // VARIANT METHODS
    fun getVariants(issueId: Int): Flow<List<Issue>> = issueDao.getVariants(issueId)

    // STORY METHODS
    fun getStoriesByIssue(issueId: Int): Flow<List<Story>> = storyDao.getStories(issueId)

    // CREDIT METHODS
    fun getCreditsByIssue(issueId: Int): Flow<List<FullCredit>> = combine(
        creditDao.getIssueCredits(issueId),
        exCreditDao.getIssueExtractedCredits(issueId)
    ) { credits1: List<FullCredit>?, credits2: List<FullCredit>? ->
        val res = (credits1 ?: emptyList()) + (credits2 ?: emptyList())
        sort(res)
        res
    }

    // CREATOR METHODS
    fun getCreatorsByFilter(filter: SearchFilter): Flow<List<Creator>> {
        Log.d(TAG, "getCreatorsByFilter")
        return if (filter.mCreators.isEmpty()) {
            creatorDao.getCreatorsByFilter(filter)
        } else {
            flow { emit(emptyList<Creator>()) }
        }
    }

    // PUBLISHER METHODS
    fun getPublishersByFilter(filter: SearchFilter): Flow<List<Publisher>> {
        Log.d(TAG, "getPublishersByFilter")
        return if (filter.mPublishers.isEmpty()) {
            publisherDao.getPublishersByFilter(filter)
        } else {
            flow { emit(emptyList<Publisher>()) }
        }
    }

    fun getPublisher(publisherId: Int): Flow<Publisher?> =
        publisherDao.getPublisher(publisherId)

    private fun refreshFilterOptions(series: Series?, creators: Set<Creator>) {
        val creatorIds = creators.map { it.creatorId }

        if (hasConnection && creatorIds.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                UpdateCreator(
                    apiService = apiService,
                    database = database,
                    prefs = prefs
                ).updateAll(creatorIds = creatorIds)
            }
        }

        series?.let {
            if (hasConnection) {
                UpdateSeries(webservice = apiService, database = database, prefs = prefs)
                    .update(seriesId = it.seriesId)
            }
        }
    }

    suspend fun getValidFilterOptions(filter: SearchFilter): Flow<List<FilterOptionAutoCompletePopupItem>> {
        val seriesList: Flow<List<FilterOptionAutoCompletePopupItem>> =
            when {
                filter.isEmpty()       -> allSeries
                filter.mSeries == null -> getSeriesByFilter(filter)
                else                   -> flow { emit(emptyList<FilterOptionAutoCompletePopupItem>()) }
            }

        val creatorsList: Flow<List<FilterOptionAutoCompletePopupItem>> =
            when {
                filter.isEmpty()           -> allCreators
                filter.mCreators.isEmpty() -> getCreatorsByFilter(filter)
                else                       -> flow { emit(emptyList<FilterOptionAutoCompletePopupItem>()) }
            }

        val publishersList: Flow<List<FilterOptionAutoCompletePopupItem>> =
            when {
                filter.isEmpty()             -> allPublishers
                filter.mPublishers.isEmpty() -> publisherDao.getPublishersByFilter(filter)
                else                         -> flow { emit(emptyList<FilterOptionAutoCompletePopupItem>()) }
            }

        return combine(
            seriesList,
            creatorsList,
            publishersList
        )
        { series: List<FilterOptionAutoCompletePopupItem>, creators: List<FilterOptionAutoCompletePopupItem>, publishers: List<FilterOptionAutoCompletePopupItem> ->
            val res: List<FilterOptionAutoCompletePopupItem> = series + creators + publishers
            sort(res)
            res
        }
    }

    fun addToCollection(issueId: Int) {
        executor.execute {
            collectionDao.insert(MyCollection(issueId = issueId))
        }
    }

    fun removeFromCollection(issueId: Int) {
        executor.execute {
            collectionDao.deleteById(issueId)
        }
    }

    fun inCollection(issueId: Int): Flow<Count> = collectionDao.inCollection(issueId)

/*
    FUTURE IMPLEMENTATION
    fun getCoverImage(issue: Issue): File = File(filesDir, issue.coverFileName)
*/

    /**
     * Builds database and adds dummy publisher and series, which are used for creating new empty
     * issue objects
     */
    @Language("RoomSql")
    private fun buildDatabase(context: Context): IssueDatabase = Room.databaseBuilder(
        context.applicationContext,
        IssueDatabase::class.java,
        DATABASE_NAME
    ).addCallback(
        object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val publisher =
                    Publisher(publisherId = DUMMY_ID, publisher = "Dummy Publisher")
                executor.execute {
                    publisherDao.upsert(
                        publisher,
                    )

                    seriesDao.upsert(
                        Series(
                            seriesId = DUMMY_ID,
                            seriesName = "Dummy Series",
                            publisherId = DUMMY_ID,
                            startDate = LocalDate.MIN,
                            endDate = LocalDate.MIN,
                        )
                    )
                }
            }
        }
    ).addMigrations(
        SimpleMigration(
            1, 2,
            """
       ALTER TABLE issue
       ADD COLUMN publicationDate TEXT
       """,
            """
       ALTER TABLE issue
       ADD COLUMN onSaleDate TEXT
       """)
    ).build()

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

        fun saveTime(prefs: SharedPreferences, key: String) {
            val editor = prefs.edit()
            editor.putString(key, LocalDate.now().toString())
            editor.apply()
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

    fun updateIssue(issue: FullIssue) {
        if (hasConnection) {
            UpdateIssueCredit(apiService, database, prefs).update(issue.issue.issueId)
            UpdateIssueCover(database, context, prefs).update(issue.issue.issueId)
        }
    }

    fun updateIssueCover(issue: FullIssue) {
        if (hasConnection) {
            UpdateIssueCover(database, context, prefs).update(issue.issue.issueId)
        }
    }
}