package com.wtb.comiccollector.repository

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.Webservice
import com.wtb.comiccollector.database.Daos.Count
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.util.Collections.sort
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


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

internal const val STATIC_DATA_UPDATED = "static_data_updated"
internal const val STATIC_DATA_LIFETIME: Long = 30
internal const val SERIES_LIST_UPDATED = "series_list_updated"
internal const val SERIES_LIST_LIFETIME: Long = 7

internal fun UPDATED_TAG(id: Int, type: String): String = "$type${id}_UPDATED"
internal fun SERIES_TAG(id: Int): String = UPDATED_TAG(id, "SERIES_")
internal fun ISSUE_TAG(id: Int) = UPDATED_TAG(id, "ISSUE_")
internal fun PUBLISHER_TAG(id: Int): String = UPDATED_TAG(id, "PUBLISHER_")
internal fun CREATOR_TAG(id: Int): String = UPDATED_TAG(id, "CREATOR_")

class Repository private constructor(val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)

    private val executor = Executors.newSingleThreadExecutor()

    private val database: IssueDatabase = buildDatabase(context)

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

    private val retrofit: Retrofit by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val apiService: Webservice by lazy {
        retrofit.create(Webservice::class.java)
    }

    init {
        StaticUpdater(apiService, database, prefs).update()
    }

    val allSeries: Flow<List<Series>> = seriesDao.getAllOfThem()
    val allPublishers: Flow<List<Publisher>> = publisherDao.getPublishersList()
    val allCreators: Flow<List<Creator>> = creatorDao.getCreatorsList()
    val allRoles: Flow<List<Role>> = roleDao.getRoleList()

    fun getSeries(seriesId: Int): Flow<Series?> = seriesDao.getSeries(seriesId)

    fun getSeriesByFilterPagingSource(filter: Filter): PagingSource<Int, FullSeries> {
        val mSeries = filter.mSeries
        if (mSeries == null) {
            refreshFilterOptions(mSeries, filter.mCreators)
            return seriesDao.getSeriesByFilter(filter)
        } else {
            Log.d(TAG, "getSeriesByFilterPagingSource: Wasn't expecting to see a series here")
//            throw java.lang.IllegalArgumentException("getSeriesByFilterPagingSource: Filter seriesId should be null $filter")
            return seriesDao.getSeriesByFilter(filter)
        }
    }

    fun getSeriesByFilterLiveData(filter: Filter): Flow<List<Series>> {
        val mSeries = filter.mSeries
        if (mSeries == null) {
            refreshFilterOptions(mSeries, filter.mCreators)
            return seriesDao.getSeriesByFilterFlow(filter)
        } else {
            throw java.lang.IllegalArgumentException("getSeriesByFilterLiveData: Filter seriesId should be null $filter")
        }
    }

    fun getIssue(issueId: Int): Flow<FullIssue?> {
        UpdateIssueCredit(apiService, database, prefs).update(issueId)
        UpdateIssueCover(database, context).update(issueId)
        return issueDao.getFullIssue(issueId = issueId)
    }

    fun getIssuesByFilter(filter: Filter): Flow<List<FullIssue>> {
        val mSeries = filter.mSeries
        if (mSeries != null) {
            refreshFilterOptions(series = mSeries, creators = filter.mCreators)
        }
        return issueDao.getIssuesByFilter(filter = filter)
    }

    fun getIssuesByFilterPagingSource(filter: Filter): PagingSource<Int, FullIssue> {
        val mSeries = filter.mSeries
        if (mSeries != null) {
            refreshFilterOptions(mSeries, filter.mCreators)
        }
        return issueDao.getIssuesByFilterPagingSource(filter = filter)
    }

    private fun refreshFilterOptions(
        series: Series?,
        creators: Set<Creator>
    ) {
        val creatorIds = creators.map { it.creatorId }

        if (creatorIds.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                UpdateCreator(apiService, database, prefs).updateAll(creatorIds)
            }
        }

        series?.let { UpdateSeries(apiService, database, prefs).update(it.seriesId) }
    }

    fun getVariants(issueId: Int): Flow<List<Issue>> = issueDao.getVariants(issueId)

    fun getStoriesByIssue(issueId: Int): Flow<List<Story>> = storyDao.getStories(issueId)

    fun getCreditsByIssue(issueId: Int): Flow<List<FullCredit>> = combine(
        creditDao.getIssueCredits(issueId),
        exCreditDao.getIssueExtractedCredits(issueId)
    ) { credits1: List<FullCredit>?, credits2: List<FullCredit>? ->
        val res = (credits1 ?: emptyList()) + (credits2 ?: emptyList())
        sort(res)
        res
    }

    fun getValidFilterOptions(filter: Filter): Flow<List<FilterOption>> {
        val seriesList: Flow<List<FilterOption>> =
            when {
                filter.isEmpty()       -> allSeries
                filter.mSeries == null -> seriesDao.getSeriesByFilterFlow(filter)
                else                   -> flow { emit(emptyList<FilterOption>()) }
            }

        val creatorsList: Flow<List<FilterOption>> =
            when {
                filter.isEmpty()           -> allCreators
                filter.mCreators.isEmpty() -> creatorDao.getCreatorsByFilter(filter)
                else                       -> flow { emit(emptyList<FilterOption>()) }
            }

        val publishersList: Flow<List<FilterOption>> =
            when {
                filter.isEmpty()             -> allPublishers
                filter.mPublishers.isEmpty() -> publisherDao.getPublishersByFilter(filter)
                else                         -> flow { emit(emptyList<FilterOption>()) }
            }

        return combine(
            seriesList,
            creatorsList,
            publishersList
        ) { series: List<FilterOption>, creators: List<FilterOption>, publishers: List<FilterOption> ->
            val res = series + creators + publishers
            sort(res)
            res
        }
    }

    fun getPublisher(publisherId: Int): Flow<Publisher?> =
        publisherDao.getPublisher(publisherId)

/*
    FUTURE IMPLEMENTATION
    fun getCoverImage(issue: Issue): File = File(filesDir, issue.coverFileName)
*/

    /**
     * Builds database and adds dummy publisher and series, which are used for creating new empty
     * issue objects
     */
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
    ).addMigrations()
        .build()

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

        // TODO: This should probably get moved out of SharedPreferences and stored with each record.
        //  The tradeoff: an extra local db query vs. having a larger prefs which will end up having
        //  a value for every item in the database.
        fun checkIfStale(
            prefsKey: String,
            shelfLife: Long,
            prefs: SharedPreferences
        ): Boolean {
            val lastUpdated = LocalDate.parse(prefs.getString(prefsKey, "${LocalDate.MIN}"))
            return DEBUG || lastUpdated.plusDays(shelfLife) < LocalDate.now()
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

    fun updateIssue(issue: FullIssue?) {
        if (issue != null) {
            UpdateIssueCover(database, context).update(issue.issue.issueId)
            UpdateIssueCredit(apiService, database, prefs).update(issue.issue.issueId)
        }
    }
}

class AllCreditsLiveData(
    credits: LiveData<List<FullCredit>>?,
    extractedCredits: LiveData<List<FullCredit>>?,
    private val combine: (List<FullCredit>?, List<FullCredit>?) -> List<FullCredit>
) : MediatorLiveData<List<FullCredit>>() {

    private var mCredits: List<FullCredit>? = null
    private var eCredits: List<FullCredit>? = null

    init {
        credits?.let { creditsLiveData ->
            super.addSource(creditsLiveData) {
                mCredits = it
                this.value = combine(mCredits, eCredits)
            }

        }
        extractedCredits?.let { creditsLiveData ->
            super.addSource(creditsLiveData) {
                eCredits = it
                this.value = combine(mCredits, eCredits)
            }
        }
    }

    override fun <S : Any?> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
        throw UnsupportedOperationException()
    }

    override fun <S : Any?> removeSource(toRemote: LiveData<S>) {
        throw UnsupportedOperationException()
    }
}

class AllFiltersLiveData(
    series: LiveData<out List<Series>>?,
    creators: LiveData<List<Creator>>?,
    publishers: LiveData<List<Publisher>>?,
    private val combine: (
        List<Series>?,
        List<Creator>?,
        List<Publisher>?
    ) -> List<FilterOption>
) : MediatorLiveData<List<FilterOption>>() {

    private var mSeries: List<Series>? = null
    private var mCreators: List<Creator>? = null
    private var mPublishers: List<Publisher>? = null

    init {
        series?.let { liveSeries ->
            super.addSource(liveSeries) {
                mSeries = it
                this.value = combine(mSeries, mCreators, mPublishers)
            }
        }
        creators?.let { liveCreators ->
            super.addSource(liveCreators) {
                mCreators = it
                value = combine(mSeries, mCreators, mPublishers)
            }
        }
        publishers?.let { livePublishers ->
            super.addSource(livePublishers) {
                mPublishers = it
                value = combine(mSeries, mCreators, mPublishers)
            }
        }
    }

    override fun <S : Any?> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
        throw UnsupportedOperationException()
    }

    override fun <S : Any?> removeSource(toRemote: LiveData<S>) {
        throw UnsupportedOperationException()
    }
}