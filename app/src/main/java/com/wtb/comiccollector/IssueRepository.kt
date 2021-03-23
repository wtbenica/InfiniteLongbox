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

const val EXTERNAL = "http://24.176.172.169/"
const val NIGHTWING = "http://192.168.0.141:8000/"
const val ALFRED = "http://192.168.0.138:8000/"
const val BASE_URL = ALFRED

class IssueRepository private constructor(context: Context) {

    private var prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)

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
    private val nameDetailDao = database.nameDetailDao()

    private val filesDir = context.applicationContext.filesDir
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val apiService: Webservice by lazy {
        retrofit.create(Webservice::class.java)
    }

    private val staticUpdater = StaticUpdater()
    private val creatorUpdater = CreatorUpdater()
    private val creditUpdater = CreditUpdater()

    var allSeries: LiveData<List<Series>> = seriesDao.getAllSeries()

    val allPublishers: LiveData<List<Publisher>> = publisherDao.getPublishersList()

    val allCreators: LiveData<List<Creator>> = creatorDao.getCreatorsList()

    val allRoles: LiveData<List<Role>> = roleDao.getRoleList()

    init {
        val lastUpdate = LocalDate.parse(
            prefs.getString(STATIC_DATA_UPDATED, LocalDate.MIN.toString())
        )

        if (lastUpdate.plusDays(STALE_DATA_NUM_DAYS) < LocalDate.now()) {
            staticUpdater.update(prefs)
        }
    }


    fun getSeries(seriesId: Int): LiveData<Series?> = seriesDao.getSeriesById(seriesId)

    fun getPublisher(publisherId: Int): LiveData<Publisher?> =
        publisherDao.getPublisher(publisherId)

    fun getFullIssue(issueId: Int): LiveData<IssueAndSeries?> = issueDao.getFullIssue(issueId)

    /**
     * Gets all stories from issue with pk issueId and also triggers an update from the server
     *
     * @param issueId the pk of the issue whose stories are being requested
     */
    fun getStoriesByIssue(issueId: Int): LiveData<List<Story>> {
        creditUpdater.update(issueId)
        return storyDao.getStories(issueId)
    }

    fun getIssueCredits(issueId: Int): LiveData<List<FullCredit>> {
        return creditDao.getIssueCredits(issueId)
    }

    fun getSeriesByCreator(creatorId: Int): LiveData<List<Series>> {
        creatorUpdater.update(creatorId)
        return seriesDao.getSeriesList(creatorId)
    }

    fun getCreatorBySeries(seriesId: Int): LiveData<List<Creator>> =
        creatorDao.getCreatorList(seriesId)


    /**
    //    private fun extractCredits(stories: List<Item<GcdStory, Story>>?) {
    //        executor.execute {
    //            stories?.map {
    //                it.toRoomModel()
    //            }.apply {
    //                this?.let { storyDao.upsert(it) }
    //            }
    //
    //            stories?.forEach { gcdStory ->
    //                val fields = gcdStory.fields
    //
    //                if (fields.script != "") {
    //                    fields.script.split("; ").map { name ->
    //                        var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
    //                        res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
    //                        Log.d(TAG, "SCRIPT: $res")
    //                        makeCredit(res, gcdStory.pk, Role.Companion.Name.SCRIPT.value)
    //                    }
    //                }
    //
    //                if (fields.pencils != "") {
    //                    fields.pencils.split("; ").map { name ->
    //                        var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
    //                        res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
    //                        Log.d(TAG, "PENCILS: $res")
    //                        makeCredit(res, gcdStory.pk, Role.Companion.Name.PENCILS.value)
    //                    }
    //                }
    //
    //                if (fields.inks != "") {
    //                    fields.inks.split("; ").map { name ->
    //                        var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
    //                        res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
    //                        Log.d(TAG, "INKS: $res")
    //                        makeCredit(res, gcdStory.pk, Role.Companion.Name.INKS.value)
    //                    }
    //                }
    //
    //                if (fields.colors != "") {
    //                    fields.colors.split("; ").map { name ->
    //                        var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
    //                        res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
    //                        Log.d(TAG, "COLORS: $res")
    //                        makeCredit(res, gcdStory.pk, Role.Companion.Name.COLORS.value)
    //                    }
    //                }
    //                if (fields.letters != "") {
    //                    fields.letters.split("; ").map { name ->
    //                        var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
    //                        res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
    //                        Log.d(TAG, "LETTERS: $res")
    //                        makeCredit(res, gcdStory.pk, Role.Companion.Name.LETTERS.value)
    //                    }
    //                }
    //
    //                if (fields.editing != "") {
    //                    fields.editing.split("; ").map { name ->
    //                        var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
    //                        res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
    //                        Log.d(TAG, "EDITS: $res")
    //                        makeCredit(res, gcdStory.pk, Role.Companion.Name.EDITING.value)
    //                    }
    //                }
    //            }
    //        }
    //    }
    //
    //    private fun makeCredit(extracted_name: String, storyId: Int, roleId: Int) {
    //        val nameDetailCall = apiService.getCreatorByName(extracted_name)
    //
    //        nameDetailCall.enqueue(
    //            object : Callback<List<Item<GcdNameDetail, NameDetail>>> {
    //                override fun onResponse(
    //                    call: Call<List<Item<GcdNameDetail, NameDetail>>>,
    //                    response: Response<List<Item<GcdNameDetail, NameDetail>>>
    //                ) {
    //                    if (response.code() == 200) {
    //                        val nameDetail = response.body()
    //                        // TODO: Need to handle multiple options (i.e. size > 1)
    //                        nameDetail?.let { nameDetails ->
    //                            if (nameDetails.size > 1) {
    //                                // Pick which one dialog
    //                            }
    //                            if (nameDetails.size > 0) {
    //                                val creatorCall =
    //                                    apiService.getCreator(nameDetails[0].fields.creatorId)
    //
    //                                creatorCall.enqueue(
    //                                    StandardCall(
    //                                        callName = "creatorCall (nd: ${nameDetails[0].toRoomModel
    //                                            ()}) (sid: $storyId) (rid: $roleId)",
    //                                        preprocess = { creatorItemList ->
    //
    //                                        },
    //                                        commit_call = { creatorList ->
    //                                            Log.d(TAG, "$")
    //                                            executor.execute {
    //                                                database.transactionDao().upsert(
    //                                                    creators = creatorList,
    //                                                    nameDetails = nameDetails.map { item -> item.toRoomModel() },
    //                                                    credits = listOf(
    //                                                        Credit(
    //                                                            storyId = storyId,
    //                                                            nameDetailId = nameDetails[0].toRoomModel().nameDetailId,
    //                                                            roleId = roleId
    //                                                        )
    //                                                    ),
    //                                                )
    //                                            }
    //                                        },
    //                                        prefs_key = null,
    //                                        prefs = prefs
    //                                    )
    //                                )
    //                            }
    //                        }
    //                    }
    //                }
    //
    //                override fun onFailure(
    //                    call: Call<List<Item<GcdNameDetail, NameDetail>>>, t:
    //                    Throwable
    //                ) {
    //                    Log.d(TAG, "creatorCall failure ${call.request()} $t")
    //                }
    //            }
    //        )
    //    }
    //
     */

    fun getIssuesBySeries(seriesId: Int): LiveData<List<FullIssue>> {
        updateIssuesBySeries(seriesId)
        return issueDao.getIssuesBySeries(seriesId)
    }

    private fun updateIssuesBySeries(seriesId: Int) {
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

/*
    FUTURE IMPLEMENTATION
    fun getCoverImage(issue: Issue): File = File(filesDir, issue.coverFileName)
*/

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

    inner class StaticUpdater {

        fun update(prefs: SharedPreferences) {
            refreshStaticData(prefs)
        }

        /**
         *  Updates publisher, series, role, and storytype tables if it has been
         *  more than [STALE_DATA_NUM_DAYS] days since last update
         *
         *  @param prefs
         */
        private fun refreshStaticData(prefs: SharedPreferences) {

            refreshAllPublishers(prefs)
            refreshAllRoles(prefs)
            refreshAllStoryTypes(prefs)
            saveTime(prefs, STATIC_DATA_UPDATED)
        }

        private fun refreshAllPublishers(prefs: SharedPreferences) {
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

        private fun refreshAllRoles(prefs: SharedPreferences) {
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

        private fun refreshAllStoryTypes(prefs: SharedPreferences) {
            val storyTypeCall = apiService.getStoryTypes()

            val commit = { it: List<StoryType> ->
                executor.execute {
                    storyTypeDao.upsert(it)

                    val lastSeriesUpdate = LocalDate.parse(
                        prefs.getString(SERIES_LIST_UPDATED, LocalDate.MIN.toString())
                    )

                    if (lastSeriesUpdate.plusDays(STALE_SERIES_NUM_DAYS) < LocalDate.now()) {
                        refreshAllSeries(prefs)
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
        private fun refreshAllSeries(prefs: SharedPreferences, page: Int = 0) {
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
                                if (seriesList.isNotEmpty()) {
                                    refreshAllSeries(prefs, page + 1)

                                    seriesList.map {
                                        if (it.pk == 5530)
                                            Log.d(TAG, "FUCKING HELL: $it")
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
                    }
                }
            )
        }
    }

    inner class CreditUpdater {

        fun update(issueId: Int) {
            refreshStoriesByIssue(issueId)
        }

        /**
         * Enqueues a retrofit call to update the story data from server
         */
        private fun refreshStoriesByIssue(issueId: Int) {
            val storiesCall = apiService.getStoriesByIssue(issueId)

            storiesCall.enqueue(
                StandardCall(
                    callName = "storiesByIssueCall",
                    preprocess = {
//                                extractCredits(it)
                    },
                    commit_call = { stories ->
                        executor.execute {
                            storyDao.upsert(stories)

                            refreshCreditsByStories(issueId, stories)
                        }
                    },
                    prefs_key = null,
                    prefs = prefs
                )
            )
        }

        private fun refreshCreditsByStories(issueId: Int, stories: List<Story>) {
            val creditsCall = apiService.getCreditsByIssue(issueId)
            apiService.getCreditsByStories(stories.map { it.storyId })

            creditsCall.enqueue(
                StandardCall(
                    callName = "creditsByStoriesCall",
                    preprocess = { gcd_credits ->
                        val nameDetailIds = gcd_credits.map { item ->
                            item.fields.nameDetailId
                        }

                        refreshNameDetailsByIds(
                            nameDetailIds,
                            stories,
                            gcd_credits.map { credit -> credit.toRoomModel() })
                    },
                    commit_call = {

                    },
                    prefs_key = null,
                    prefs = prefs
                )
            )
        }

        private fun refreshNameDetailsByIds(
            nameDetailIds: List<Int>,
            stories: List<Story>,
            credits: List<Credit>
        ) {

            val nameDetailCall: Call<List<Item<GcdNameDetail, NameDetail>>> =
                apiService.getNameDetails(nameDetailIds = nameDetailIds)

            nameDetailCall.enqueue(
                StandardCall(
                    callName = "nameDetailsCall",
                    commit_call = { nameDetails ->
                        refreshCreators(
                            creatorIds = nameDetails.map { nameDetail -> nameDetail.creatorId },
                            nameDetails = nameDetails,
                            stories = stories,
                            credits = credits
                        )
                    },
                )
            )
        }

        private fun refreshCreators(
            creatorIds: List<Int>,
            nameDetails: List<NameDetail>,
            stories: List<Story>,
            credits: List<Credit>
        ) {

            val creatorCall = apiService.getCreator(creatorIds)

            creatorCall.enqueue(
                StandardCall(
                    callName = "creatorsCallRC",
                    commit_call = { creators ->
                        executor.execute {
                            database.transactionDao().upsert(
                                stories = stories,
                                nameDetails = nameDetails,
                                credits = credits,
                                creators = creators
                            )
                        }
                    },
                )
            )
        }
    }

    inner class CreatorUpdater {
        fun update(creatorId: Int) {
            refreshNameDetails(creatorId)
        }

        private fun refreshNameDetails(creatorId: Int) {
            val nameDetailCall = apiService.getNameDetailsByCreatorIds(listOf(creatorId))

            nameDetailCall.enqueue(
                StandardCall(
                    callName = "nameDetailCall",
                    preprocess = {

                    },
                    commit_call = { nameDetailList ->
                        executor.execute {
                            nameDetailDao.upsert(nameDetailList)
                            refreshStoriesByNameDetail(nameDetailList.map { nameDetail -> nameDetail.nameDetailId })
                        }
                    },
                )
            )
        }

        private fun refreshStoriesByNameDetail(nameDetailId: List<Int>) {
            val storyByNameDetail = apiService.getStoriesByNameDetail(nameDetailId)

            storyByNameDetail.enqueue(
                StandardCall(
                    callName = "storyByNameDetailCall",
                    preprocess = {

                    },
                    commit_call = { storyList ->
                        refreshIssuesByStory(storyList)
                    },
                )
            )
        }

        private fun refreshIssuesByStory(storyList: List<Story>) {
            val issuesByStoryCall = apiService.getIssues(storyList.map { it.issueId })

            issuesByStoryCall.enqueue(
                StandardCall(
                    callName = "issuesByStoryCall",
                    preprocess = {

                    },
                    commit_call = { issueList ->
                        refreshVariantOfIssues(storyList, issueList)
                    },
                )
            )
        }

        private fun refreshVariantOfIssues(storyList: List<Story>, issueList: List<Issue>) {
            val variantsCall = apiService.getIssues(issueList.mapNotNull { it.variantOf })

            variantsCall.enqueue(
                StandardCall(
                    callName = "variantsCall",
                    preprocess = {

                    },
                    commit_call = { variantList ->
                        refreshCreditsByStories(storyList, issueList + variantList)
                    },
                    error_call = {
                        refreshCreditsByStories(storyList, issueList)
                    },
                )
            )
        }

        private fun refreshCreditsByStories(storyList: List<Story>, issueList: List<Issue>) {
            val creditsByStoriesCall = apiService.getCreditsByStories(storyList.map { it.storyId })

            creditsByStoriesCall.enqueue(
                StandardCall(
                    callName = "creditsByStoriesCall",
                    preprocess = {

                    },
                    commit_call = { creditList ->
                        refreshNameDetailsByCredits(storyList, issueList, creditList)
                    },
                )
            )
        }

        private fun refreshNameDetailsByCredits(
            storyList: List<Story>,
            issueList: List<Issue>,
            creditList: List<Credit>
        ) {
            val nameDetailsCall = apiService.getNameDetails(creditList.map { it.nameDetailId })

            nameDetailsCall.enqueue(
                StandardCall(
                    callName = "nameDetailsCall",
                    preprocess = {

                    },
                    commit_call = { nameDetailList ->
                        refreshCreatorsByNameDetail(
                            storyList,
                            issueList,
                            creditList,
                            nameDetailList
                        )
                    }
                )
            )
        }

        private fun refreshCreatorsByNameDetail(
            storyList: List<Story>,
            issueList: List<Issue>,
            creditList: List<Credit>,
            nameDetailList: List<NameDetail>
        ) {
            val creatorsCall = apiService.getCreator(nameDetailList.map { it.creatorId })

            creatorsCall.enqueue(
                StandardCall(
                    callName = "creatorsCallRCND",
                    preprocess = {

                    },
                    commit_call = { creatorList ->
                        executor.execute {
                            database.transactionDao().upsert(
                                stories = storyList,
                                creators = creatorList,
                                nameDetails = nameDetailList,
                                credits = creditList,
                                issues = issueList
                            )
                        }
                    }
                )
            )
        }
    }

    class StandardCall<G : GcdJson<D>, D : DataModel>(
        val callName: String,
        val preprocess: ((List<Item<G, D>>) -> Unit)? = null,
        val commit_call: (List<D>) -> Unit,
        val error_call: (() -> Unit)? = null,
        val prefs_key: String? = null,
        val prefs: SharedPreferences? = null
    ) : Callback<List<Item<G, D>>> {
        override fun onResponse(
            call: Call<List<Item<G, D>>>,
            response: Response<List<Item<G, D>>>
        ) {
            if (response.code() == 200) {
                Log.d(TAG, "STANDARD_CALL: $callName ${call.request()} $response")
                val itemList: List<Item<G, D>>? = response.body()

                preprocess?.let {
                    itemList?.let { list ->
                        it(list)
                    }
                }

                itemList?.map {
                    it.toRoomModel()
                }?.let {
                    commit_call(it)

                    if (prefs_key != null && prefs != null) {
                        saveTime(prefs, prefs_key)
                    }
                }
            } else {
                error_call?.invoke()
            }
        }

        override fun onFailure(
            call: Call<List<Item<G, D>>>,
            t: Throwable
        ) {
            Log.d(TAG, "STANDARD_CALL: $callName onFailure ${call.request()} $t")
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

