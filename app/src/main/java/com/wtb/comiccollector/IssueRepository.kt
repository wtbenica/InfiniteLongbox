package com.wtb.comiccollector

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
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.comiccollector.database.IssueDatabase
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val DUMMY_ID = Int.MAX_VALUE

private const val DATABASE_NAME = "issue-database"
private const val TAG = APP + "IssueRepository"
private const val GET_STORY_SIZE = 200


private const val SHARED_PREFS = "CCPrefs"

private const val STATIC_DATA_UPDATED = "static_data_updated"
private const val SERIES_LIST_UPDATED = "series_list_updated"

private const val STATIC_DATA_LIFETIME: Long = 30
private const val SERIES_LIST_LIFETIME: Long = 7
private const val ISSUE_LIFETIME: Long = 30
private const val CREATOR_LIFETIME: Long = 7


const val EXTERNAL = "http://24.176.172.169/"
const val NIGHTWING = "http://192.168.0.141:8000/"
const val ALFRED = "http://192.168.0.138:8000/"
const val BASE_URL = ALFRED

class IssueRepository private constructor(context: Context) {

    internal val prefs: SharedPreferences =
        context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)

    private val executor = Executors.newSingleThreadExecutor()
    internal val database: IssueDatabase = buildDatabase(context)
    private val seriesDao = database.seriesDao()
    private val issueDao = database.issueDao()
    private val creatorDao = database.creatorDao()
    private val publisherDao = database.publisherDao()
    private val roleDao = database.roleDao()
    private val storyDao = database.storyDao()
    private val creditDao = database.creditDao()
    private val storyTypeDao = database.storyTypeDao()
    private val nameDetailDao = database.nameDetailDao()

    private val filesDir = context.applicationContext.filesDir
    private val retrofit: Retrofit by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
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

    var allSeries: LiveData<List<Series>> = seriesDao.getAllSeries()

    val allPublishers: LiveData<List<Publisher>> = publisherDao.getPublishersList()

    val allCreators: LiveData<List<Creator>> = creatorDao.getCreatorsList()

    val allRoles: LiveData<List<Role>> = roleDao.getRoleList()

    init {
        StaticUpdater().update()
    }

    fun getSeries(seriesId: Int) = seriesDao.getSeriesById(seriesId)

    fun getPublisher(publisherId: Int) = publisherDao.getPublisher(publisherId)

    fun getFullIssue(issueId: Int): LiveData<IssueAndSeries?> = issueDao.getFullIssue(issueId)

    fun getStoriesByIssue(issueId: Int): LiveData<List<Story>> {
        CreditUpdater().update(issueId)
        return storyDao.getStories(issueId)
    }

    fun getIssueCredits(issueId: Int): LiveData<List<FullCredit>> =
        creditDao.getIssueCredits(issueId)


    fun getSeriesByCreator(creatorId: Int): LiveData<List<Series>> {
        CreatorUpdater().update(creatorId)
        return seriesDao.getSeriesList(creatorId)
    }

    fun getCreatorBySeries(seriesId: Int): LiveData<List<Creator>> =
        creatorDao.getCreatorList(seriesId)

    fun getIssuesBySeries(seriesId: Int): LiveData<List<FullIssue>> {
        IssueUpdater().update(seriesId)
        return issueDao.getIssuesBySeries(seriesId)
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
                            endDate = LocalDate.MIN
                        )
                    )
                }
            }
        }
    ).build()

    inner class StaticUpdater {
        /**
         *  Updates publisher, series, role, and storytype tables
         */
        internal fun update() {
            if (checkIfStale(STATIC_DATA_UPDATED, STATIC_DATA_LIFETIME)) {
                val publishers = GlobalScope.async {
                    apiService.getPublishers()
                }

                val roles = GlobalScope.async {
                    apiService.getRoles()
                }

                val storyTypes = GlobalScope.async {
                    apiService.getStoryTypes()
                }

                GlobalScope.launch {
                    GlobalScope.async {
                        database.transactionDao().upsertStatic(
                            publishers = publishers.await().map { it.toRoomModel() },
                            roles = roles.await().map { it.toRoomModel() },
                            storyTypes = storyTypes.await().map { it.toRoomModel() }
                        )
                    }.await().let {
                        updateSeries()
                    }
                }
            } else {
                if (checkIfStale(SERIES_LIST_UPDATED, SERIES_LIST_LIFETIME)) {
                    GlobalScope.launch {
                        updateSeries()
                    }
                }
            }
        }

        private suspend fun updateSeries() {
            var page = 0
            var stop = false
            do {
                GlobalScope.async {
                    apiService.getSeries(page)
                }.await().let { seriesItems ->
                    if (seriesItems.isEmpty()) {
                        stop = true
                    } else {
                        seriesDao.upsertSus(seriesItems.map { it.toRoomModel() })
                    }
                }
                page += 1
            } while (!stop)
            saveTime(prefs, STATIC_DATA_UPDATED)
        }
    }

    inner class CreditUpdater {
        internal fun update(issueId: Int) {
            if (checkIfStale("${issueId}_updated", ISSUE_LIFETIME)) {
                val storyItems = GlobalScope.async {
                    apiService.getStoriesByIssue(issueId)
                }

                val creditItems = GlobalScope.async {
                    storyItems.await().let {
                        if (it.isNotEmpty()) {
                            apiService.getCreditsByStories(it.map { item -> item.pk })
                        } else {
                            null
                        }
                    }
                }

                val nameDetails: Deferred<List<Item<GcdNameDetail, NameDetail>>?> =
                    GlobalScope.async {
                        creditItems.await()?.let {
                            if (it.isNotEmpty()) {
                                apiService.getNameDetailsByIds(it.map { it.fields.nameDetailId })
                            } else {
                                null
                            }
                        }
                    }

                val creators: Deferred<List<Item<GcdCreator, Creator>>?> = GlobalScope.async {
                    nameDetails.await()?.let {
                        if (it.isNotEmpty()) {
                            apiService.getCreator(it.map { it.fields.creatorId })
                        } else {
                            null
                        }
                    }
                }

                GlobalScope.launch {
                    GlobalScope.async {
                        val stories1 = storyItems.await().map { it.toRoomModel() }
                        val credits1 = creditItems.await()?.map { it.toRoomModel() }
                        val nameDetails1 = nameDetails.await()?.map { it.toRoomModel() }
                        val creators1 = creators.await()?.map { it.toRoomModel() }
                        Log.d(TAG, "INSERTING $stories1\n$credits1\n$nameDetails1\n$creators1")
                        database.transactionDao().upsertSus(
                            stories = stories1,
                            credits = credits1,
                            nameDetails = nameDetails1,
                            creators = creators1
                        )
                    }.await().let {
                        val stories1 = storyItems.await()
                        Log.d(TAG, "EXtrACtiNG: $stories1")
                        CreditExtractor().extractCredits(stories1)
                    }
                }
            }
        }
    }

    inner class CreatorUpdater {

        internal fun update(creatorId: Int) {
            val lastUpdate =
                LocalDate.parse(prefs.getString("${creatorId}_UPDATED", "${LocalDate.MIN}"))
            if (checkIfStale("${creatorId}_updated", CREATOR_LIFETIME)) {
                refreshNewStyleCredits(creatorId)
                refreshOldStyleCredits(creatorId)
            }
        }

        private fun refreshOldStyleCredits(creatorId: Int) {
            val creator: Deferred<Creator?> = GlobalScope.async {
                Log.d(TAG, "old getCreatorSus $creatorId")
                creatorDao.getCreatorSus(creatorId)
            }

            val stories: Deferred<List<Item<GcdStory, Story>>?> = GlobalScope.async {
                creator.await()?.name?.let {
                    Log.d(TAG, "old getStories $it")
                    apiService.getStoriesByName(it)
                }
            }

            val issuesDef: Deferred<List<Item<GcdIssue, Issue>>?> = GlobalScope.async {
                stories.await()?.let {
                    val issueIds = it.map { item -> item.fields.issueId }
                    Log.d(TAG, "ISSUE_IDS 1: $issueIds")
                    apiService.getIssues(issueIds)
                }
            }

            val variants: Deferred<List<Item<GcdIssue, Issue>>?> = GlobalScope.async {
                issuesDef.await()?.let { issueItems ->
                    val issues: List<Issue> = issueItems.map { it.toRoomModel() }
                    val ids: List<Int> = issues.mapNotNull { it.variantOf }

                    if (ids.isNotEmpty()) {
                        Log.d(TAG, "ISSUE_IDS 2: $ids")
                        apiService.getIssues(ids)
                    } else {
                        null
                    }
                }
            }

            val issuesInserted = GlobalScope.async {
                issueDao.upsertSus((variants.await()?.map { it.toRoomModel() } ?: emptyList()) +
                        (issuesDef.await()?.map { it.toRoomModel() } ?: emptyList()))
            }

            GlobalScope.launch {
                issuesInserted.await().let {
                    Log.d(TAG, "inserted issues")
                }
            }

            val storiesInserted = GlobalScope.async {
                issuesInserted.await().let { unit ->
                    stories.await()?.let { storyItems ->
                        Log.d(TAG, "insert stories ${storyItems.size}")
                        storyDao.upsertSus(storyItems.map { it.toRoomModel() })
                    }
                }
            }

            GlobalScope.launch {
                GlobalScope.async {
                    storiesInserted.await().let {
                        Log.d(TAG, "extractCredits ${stories.await()?.size ?: 0}")
                        CreditExtractor().extractCredits(stories.await())
                    }
                }.await().let {
                    saveTime(prefs, "${creatorId}_UPDATED")
                }
            }
        }

        private fun refreshNewStyleCredits(creatorId: Int) {
            val nameDetail = GlobalScope.async {
                nameDetailDao.getNameDetailByCreatorIdSus(creatorId)
            }

            val credits = GlobalScope.async {
                nameDetail.await()?.let {
                    apiService.getCreditsByNameDetail(listOf(it.nameDetailId))
                }
            }

            val stories = GlobalScope.async {
                credits.await()?.let {
                    apiService.getStories(it.map { item -> item.toRoomModel().storyId })
                }
            }

            val issues: Deferred<List<Item<GcdIssue, Issue>>?> = GlobalScope.async {
                stories.await()?.let {
                    val issueIds = it.map { item -> item.toRoomModel().issueId }
                    Log.d(TAG, "ISSUE_IDS 3: $issueIds")
                    apiService.getIssues(issueIds)
                }
            }

            val variants: Deferred<List<Item<GcdIssue, Issue>>?> = GlobalScope.async {
                issues.await()?.let {
                    val issueIds = it.mapNotNull { item -> item.toRoomModel().variantOf }
                    if (issueIds.isNotEmpty()) {
                        Log.d(TAG, "ISSUE_IDS 4: $issueIds")
                        apiService.getIssues(issueIds)
                    } else {
                        null
                    }
                }
            }

            GlobalScope.launch {
                database.transactionDao().upsert(
                    stories = stories.await()?.map { it.toRoomModel() },
                    issues = (variants.await()?.map { it.toRoomModel() } ?: emptyList()) +
                            (issues.await()?.map { it.toRoomModel() } ?: emptyList()),
                    credits = credits.await()?.map { it.toRoomModel() })
            }

        }
    }

    inner class IssueUpdater {
        internal fun update(seriesId: Int) {
            if (checkIfStale("${seriesId}_updated", ISSUE_LIFETIME))
            GlobalScope.launch {
                GlobalScope.async {
                    apiService.getIssuesBySeries(seriesId)
                }.await().let { issueItems ->
                    issueDao.upsertSus(issueItems.map { it.toRoomModel() })
                }
            }
        }
    }

    inner class CreditExtractor {
        suspend fun extractCredits(stories: List<Item<GcdStory, Story>>?) {
            stories?.forEach { gcdStory ->
                val story = gcdStory.fields
                Log.d(TAG, "Extracting ${story.title}")
                if (story.script != "") {
                    story.script.split("; ").map { name ->
                        var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                        res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                        Log.d(TAG, "Script: $res")
                        makeCredit(res, gcdStory.pk, Role.Companion.Name.SCRIPT.value)
                    }
                }

                if (story.pencils != "") {
                    story.pencils.split("; ").map { name ->
                        var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                        res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                        Log.d(TAG, "Pencils: $res")
                        makeCredit(res, gcdStory.pk, Role.Companion.Name.PENCILS.value)
                    }
                }

                if (story.inks != "") {
                    story.inks.split("; ").map { name ->
                        var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                        res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                        makeCredit(res, gcdStory.pk, Role.Companion.Name.INKS.value)
                    }
                }

                if (story.colors != "") {
                    story.colors.split("; ").map { name ->
                        var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                        res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                        makeCredit(res, gcdStory.pk, Role.Companion.Name.COLORS.value)
                    }
                }

                if (story.letters != "") {
                    story.letters.split("; ").map { name ->
                        var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                        res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                        makeCredit(res, gcdStory.pk, Role.Companion.Name.LETTERS.value)
                    }
                }

                if (story.editing != "") {
                    story.editing.split("; ").map { name ->
                        var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                        res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                        makeCredit(res, gcdStory.pk, Role.Companion.Name.EDITING.value)
                    }
                }
            }
        }


        private suspend fun makeCredit(extracted_name: String, storyId: Int, roleId: Int) {
            val localCreatorDef = GlobalScope.async {
                listOf(creatorDao.getCreatorByNameSus(extracted_name))
            }

            localCreatorDef.await().let { localCreators ->
                if (localCreators.isEmpty() || localCreators[0] == null) {
                    val nameDetailDef: Deferred<List<Item<GcdNameDetail, NameDetail>>> =
                        GlobalScope.async {
                            apiService.getNameDetailByName(extracted_name)
                        }

                    nameDetailDef.await().let { nameDetails ->
                        if (nameDetails.isNotEmpty()) {
                            val creatorIds = nameDetails.mapNotNull { ndItem ->
                                val creatorId = ndItem.toRoomModel().creatorId
                                val lastUpdated = LocalDate.parse(
                                    prefs.getString("${creatorId}_UPDATED", "${LocalDate.MIN}")
                                )
                                if (lastUpdated.plusDays(14) < LocalDate.now()) {
                                    creatorId
                                } else {
                                    null
                                }
                            }

                            val remoteCreatorDef = GlobalScope.async {
                                if (creatorIds.isNotEmpty()) {
                                    apiService.getCreator(creatorIds)
                                } else {
                                    emptyList()
                                }.map { it.toRoomModel() }
                            }

                            remoteCreatorDef.await().let { creators ->
                                val creatorInserted: Deferred<Unit> = GlobalScope.async {
                                    creatorDao.upsertSus(creators.map { it })
                                }

                                val nameDetailInserted: Deferred<Unit>
                                creatorInserted.await().let {
                                    nameDetailInserted = GlobalScope.async {
                                        nameDetailDao.upsertSus(
                                            nameDetails.map { it.toRoomModel() })
                                    }
                                }

                                GlobalScope.launch {
                                    nameDetailInserted.await().let {
                                        Log.d(TAG, "Saving ${storyId} ${creators[0].name}")
                                        creditDao.upsertSus(
                                            listOf(
                                                Credit(
                                                    storyId = storyId,
                                                    nameDetailId = nameDetails[0].pk,
                                                    roleId = roleId
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    GlobalScope.launch {
                        val nameDetail = GlobalScope.async {
                            localCreators[0]?.creatorId?.let { id ->
                                nameDetailDao.getNameDetailByCreatorIdSus(id)
                            }
                        }

                        nameDetail.await().let {
                            it?.let {
                                creditDao.upsertSus(
                                    listOf(
                                        Credit(
                                            storyId = storyId,
                                            nameDetailId = it.nameDetailId,
                                            roleId = roleId
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkIfStale(prefsKey: String, shelfLife: Long) =
        LocalDate.parse(prefs.getString(prefsKey, "${LocalDate.MIN}"))
            .plusDays(shelfLife) < LocalDate.now()

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
        private var INSTANCE: IssueRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = IssueRepository(context)
            }
        }

        fun get(): IssueRepository {
            return INSTANCE
                ?: throw IllegalStateException("IssueRepository must be initialized")
        }

        fun saveTime(prefs: SharedPreferences, key: String) {
            val editor = prefs.edit()
            editor.putString(key, LocalDate.now().toString())
            editor.apply()
        }
    }
}
