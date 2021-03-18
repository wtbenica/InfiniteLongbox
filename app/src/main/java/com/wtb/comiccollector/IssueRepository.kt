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
private const val STALE_DATA_NUM_DAYS: Long = 14

const val BASE_URL = "http://192.168.0.141:8000/"

class IssueRepository private constructor(context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val database: IssueDatabase = buildDatabase(context)
    private val issueDao = database.issueDao()
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

    var allSeries: LiveData<List<Series>> = issueDao.getAllSeries()

    val allPublishers: LiveData<List<Publisher>> = issueDao.getPublishersList()

    val allCreators: LiveData<List<Creator>> = issueDao.getCreatorsList()

    val allWriters: LiveData<List<Creator>> = issueDao.getWritersList()

    val allRoles: LiveData<List<Role>> = issueDao.getRoleList()

    init {
        refreshStaticData(context)
    }

    /**
     *  Updates publisher, series, role, and storytype tables if it has been
     *  more than [STALE_DATA_NUM_DAYS] days since last update
     *
     *  @param context
     */
    private fun refreshStaticData(context: Context) {
        val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val series_last_updated = prefs.getString("series_list_updated", LocalDate.MIN.toString())

        if (LocalDate.parse(series_last_updated).plusDays(STALE_DATA_NUM_DAYS) < LocalDate.now()) {
            Log.d(TAG, "init: updating series list from db")

            refreshPublishers(prefs)

            refreshRoles(prefs)

            refreshStoryTypes(prefs)
        }

    }

    private fun refreshPublishers(prefs: SharedPreferences) {
        val publisherCall: Call<List<Item<GcdPublisherJson, Publisher>>> =
            apiService.getPublishers()

        publisherCall.enqueue(
            object : Callback<List<Item<GcdPublisherJson, Publisher>>> {
                override fun onResponse(
                    call: Call<List<Item<GcdPublisherJson, Publisher>>>,
                    response: Response<List<Item<GcdPublisherJson, Publisher>>>
                ) {
                    Log.d(TAG, "pubCall onResponse ${call.request()} ${response}")
                    if (response.code() == 200) {
                        val publisherList: List<Item<GcdPublisherJson, Publisher>>? =
                            response.body()

                        Log.d(TAG, "publisherList: $publisherList")

                        publisherList?.map {
                            it.toRoomModel()
                        }?.let {
                            executor.execute {
                                issueDao.insertPublisher(*it.toTypedArray())
                            }

                            val editor = prefs.edit()
                            editor.putString(
                                "publisher_list_updated", LocalDate.now()
                                    .toString()
                            )
                            editor.apply()
                        }

                        refreshSeries(prefs, 0)
                    }
                }

                override fun onFailure(
                    call: Call<List<Item<GcdPublisherJson, Publisher>>>,
                    t: Throwable
                ) {
                    Log.d(TAG, "pubCall onFailure $call $t")
                }
            }
        )
    }

    private fun refreshRoles(prefs: SharedPreferences) {
        val roleCall: Call<List<Item<GcdRoleJson, Role>>> = apiService.getRoles()

        roleCall.enqueue(
            object : Callback<List<Item<GcdRoleJson, Role>>> {
                override fun onResponse(
                    call: Call<List<Item<GcdRoleJson, Role>>>,
                    response: Response<List<Item<GcdRoleJson, Role>>>
                ) {
                    Log.d(TAG, "pubCall onResponse ${call.request()} ${response}")

                    if (response.code() == 200) {
                        val roleList: List<Item<GcdRoleJson, Role>>? = response.body()

                        roleList?.map {
                            Role.fromItem(it)
                        }?.let {
                            executor.execute {
                                issueDao.insertRole(*it.toTypedArray())
                            }

                            val editor = prefs.edit()
                            editor.putString("role_list_updated", LocalDate.now().toString())
                            editor.apply()
                        }
                    }
                }

                override fun onFailure(
                    call: Call<List<Item<GcdRoleJson, Role>>>, t: Throwable
                ) {
                    Log.d(TAG, "roleCall onFailure $call $t")
                }
            }
        )
    }

    private fun refreshStoryTypes(prefs: SharedPreferences) {
        val storyTypeCall: Call<List<Item<GcdStoryType, StoryType>>> = apiService.getStoryTypes()

        storyTypeCall.enqueue(
            object : Callback<List<Item<GcdStoryType, StoryType>>> {
                override fun onResponse(
                    call: Call<List<Item<GcdStoryType, StoryType>>>,
                    response: Response<List<Item<GcdStoryType, StoryType>>>
                ) {
                    if (response.code() == 200) {
                        val storyTypeList: List<Item<GcdStoryType, StoryType>>? = response.body()

                        storyTypeList?.map {
                            it.toRoomModel()
                        }?.let {
                            executor.execute {
                                issueDao.insertStoryType(*it.toTypedArray())
                            }
                        }
                    }
                }

                override fun onFailure(
                    call: Call<List<Item<GcdStoryType, StoryType>>>,
                    t: Throwable
                ) {
                    Log.d(TAG, "storyTypeCall onFailure ${call.request()} $t")
                }

            }
        )
    }

    private fun refreshSeries(prefs: SharedPreferences, page: Int) {
        val seriesCall: Call<List<Item<GcdSeriesJson, Series>>> = apiService.getSeries(page)

        seriesCall.enqueue(
            object : Callback<List<Item<GcdSeriesJson, Series>>> {
                override fun onResponse(
                    call: Call<List<Item<GcdSeriesJson, Series>>>,
                    response: Response<List<Item<GcdSeriesJson, Series>>>
                ) {
                    Log.d(
                        TAG,
                        "seriesCall onResponse ${call.request()} ${response}"
                    )
                    if (response.code() == 200) {
                        val seriesList: List<Item<GcdSeriesJson, Series>>? =
                            response.body()

                        Log.d(TAG, "seriesList: $seriesList")

                        seriesList?.let {
                            if (seriesList.size > 0) {
                                refreshSeries(prefs, page + 1)

                                seriesList.map {
                                    Log.d(TAG, it.pk.toString())
                                    it.toRoomModel()
                                }.let {
                                    executor.execute {
                                        issueDao.insertSeries(*it.toTypedArray())
                                    }

                                    val editor = prefs.edit()
                                    editor.putString(
                                        "series_list_updated",
                                        LocalDate.now().toString()
                                    )
                                    editor.apply()
                                }
                            }
                        }
                    }
                }

                override fun onFailure(
                    call: Call<List<Item<GcdSeriesJson, Series>>>,
                    t: Throwable
                ) {
                    Log.d(TAG, "seriesCall onFailure ${call.request()} $t")
                    val res = null
                }
            }
        )
    }

    fun getStories(issueId: Int): LiveData<List<Story>> {
        refreshStories(issueId)
        return issueDao.getStories(issueId)
    }

    fun refreshStories(issueId: Int) {
        val storiesCall: Call<List<Item<GcdStory, Story>>> = apiService.getStories(issueId)

        storiesCall.enqueue(
            object : Callback<List<Item<GcdStory, Story>>> {
                override fun onResponse(
                    call: Call<List<Item<GcdStory, Story>>>,
                    response: Response<List<Item<GcdStory, Story>>>
                ) {
                    if (response.code() == 200) {
                        Log.d(TAG, "storiesCall success: $response")
                        val stories: List<Item<GcdStory, Story>>? = response.body()

                        stories?.let {
                            Log.d(TAG, "about to refresh creators!")
                            refreshCredits(issueId, it.map { item -> item.toRoomModel() })
                        }
                    }
                }

                override fun onFailure(call: Call<List<Item<GcdStory, Story>>>, t: Throwable) {
                    Log.d(TAG, "storiesCall failure: ${call.request()} $t")
                }

            }
        )
    }

    private fun refreshCredits(issueId: Int, stories: List<Story>) {
        val creditsCall: Call<List<Item<GcdStoryCredit, Credit>>> = apiService.getCredits(issueId)

        creditsCall.enqueue(
            object : Callback<List<Item<GcdStoryCredit, Credit>>> {
                override fun onResponse(
                    call: Call<List<Item<GcdStoryCredit, Credit>>>,
                    response: Response<List<Item<GcdStoryCredit, Credit>>>
                ) {
                    if (response.code() == 200) {
                        val credits: List<Item<GcdStoryCredit, Credit>>? = response.body()

                        credits?.let {
                            val creditModels = it.map {
                                it.toRoomModel()
                            }

                            val creatorModels = it.map {
                                it.fields.getCreatorModel()
                            }

                            executor.execute {
                                issueDao.insertCreditTransaction(
                                    stories.toTypedArray(),
                                    creatorModels.toTypedArray(),
                                    creditModels.toTypedArray()
                                )
                            }

                        }
                    }
                }

                override fun onFailure(
                    call: Call<List<Item<GcdStoryCredit, Credit>>>,
                    t: Throwable
                ) {
                    Log.d(TAG, "creditsCall failure: ${call.request()} $t")
                }

            }
        )
    }

    //    fun refreshCreatorByName(name: String): LiveData<Creator?> {
//        val creatorCall: Call<Item<GcdCreator, Creator>> = apiService.getCreatorByName(name)
//
//        creatorCall.enqueue(
//            object : Callback<Item<GcdCreator, Creator>> {
//                override fun onResponse(
//                    call: Call<Item<GcdCreator, Creator>>,
//                    response: Response<Item<GcdCreator, Creator>>
//                ) {
//                    Log.d(TAG, "creatorByNameCall response: ${call.request()} $response")
//
//                    if (response.code() == 200) {
//                        val creator: Item<GcdCreator, Creator>? = response.body()
//
//                        creator?.let {
//                            executor.execute {
//                                issueDao.insertCreator(it.toRoomModel())
//                            }
//                        }
//                    }
//                }
//
//                override fun onFailure(call: Call<Item<GcdCreator, Creator>>, t: Throwable) {
//                    Log.d(TAG, "creatorByNameCall failure: ${call.request()} $t")
//                }
//
//            }
//        )
//
//        return issueDao.getCreatorByName(name)
//    }
//
//    private fun refreshStoryCredits(stories: List<Item<GcdStory, Story>>, issueId: Int) {
//        /*
//        For each field [script, pencils, inks, colors, letters, editing], extract name(s) by
//        splitting on [;], removing non-relevant characters [(...), ?, etc.]
//
//        For each namend get
//         */
//        val creators = arrayListOf<String>()
//
//        for (storyItem in stories) {
//            val script_names = storyItem.fields.script.split("; ")
//            for (name in script_names) {
//                val cleaned_name = name.replace(regex = Regex(""), "")
//                creators.add(cleaned_name)
//            }
//            refreshCreatorByName(creators)
//        }
//
//        refreshCredits(issueId, stories.map { it.toRoomModel() }, creators)
//    }
//
    fun getIssuesBySeries(seriesId: Int): LiveData<List<FullIssue>> {
        updateIssuesBySeries(seriesId)
        return issueDao.getIssuesBySeries(seriesId)
    }

    fun updateIssuesBySeries(seriesId: Int) {
        val issuesCall: Call<List<Item<GcdIssueJson, Issue>>> =
            apiService.getIssuesBySeries(seriesId)

        issuesCall.enqueue(
            object : Callback<List<Item<GcdIssueJson, Issue>>> {
                override fun onResponse(
                    call: Call<List<Item<GcdIssueJson, Issue>>>,
                    response: Response<List<Item<GcdIssueJson, Issue>>>
                ) {
                    Log.d(TAG, "issuesCall onResponse ${call.request()} ${response}")
                    if (response.code() == 200) {
                        val issueList: List<Item<GcdIssueJson, Issue>>? = response.body()

                        Log.d(TAG, "issueList: $issueList")

                        issueList?.map {
                            it.toRoomModel()
                        }?.let {
                            executor.execute {
                                issueDao.insertIssue(*it.toTypedArray())
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<List<Item<GcdIssueJson, Issue>>>, t: Throwable) {
                    Log.d(TAG, "issuesCall onFailure ${call.request()} $t")
                    val res = null
                }
            }
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
                    issueDao.insertPublisher(
                        publisher,
                    )

                    issueDao.insertSeries(
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

    fun addIssue(issue: Issue) {
        executor.execute {
            try {
                issueDao.insertIssue(issue)
            } catch (e: SQLiteConstraintException) {
                // TODO: some real exception handling
                Log.d(TAG, "addIssue: $e")
            }
        }
    }

    fun addSeries(series: Series) {
        executor.execute {
            try {
                issueDao.insertSeries(series)
            } catch (e: SQLiteConstraintException) {
                Log.d(TAG, "addSeries: $e")
            }
        }
    }

    fun addCreator(creator: Creator) {
        executor.execute {
            issueDao.insertCreator(creator)
        }
    }

    fun addRole(role: Role) {
        executor.execute {
            issueDao.insertRole(role)
        }
    }

    fun addCredit(credit: Credit) {
        executor.execute {
            try {
                issueDao.insertCredit(credit)
            } catch (e: SQLiteConstraintException) {
                Log.d(TAG, "addCredit: $e")
                // TODO: notify user that they are updating an existing item
                issueDao.updateCredit(credit)
            }
        }
    }

    fun updateIssue(issue: Issue) {
        executor.execute {
            try {
                issueDao.updateIssue(issue)
            } catch (e: SQLiteConstraintException) {
                // TODO: some real exception handling
                Log.d(TAG, "updateIssue: $e")
            }
        }
    }

    fun updateSeries(series: Series) {
        executor.execute {
            try {
                issueDao.updateSeries(series)
            } catch (e: SQLiteConstraintException) {
                // TODO: some real exception handling
                Log.d(TAG, "updateSeries: $e")
            }
        }
    }

    fun deleteIssue(issue: Issue) {
        executor.execute {
            issueDao.deleteIssue(issue)
        }
    }

    fun deleteSeries(series: Series) {
        executor.execute {
            issueDao.deleteSeries(series)
        }
    }

    fun getSeriesList(): LiveData<List<Series>> {
//        TODO("update this to work with getSeriesList in IssueDao")
        return issueDao.getAllSeries()
    }

    fun getSeries(seriesId: Int): LiveData<Series?> =
        issueDao.getSeriesById(seriesId)

    fun updateCreator(creator: Creator) {
        executor.execute {
            issueDao.updateCreator(creator)
        }
    }

    fun updateRole(role: Role) {
        executor.execute {
            issueDao.updateRole(role)
        }
    }

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
            issueDao.updateCredit(credit)
        }
    }

    fun deleteCreator(creator: Creator) {
        executor.execute {
            issueDao.deleteCreator(creator)
        }
    }

    fun deleteRole(role: Role) {
        executor.execute {
            issueDao.deleteRole(role)
        }
    }

    fun deleteCredit(credit: Credit) {
        executor.execute {
            issueDao.deleteCredit(credit)
        }
    }

    fun getPublisher(publisherId: Int): LiveData<Publisher?> = issueDao.getPublisher(publisherId)

    fun getFullIssue(issueId: Int): LiveData<IssueAndSeries?> = issueDao.getFullIssue(issueId)

    fun getIssueCredits(issueId: Int): LiveData<List<FullCredit>> {
        refreshStories(issueId)
        return issueDao.getIssueCredits(issueId)
    }


    fun getSeriesByCreator(creatorId: Int): LiveData<List<Series>> =
        issueDao.getSeriesList(creatorId)

    fun getCreatorBySeries(seriesId: Int): LiveData<List<Creator>> =
        issueDao.getCreatorList(seriesId)

/*
    FUTURE IMPLEMENTATION
    fun getCoverImage(issue: Issue): File = File(filesDir, issue.coverFileName)
*/

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
    }
}

