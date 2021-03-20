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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.util.concurrent.Executors

const val DUMMY_ID = Int.MAX_VALUE

private const val DATABASE_NAME = "issue-database"
private const val TAG = "IssueRepository"
private const val STALE_DATA_NUM_DAYS: Long = 0
private const val STALE_SERIES_NUM_DAYS: Long = 1

private const val SHARED_PREFS = "CCPrefs"
private const val STATIC_DATA_UPDATED = "static_data_updated"
private const val PUBLISHERS_UPDATED = "publisher_list_updated"
private const val ROLES_UPDATED = "role_list_updated"
private const val STORY_TYPES_UPDATED = "story_type_list_updated"
private const val SERIES_LIST_UPDATED = "series_list_updated"


const val BASE_URL = "http://192.168.0.141:8000/"

class IssueRepository private constructor(context: Context) {

    private var prefs: SharedPreferences

    private val executor = Executors.newSingleThreadExecutor()
    private val database: IssueDatabase = buildDatabase(context)
    private val seriesDao = database.seriesDao()
    private val issueDao = database.issueDao()
    private val creatorDao = database.creatorDao()
    private val publisherDao = database.publisherDao()
    private val roleDao = database.roleDao()
    private val storyDao = database.storyDao()
    private val creditDao = database.creditDao()
    private val storyTypeDao = database.storyTypeDao()

    private val filesDir = context.applicationContext.filesDir
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val apiService: Webservice by lazy {
        retrofit.create(Webservice::class.java)
    }

    var allSeries: LiveData<List<Series>> = seriesDao.getAllSeries()

    val allPublishers: LiveData<List<Publisher>> = publisherDao.getPublishersList()

    val allCreators: LiveData<List<Creator>> = creatorDao.getCreatorsList()

    val allRoles: LiveData<List<Role>> = roleDao.getRoleList()

    init {
        prefs = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        val last_update = LocalDate.parse(
            prefs.getString(STATIC_DATA_UPDATED, LocalDate.MIN.toString())
        )

        if (last_update.plusDays(STALE_DATA_NUM_DAYS) < LocalDate.now()) {
            refreshStaticData(prefs)
        }
    }

    /**
     *  Updates publisher, series, role, and storytype tables if it has been
     *  more than [STALE_DATA_NUM_DAYS] days since last update
     *
     *  @param context
     */
    private fun refreshStaticData(prefs: SharedPreferences) {

        refreshPublishers(prefs)
        refreshRoles(prefs)
        refreshStoryTypes(prefs)
        saveTime(prefs, STATIC_DATA_UPDATED)
    }

    private fun refreshPublishers(prefs: SharedPreferences) {
        val publisherCall = apiService.getPublishers()

        publisherCall.enqueue(
            StandardCall(
                callName = "pubCall",
                commit_call = {
                    executor.execute {
                        publisherDao.upsert(it)
                    }
                },
                prefs_key = PUBLISHERS_UPDATED,
                prefs = prefs
            )
        )
    }

    private fun refreshRoles(prefs: SharedPreferences) {
        val roleCall = apiService.getRoles()

        roleCall.enqueue(
            StandardCall(
                callName = "roleCall",
                commit_call = {
                    saveRole(*it.toTypedArray())
                },
                prefs_key = ROLES_UPDATED,
                prefs = prefs
            )
        )
    }

    private fun refreshStoryTypes(prefs: SharedPreferences) {
        val storyTypeCall = apiService.getStoryTypes()

        val commit = { it: List<StoryType> ->
            executor.execute {
                storyTypeDao.upsert(it)

                val last_series_update = LocalDate.parse(
                    prefs.getString(SERIES_LIST_UPDATED, LocalDate.MIN.toString())
                )

                if (last_series_update.plusDays(STALE_SERIES_NUM_DAYS) < LocalDate.now()) {
                    refreshSeries(prefs)
                }
            }
        }

        storyTypeCall.enqueue(
            StandardCall(
                callName = "storyTypeCall",
                commit_call = commit,
                prefs_key = STORY_TYPES_UPDATED,
                prefs = prefs
            )
        )

    }

    /**
     * Recursive call that updates the series list by page
     *
     * @param prefs SharedPreferences to save the time of update
     * @param page the number of the page to request from the server
     */
    private fun refreshSeries(prefs: SharedPreferences, page: Int = 0) {
        val seriesCall = apiService.getSeries(page)

        seriesCall.enqueue(
            object : Callback<List<Item<GcdSeries, Series>>> {
                override fun onResponse(
                    call: Call<List<Item<GcdSeries, Series>>>,
                    response: Response<List<Item<GcdSeries, Series>>>
                ) {
                    if (response.code() == 200) {
                        val seriesList: List<Item<GcdSeries, Series>>? =
                            response.body()

                        Log.d(TAG, "seriesList: $seriesList")

                        seriesList?.let {
                            if (seriesList.size > 0) {
                                refreshSeries(prefs, page + 1)

                                seriesList.map {
                                    Log.d(TAG, it.pk.toString())
                                    it.toRoomModel()
                                }.let {
                                    saveSeries(*it.toTypedArray())

                                    saveTime(prefs, SERIES_LIST_UPDATED)
                                }
                            }
                        }
                    }
                }

                override fun onFailure(
                    call: Call<List<Item<GcdSeries, Series>>>,
                    t: Throwable
                ) {
                    Log.d(TAG, "seriesCall onFailure ${call.request()} $t")
                    val res = null
                }
            }
        )
    }

    /**
     * Gets all stories from issue with pk issueId and also triggers an update from the server
     *
     * @param issueId the pk of the issue whose stories are being requested
     */
    fun getStories(issueId: Int): LiveData<List<Story>> {
        refreshStories(issueId)
        return storyDao.getStories(issueId)
    }

    /**
     * Enqueues a retrofit call to update the story data from server
     */
    private fun refreshStories(issueId: Int) {
        val storiesCall = apiService.getStories(issueId)

        storiesCall.enqueue(
            object : Callback<List<Item<GcdStory, Story>>> {
                override fun onResponse(
                    call: Call<List<Item<GcdStory, Story>>>,
                    response: Response<List<Item<GcdStory, Story>>>
                ) {
                    if (response.code() == 200) {
                        val stories = response.body()

                        stories?.let {
                            refreshCredits(issueId, it.map { item -> item.toRoomModel() })
                            extractCredits(stories)
                        }
                    }
                }

                override fun onFailure(call: Call<List<Item<GcdStory, Story>>>, t: Throwable) {
                    Log.d(TAG, "storiesCall failure: ${call.request()} $t")
                }
            }
        )
    }

    private fun extractCredits(stories: List<Item<GcdStory, Story>>?) {

        stories?.forEach { gcdStory ->
            executor.execute {
                storyDao.upsert(gcdStory.toRoomModel())
            }

            val story = gcdStory.fields

            if (story.script != "") {
                story.script.split("; ").map { name ->
                    var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                    res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                    Log.d(TAG, "SCRIPT: $res")
                    makeCredit(res, gcdStory.pk, Role.Companion.Name.SCRIPT.value)
                }
            }

            if (story.pencils != "") {
                story.pencils.split("; ").map { name ->
                    var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                    res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                    Log.d(TAG, "PENCILS: $res")
                    makeCredit(res, gcdStory.pk, Role.Companion.Name.PENCILS.value)
                }
            }

            if (story.inks != "") {
                story.inks.split("; ").map { name ->
                    var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                    res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                    Log.d(TAG, "INKS: $res")
                    makeCredit(res, gcdStory.pk, Role.Companion.Name.INKS.value)
                }
            }

            if (story.colors != "") {
                story.colors.split("; ").map { name ->
                    var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                    res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                    Log.d(TAG, "COLORS: $res")
                    makeCredit(res, gcdStory.pk, Role.Companion.Name.COLORS.value)
                }
            }
            if (story.letters != "") {
                story.letters.split("; ").map { name ->
                    var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                    res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                    Log.d(TAG, "LETTERS: $res")
                    makeCredit(res, gcdStory.pk, Role.Companion.Name.LETTERS.value)
                }
            }

            if (story.editing != "") {
                story.editing.split("; ").map { name ->
                    var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                    res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                    Log.d(TAG, "EDITS: $res")
                    makeCredit(res, gcdStory.pk, Role.Companion.Name.EDITING.value)
                }
            }
        }
    }

    private fun makeCredit(extracted_name: String, storyId: Int, roleId: Int) {
        val creatorCall = apiService.getCreatorByName(extracted_name)
        Log.d(TAG, "Extracted Name: $extracted_name")
        creatorCall.enqueue(
            object : Callback<List<Item<GcdCreator, Creator>>> {
                override fun onResponse(
                    call: Call<List<Item<GcdCreator, Creator>>>,
                    response: Response<List<Item<GcdCreator, Creator>>>
                ) {
                    if (response.code() == 200) {
                        val creator = response.body()
                        // TODO: Need to handle multiple options (i.e. size > 1)
                        creator?.let {
                            if (it.size > 0) {
                                it[0].toRoomModel().let {
                                    executor.execute {
                                        database.insertCreatorAndCredit(
                                            it,
                                            Credit(
                                                storyId = storyId,
                                                creatorId = it.creatorId,
                                                roleId = roleId
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<List<Item<GcdCreator, Creator>>>, t: Throwable) {
                    Log.d(TAG, "creatorCall failure ${call.request()} $t")
                }
            }
        )
    }

    private fun refreshCredits(issueId: Int, stories: List<Story>) {
        val creditsCall = apiService.getCredits(issueId)

        creditsCall.enqueue(
            object : Callback<List<Item<GcdCredit, Credit>>> {
                override fun onResponse(
                    call: Call<List<Item<GcdCredit, Credit>>>,
                    response: Response<List<Item<GcdCredit, Credit>>>
                ) {
                    if (response.code() == 200) {
                        val credits: List<Item<GcdCredit, Credit>>? = response.body()

                        credits?.let {
                            val creditModels = it.map {
                                it.toRoomModel()
                            }

                            val creatorModels = it.map {
                                it.fields.getCreatorModel()
                            }

                            executor.execute {
                                database.insertCredits(
                                    stories,
                                    creatorModels,
                                    creditModels
                                )
                            }

                        }
                    }
                }

                override fun onFailure(
                    call: Call<List<Item<GcdCredit, Credit>>>,
                    t: Throwable
                ) {
                    Log.d(TAG, "creditsCall failure: ${call.request()} $t")
                }
            }
        )
    }

    fun getIssuesBySeries(seriesId: Int): LiveData<List<FullIssue>> {
        updateIssuesBySeries(seriesId)
        return issueDao.getIssuesBySeries(seriesId)
    }

    fun updateIssuesBySeries(seriesId: Int) {
        val issuesCall = apiService.getIssuesBySeries(seriesId)

        issuesCall.enqueue(
            StandardCall(
                callName = "issuesCall",
                commit_call = {
                    executor.execute {
                        issueDao.upsert(it)
                    }
                },
                prefs_key = "${seriesId}_updated",
                prefs = prefs
            )
        )
    }

    /**
     * Builds database and adds dummy publisher and series, which are used for creating new empty
     * issue objects
     */
    private fun buildDatabase(context: Context) = Room.databaseBuilder(
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

    fun saveIssue(vararg issue: Issue) {
        executor.execute {
            try {
                issueDao.upsert(issue.asList())
            } catch (e: SQLiteConstraintException) {
                Log.d(TAG, "addIssue: $e")
            }
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

    fun deleteIssue(issue: Issue) {
        executor.execute {
            issueDao.delete(issue)
        }
    }

    fun deleteSeries(series: Series) {
        executor.execute {
            seriesDao.delete(series)
        }
    }

    fun getSeries(seriesId: Int): LiveData<Series?> = seriesDao.getSeriesById(seriesId)

    class DuplicateFrament : DialogFragment() {
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

    fun updateCredit(credit: Credit) {
        executor.execute {
            creditDao.update(credit)
        }
    }

    fun deleteCredit(credit: Credit) {
        executor.execute {
            creditDao.delete(credit)
        }
    }

    fun getPublisher(publisherId: Int): LiveData<Publisher?> =
        publisherDao.getPublisher(publisherId)

    fun getFullIssue(issueId: Int): LiveData<IssueAndSeries?> = issueDao.getFullIssue(issueId)

    fun getIssueCredits(issueId: Int): LiveData<List<FullCredit>> {
        refreshStories(issueId)
        return creditDao.getIssueCredits(issueId)
    }


    fun getSeriesByCreator(creatorId: Int): LiveData<List<Series>> =
        seriesDao.getSeriesList(creatorId)

    fun getCreatorBySeries(seriesId: Int): LiveData<List<Creator>> =
        creatorDao.getCreatorList(seriesId)

/*
    FUTURE IMPLEMENTATION
    fun getCoverImage(issue: Issue): File = File(filesDir, issue.coverFileName)
*/

    class StandardCall<G : GcdJson<D>, D : DataModel>(
        val callName: String,
        val commit_call: (List<D>) -> Unit,
        val prefs_key: String,
        val prefs: SharedPreferences
    ) : Callback<List<Item<G, D>>> {
        override fun onResponse(
            call: Call<List<Item<G, D>>>,
            response: Response<List<Item<G, D>>>
        ) {
            if (response.code() == 200) {
                Log.d(TAG, "$callName ${call.request()} ${response}")
                val itemList: List<Item<G, D>>? = response.body()

                itemList?.map {
                    it.toRoomModel()
                }?.let {
                    commit_call(it)

                    saveTime(prefs, prefs_key)
                }
            }
        }

        override fun onFailure(
            call: Call<List<Item<G, D>>>,
            t: Throwable
        ) {
            Log.d(TAG, "$callName onFailure ${call.request()} $t")
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

