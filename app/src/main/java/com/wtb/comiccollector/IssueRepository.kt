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
import androidx.lifecycle.liveData
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.comiccollector.GroupListViewModels.GroupListViewModel
import com.wtb.comiccollector.database.IssueDatabase
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

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
    private val characterDao = database.characterDao()
    private val appearanceDao = database.appearanceDao()

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

    fun getSeriesByFilter(filter: GroupListViewModel.Filter): LiveData<List<Series>> {
        filter.filterId?.let { CreatorUpdater().update(it) }
        return seriesDao.getSeriesByFilter(filter)
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
                    withContext(Dispatchers.Default) {
                        database.transactionDao().upsertStatic(
                            publishers = publishers.await().map { it.toRoomModel() },
                            roles = roles.await().map { it.toRoomModel() },
                            storyTypes = storyTypes.await().map { it.toRoomModel() }
                        )
                    }.let {
                        saveTime(prefs, STATIC_DATA_UPDATED)
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
                withContext(Dispatchers.Default) {
                    apiService.getSeries(page)
                }.let { seriesItems ->
                    if (seriesItems.isEmpty()) {
                        stop = true
                    } else {
                        seriesDao.upsertSus(seriesItems.map { it.toRoomModel() })
                    }
                }
                page += 1
            } while (!stop)
            saveTime(prefs, SERIES_LIST_UPDATED)
        }
    }

    inner class CreditUpdater {
        internal fun update(issueId: Int) {
            if (checkIfStale("${issueId}_updated", ISSUE_LIFETIME)) {
                val storyItemsCall = GlobalScope.async {
                    Log.d(TAG, "WEBSERVICE: storiesByIssue $issueId")
                    apiService.getStoriesByIssue(issueId)
                }

                val creditItemsCall = GlobalScope.async {
                    storyItemsCall.await().let { storyItems ->
                        if (storyItems.isNotEmpty()) {
                            Log.d(TAG, "WEBSERVICE: creditsByStories $storyItems")
                            apiService.getCreditsByStories(storyItems.map { item -> item.pk })
                        } else {
                            null
                        }
                    }
                }

                val nameDetailItemsCall = GlobalScope.async {
                    creditItemsCall.await()?.let { creditItems ->
                        if (creditItems.isNotEmpty()) {
                            Log.d(TAG, "WEBSERVICE: nameDetails $creditItems")
                            apiService.getNameDetailsByIds(creditItems.map { it.fields.nameDetailId })
                        } else {
                            null
                        }
                    }
                }

                val creatorItemsCall = GlobalScope.async {
                    nameDetailItemsCall.await()?.let { nameDetailItems ->
                        if (nameDetailItems.isNotEmpty()) {
                            Log.d(TAG, "WEBSERVICE: creator $nameDetailItems")
                            apiService.getCreator(nameDetailItems.map { it.fields.creatorId })
                        } else {
                            null
                        }
                    }
                }

                GlobalScope.launch {
                    withContext(Dispatchers.Default) {
                        val stories = storyItemsCall.await().map { it.toRoomModel() }
                        val credits = creditItemsCall.await()?.map { it.toRoomModel() }
                        val nameDetails = nameDetailItemsCall.await()?.map { it.toRoomModel() }
                        val creators = creatorItemsCall.await()?.map { it.toRoomModel() }

                        database.transactionDao().upsertSus(
                            stories = stories,
                            credits = credits,
                            nameDetails = nameDetails,
                            creators = creators
                        )
                    }.let {
//                        CharacterExtractor().extractCharacters(storyItems.await())
                        CreditExtractor().extractCredits(storyItemsCall.await())
                    }
                }
            }
        }
    }

    inner class CreatorUpdater {

        internal fun update(creatorId: Int) {
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
                    Log.d(TAG, "WEBSERVICE: storiesByName $it")
                    apiService.getStoriesByName(it)
                }
            }

            val issuesDef: Deferred<List<Item<GcdIssue, Issue>>?> = GlobalScope.async {
                stories.await()?.let {
                    val issueIds = it.map { item -> item.fields.issueId }
                    Log.d(TAG, "WEBSERVICE: issues (from stories OLD) $issueIds")
                    apiService.getIssues(issueIds)
                }
            }

            val variants: Deferred<List<Item<GcdIssue, Issue>>?> = GlobalScope.async {
                issuesDef.await()?.let { issueItems ->
                    val issues: List<Issue> = issueItems.map { it.toRoomModel() }
                    val ids: List<Int> = issues.mapNotNull { it.variantOf }

                    if (ids.isNotEmpty()) {
                        Log.d(TAG, "WEBSERVICE: issues (from variantOf OLD) $ids")
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
                withContext(Dispatchers.Default) {
                    storiesInserted.await().let {
//                        CharacterExtractor().extractCharacters(stories.await())
                        CreditExtractor().extractCredits(stories.await())
                    }
                }.let {
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
                    Log.d(TAG, "WEBSERVICE: creditsByNameDetail $it")
                    apiService.getCreditsByNameDetail(listOf(it.nameDetailId))
                }
            }

            val stories = GlobalScope.async {
                credits.await()?.let {
                    val storyIds = it.map { item -> item.toRoomModel().storyId }
                    if (storyIds.isNotEmpty()) {
                        Log.d(TAG, "WEBSERVICE: stories $storyIds")
                        apiService.getStories(storyIds)
                    } else {
                        null
                    }
                }
            }

            val issues: Deferred<List<Item<GcdIssue, Issue>>?> = GlobalScope.async {
                stories.await()?.let {
                    val issueIds = it.map { item -> item.toRoomModel().issueId }
                    Log.d(TAG, "WEBSERVICE: issues (from stories NEW) $issueIds")
                    apiService.getIssues(issueIds)
                }
            }

            val variants: Deferred<List<Item<GcdIssue, Issue>>?> = GlobalScope.async {
                issues.await()?.let {
                    val issueIds = it.mapNotNull { item -> item.toRoomModel().variantOf }
                    if (issueIds.isNotEmpty()) {
                        Log.d(TAG, "WEBSERVICE: issues (from variantOf NEW) $issueIds")
                        apiService.getIssues(issueIds)
                    } else {
                        null
                    }
                }
            }

            GlobalScope.launch {
                val stories1 = stories.await()?.map { it.toRoomModel() }
                Log.d(TAG, "SIDS: ${stories1?.map { it.storyId }}")
                val variants1 = variants.await()?.map { it.toRoomModel() } ?: emptyList()
                Log.d(TAG, "VIDS: ${variants1.map { it.issueId }}")
                val issues1 = issues.await()?.map { it.toRoomModel() } ?: emptyList()
                Log.d(TAG, "IIDS: ${issues1.map { it.issueId }}")
                val credits1 = credits.await()?.map { it.toRoomModel() }
                Log.d(TAG, "SIDS: ${credits1?.map { it.storyId }}")
                Log.d(TAG, "NIDS: ${nameDetail.await()}")
                Log.d(TAG, "NIDS: ${credits1?.map { it.nameDetailId }}")
                database.transactionDao().upsertSus(
                    stories = stories1,
                    issues = variants1 + issues1,
                    credits = credits1
                )
            }

        }
    }

    inner class IssueUpdater {
        internal fun update(seriesId: Int) {
            if (checkIfStale("${seriesId}_updated", ISSUE_LIFETIME))
                GlobalScope.launch {
                    withContext(Dispatchers.Default) {
                        Log.d(TAG, "WEBSERVICE: issuesBySeries $seriesId")
                        apiService.getIssuesBySeries(seriesId)
                    }.let { issueItems ->
                        issueDao.upsertSus(issueItems.map { it.toRoomModel() })
                    }
                }
        }
    }

    inner class CharacterExtractor {
        suspend fun extractCharacters(stories: List<Item<GcdStory, Story>>?) {
            stories?.forEach { gcdStory ->
                val story = gcdStory.fields

                val characters = story.characters.split("; ")

                characters.forEach { character ->
                    GlobalScope.launch {
                        makeCharacterCredit(character, gcdStory.pk)
                    }
                }
            }
        }

        private suspend fun makeCharacterCredit(characterName: String, pk: Int) {

            var info: String? = null
            val infoRegex = Pattern.compile("\\((.*?)\\)")
            val infoMatcher = infoRegex.matcher(characterName)

            if (infoMatcher.find()) {
                info = infoMatcher.group(1)
            }

            val name = characterName.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")

            val character = GlobalScope.async {
                characterDao.getCharacterByInfo(name)
            }

            character.await().let { chars ->
                if (chars != null && chars.isNotEmpty()) {
                    appearanceDao.upsertSus(
                        listOf(
                            Appearance(
                                storyId = pk,
                                characterId = chars[0].characterId,
                                details = info
                            )
                        )
                    )
                } else {
                    withContext(Dispatchers.Default) {
                        characterDao.upsertSus(
                            listOf(
                                Character(
                                    name = name,
                                )
                            )
                        )
                    }.let {
                        withContext(Dispatchers.Default) {
                            characterDao.getCharacterByInfo(name)
                        }.let {
                            if (it != null && it.isNotEmpty()) {
                                appearanceDao.upsertSus(
                                    listOf(
                                        Appearance(
                                            storyId = pk,
                                            characterId = it[0].characterId,
                                            details = info
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        private fun handleRest(subList: List<String>) {
            TODO("Not yet implemented")
        }

        private fun handleFirst(s: String) {
            TODO("Not yet implemented")
        }
    }

    inner class CreditExtractor {
        suspend fun extractCredits(stories: List<Item<GcdStory, Story>>?) {
            stories?.forEach { gcdStory ->
                val story = gcdStory.fields

                checkField(story.script, gcdStory.pk, Role.Companion.Name.SCRIPT.value)
                checkField(story.pencils, gcdStory.pk, Role.Companion.Name.PENCILS.value)
                checkField(story.inks, gcdStory.pk, Role.Companion.Name.INKS.value)
                checkField(story.colors, gcdStory.pk, Role.Companion.Name.COLORS.value)
                checkField(story.letters, gcdStory.pk, Role.Companion.Name.LETTERS.value)
                checkField(story.editing, gcdStory.pk, Role.Companion.Name.EDITING.value)
            }
        }

        private suspend fun checkField(value: String, storyId: Int, roleId: Int) {
            if (value != "") {
                value.split("; ").map { name ->
                    var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
                    res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
                    makeCredit(res, storyId, roleId)
                }
            }
        }

        private suspend fun makeCredit(extracted_name: String, storyId: Int, roleId: Int) {

            val checkLocalCreators = GlobalScope.async {
                creatorDao.getCreatorByNameSus(extracted_name)
            }

            checkLocalCreators.await().let { localCreators: List<Creator>? ->
                if (localCreators == null || localCreators.isEmpty()) {
                    val nameDetails = GlobalScope.async {
                        apiService.getNameDetailByName(extracted_name).map { it.toRoomModel() }
                    }

                    nameDetails.await().let { nameDetailItems1: List<NameDetail> ->
                        if (nameDetailItems1.isNotEmpty()) {
                            val creatorIds = nameDetailItems1.map { it.creatorId }

                            val creators = GlobalScope.async {
                                if (creatorIds.isNotEmpty()) {
                                    apiService.getCreator(creatorIds).map { it.toRoomModel() }
                                } else {
                                    null
                                }
                            }

                            creators.await()?.let { it: List<Creator> ->
                                if (it.size > 1) {
                                    Log.d(
                                        TAG,
                                        "Multiple creator matches: $extracted_name ${it.size}"
                                    )
                                }
                                withContext(Dispatchers.Default) {
                                    creatorDao.upsertSus(it)
                                }.let {
                                    withContext(Dispatchers.Default) {
                                        nameDetailDao.upsertSus(nameDetailItems1)
                                    }.let {
                                        withContext(Dispatchers.Default) {
                                            apiService.getNameDetailsByCreatorIds(creatorIds)
                                        }
                                            .let { ndItems: List<Item<GcdNameDetail, NameDetail>> ->
                                                withContext(Dispatchers.Default) {
                                                    nameDetailDao.upsertSus(ndItems.map { it.toRoomModel() })
                                                }.let {
                                                    creditDao.upsertSus(
                                                        listOf(
                                                            Credit(
                                                                storyId = storyId,
                                                                nameDetailId = ndItems[0].pk,
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
                } else {
                    GlobalScope.launch {
                        val nameDetail = GlobalScope.async {
                            localCreators[0].creatorId.let { id ->
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

    // TODO: This should probably get moved out of SharedPreferences and stored with each record.
//  The tradeoff: an extra local db query vs. having a larger prefs which will end up having
//  a value for every item in the database.
    private fun checkIfStale(prefsKey: String, shelfLife: Long): Boolean {
        val lastUpdated = LocalDate.parse(prefs.getString(prefsKey, "${LocalDate.MIN}"))
        Log.d(TAG, "$prefsKey ${lastUpdated.plusDays(14)}")
        return lastUpdated.plusDays(shelfLife) < LocalDate.now()
    }

    fun getVariants(issueId: Int): LiveData<List<Issue>> {
        val variantsCall = GlobalScope.async {
            issueDao.getVariants(issueId)
        }

        val issueCall = GlobalScope.async {
            variantsCall.await().let {
                if (it.size == 1 && it[0].variantOf != null) {
                    issueDao.getVariants(it[0].variantOf!!)
                } else {
                    it
                }
            }
        }

        return liveData { emit(issueCall.await()) }
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
