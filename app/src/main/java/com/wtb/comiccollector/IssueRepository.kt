package com.wtb.comiccollector

import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.comiccollector.database.*
import com.wtb.comiccollector.database.Daos.Count
import com.wtb.comiccollector.database.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.time.LocalDate
import java.util.Collections.sort
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


const val DUMMY_ID = Int.MAX_VALUE

private const val DATABASE_NAME = "issue-database"
private const val TAG = APP + "IssueRepository"
private const val DEBUG = true

internal const val SHARED_PREFS = "CCPrefs"

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

private fun UPDATED_TAG(id: Int, type: String): String = "$type${id}_UPDATED"
private fun ISSUE_TAG(id: Int) = UPDATED_TAG(id, "ISSUE_")
private fun SERIES_TAG(id: Int): String = UPDATED_TAG(id, "SERIES_")
private fun PUBLISHER_TAG(id: Int): String = UPDATED_TAG(id, "PUBLISHER_")
private fun CREATOR_TAG(id: Int): String = UPDATED_TAG(id, "CREATOR_")

class IssueRepository private constructor(val context: Context) {

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
    private val exCreditDao = database.exCreditDao()
    private val storyTypeDao = database.storyTypeDao()
    private val nameDetailDao = database.nameDetailDao()
    private val characterDao = database.characterDao()
    private val appearanceDao = database.appearanceDao()
    private val collectionDao = database.collectionDao()
    private val coverDao = database.coverDao()

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

    var allSeries: LiveData<List<Series>> = seriesDao.getAllOfThem()
    val allPublishers: LiveData<List<Publisher>> = publisherDao.getPublishersList()
    val allCreators: LiveData<List<Creator>> = creatorDao.getCreatorsList()
    val allRoles: LiveData<List<Role>> = roleDao.getRoleList()

    fun filterOptions(filter: Filter): AllFiltersLiveData {
        val seriesList = if (filter.isEmpty()) {
            allSeries
        } else if (filter.mSeries == null) {
            seriesDao.getSeriesByFilterLiveData(filter)
        } else {
            null
        }

        val creatorsList = if (filter.isEmpty()) {
            allCreators
        } else if (filter.mCreators.isEmpty()) {
            creatorDao.getCreatorsByFilter(filter)
        } else {
            null
        }

        val publishersList = if (filter.isEmpty()) {
            allPublishers
        } else if (filter.mPublishers.isEmpty()) {
            publisherDao.getPublishersByFilter(filter)
        } else {
            null
        }

        return AllFiltersLiveData(
            series = seriesList,
            creators = creatorsList,
            publishers = publishersList,
            combine = { series: List<Series>?, creators: List<Creator>?, publishers:
            List<Publisher>? ->
                val res =
                    (series?.toList() as List<FilterOption?>?
                        ?: emptyList()) + (creators as List<FilterOption>?
                        ?: emptyList()) + (publishers as List<FilterOption>? ?: emptyList())
                val x: List<FilterOption> = res.mapNotNull { it }
                sort(x)
                x
            })
    }

    init {
        StaticUpdater().update()
    }

    fun getSeries(seriesId: Int): LiveData<Series?> = seriesDao.getSeries(seriesId)

    fun getPublisher(publisherId: Int): LiveData<Publisher?> =
        publisherDao.getPublisher(publisherId)

    fun getIssue(issueId: Int): LiveData<FullIssue?> {
        IssueCreditUpdater().update(issueId)
        IssueCoverUpdater().update(issueId)
        return issueDao.getFullIssue(issueId)
    }

    fun getIssuesByFilterPagingSource(filter: Filter): PagingSource<Int, FullIssue> {
        val mSeries = filter.mSeries

        if (mSeries != null) {
            val seriesId = mSeries.seriesId
            val creatorIds = filter.mCreators.map { it.creatorId }

            if (filter.hasCreator()) {
                CoroutineScope(Dispatchers.IO).launch {
                    CreatorUpdater().updateAll(creatorIds)
                }
            }

            IssueUpdater().update(seriesId)
        }

        return issueDao.getIssuesByFilterPagingSource(filter)
    }

    fun getIssuesByFilterLiveData(filter: Filter): Flow<List<FullIssue>> {
        val mSeries = filter.mSeries

        if (mSeries != null) {
            val seriesId = mSeries.seriesId
            val creatorIds = filter.mCreators.map { it.creatorId }

            if (filter.hasCreator()) {
                CoroutineScope(Dispatchers.IO).launch {
                    CreatorUpdater().updateAll(creatorIds)
                }
            }

            IssueUpdater().update(seriesId)
        }

        return issueDao.getIssuesByFilterLiveData(filter)
    }

    fun getSeriesByFilter(filter: Filter): PagingSource<Int, Series> {
        Log.d(TAG, "getSeriesByFilter")
        val mSeries = filter.mSeries
        if (mSeries == null) {
            val creatorIds = filter.mCreators.map { it.creatorId }

            if (filter.hasCreator()) {
                Log.d(TAG, "Filter has creator")
                CoroutineScope(Dispatchers.IO).launch {
                    CreatorUpdater().updateAll(creatorIds)
                }
            }

            return seriesDao.getSeriesByFilter(filter)
        } else {
            throw java.lang.IllegalArgumentException("Filter seriesId should be null")
        }
    }

    fun getSeriesByFilterLiveData(filter: Filter): LiveData<List<Series>> {
        val mSeries = filter.mSeries
        if (mSeries == null) {
            val creatorIds = filter.mCreators.map { it.creatorId }

            if (filter.hasCreator()) {
                CoroutineScope(Dispatchers.IO).launch {
                    CreatorUpdater().updateAll(creatorIds)
                }
            }

            return seriesDao.getSeriesByFilterLiveData(filter)
        } else {
            throw java.lang.IllegalArgumentException("Filter seriesId should be null")
        }
    }

    fun getVariants(issueId: Int): LiveData<List<Issue>> {
        val variantsCall = CoroutineScope(Dispatchers.IO).async {
            issueDao.getVariants(issueId)
        }

        val issueCall = CoroutineScope(Dispatchers.IO).async {
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

    fun getStoriesByIssue(issueId: Int): LiveData<List<Story>> = storyDao.getStories(issueId)

    fun getCreditsByIssue(issueId: Int) = AllCreditsLiveData(
        creditDao.getIssueCredits(issueId),
        exCreditDao.getIssueExtractedCredits(issueId),
        combine = { credits1: List<FullCredit>?, credits2: List<FullCredit>? ->
            val res = (credits1 ?: emptyList()) + (credits2 ?: emptyList())
            sort(res)
            res
        })

    fun getCoverByIssueId(issueId: Int): LiveData<Cover?> = coverDao.getCoverByIssueId(issueId)
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
    ).addMigrations(migration_1_2, migration_2_3, migration_3_4, migration_4_5, migration_5_6)
        .build()

    inner class StaticUpdater {
        /**
         *  Updates publisher, series, role, and storytype tables
         */
        internal fun update() {
            if (checkIfStale(STATIC_DATA_UPDATED, STATIC_DATA_LIFETIME)) {
                Log.d(TAG, "StaticUpdater update")
                val publishers = CoroutineScope(Dispatchers.IO).async {
                    apiService.getPublishers()
                }

                val roles = CoroutineScope(Dispatchers.IO).async {
                    apiService.getRoles()
                }

                val storyTypes = CoroutineScope(Dispatchers.IO).async {
                    apiService.getStoryTypes()
                }

                CoroutineScope(Dispatchers.IO).launch {
                    database.transactionDao().upsertStatic(
                        publishers = publishers.await().map { it.toRoomModel() },
                        roles = roles.await().map { it.toRoomModel() },
                        storyTypes = storyTypes.await().map { it.toRoomModel() }
                    )
                        .let {
                            saveTime(prefs, STATIC_DATA_UPDATED)
                            updateSeries()
                        }
                }
            } else {
                if (checkIfStale(SERIES_LIST_UPDATED, SERIES_LIST_LIFETIME)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        updateSeries()
                    }
                }
            }
        }

        private suspend fun updateSeries() {

            var page = 0
            var stop = false

            do {
                apiService.getSeries(page).let { seriesItems ->
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

    /***
     * Updates an issues credits
     */
    inner class IssueCreditUpdater {
        internal fun updateAll(issueIds: List<Int>) {
            issueIds.forEach { update(it) }
        }

        internal fun update(issueId: Int) {
            if (checkIfStale(ISSUE_TAG(issueId), ISSUE_LIFETIME)) {
                Log.d(TAG, "CreditUpdater update $issueId")

                val storyItemsCall = CoroutineScope(Dispatchers.IO).async {
                    apiService.getStoriesByIssue(issueId)
                }

                val creditItemsCall = CoroutineScope(Dispatchers.IO).async {
                    storyItemsCall.await().let { storyItems ->
                        if (storyItems.isNotEmpty()) {
                            apiService.getCreditsByStories(storyItems.map { item -> item.pk })
                        } else {
                            null
                        }
                    }
                }

                val nameDetailItemsCall = CoroutineScope(Dispatchers.IO).async {
                    creditItemsCall.await()?.let { creditItems ->
                        if (creditItems.isNotEmpty()) {
                            apiService.getNameDetailsByIds(creditItems.map { it.fields.nameDetailId })
                        } else {
                            null
                        }
                    }
                }

                val creatorItemsCall = CoroutineScope(Dispatchers.IO).async {
                    nameDetailItemsCall.await()?.let { nameDetailItems ->
                        if (nameDetailItems.isNotEmpty()) {
                            Log.d(TAG, "WEBSERVICE: creator")
                            apiService.getCreator(nameDetailItems.map { it.fields.creatorId })
                        } else {
                            null
                        }
                    }
                }

                val extractedCreditItemsCall = CoroutineScope(Dispatchers.IO).async {
                    storyItemsCall.await().let { storyItems ->
                        if (storyItems.isNotEmpty()) {
                            Log.d(TAG, "WEBSERVICE: extractedCreditsByStories")
                            apiService.getExtractedCreditsByStories(storyItems.map { item -> item.pk })
                        } else {
                            null
                        }
                    }
                }

                val extractedNameDetailItemsCall = CoroutineScope(Dispatchers.IO).async {
                    extractedCreditItemsCall.await()?.let { creditItems ->
                        if (creditItems.isNotEmpty()) {
                            Log.d(TAG, "WEBSERVICE: extractedNameDetails")
                            apiService.getNameDetailsByIds(creditItems.map { it.fields.nameDetailId })
                        } else {
                            null
                        }
                    }
                }

                val extractedCreatorItemsCall = CoroutineScope(Dispatchers.IO).async {
                    extractedNameDetailItemsCall.await()?.let { nameDetailItems ->
                        if (nameDetailItems.isNotEmpty()) {
                            Log.d(TAG, "WEBSERVICE: extractedCreator")
                            apiService.getCreator(nameDetailItems.map { it.fields.creatorId })
                        } else {
                            null
                        }
                    }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val stories = storyItemsCall.await().map { it.toRoomModel() }
                    val credits = creditItemsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val nameDetails =
                        nameDetailItemsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val creators = creatorItemsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val extracts =
                        extractedCreditItemsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val eNameDetails =
                        extractedNameDetailItemsCall.await()?.map { it.toRoomModel() }
                            ?: emptyList()
                    val eCreators =
                        extractedCreatorItemsCall.await()?.map { it.toRoomModel() } ?: emptyList()
                    val allCreators = creators + eCreators
                    val allNameDetails = nameDetails + eNameDetails

                    database.transactionDao().upsertSus(
                        stories = stories,
                        credits = credits,
                        exCredits = extracts,
                        nameDetails = allNameDetails,
                        creators = allCreators,
                    )
                        .let {
//                        CharacterExtractor().extractCharacters(storyItems.await())
//                        CreditExtractor().extractCredits(storyItemsCall.await())
                        }
                }
            }
        }
    }

    inner class IssueCoverUpdater {
        internal fun update(issueId: Int) {
            Log.d(TAG, "CoverUpdater_____________________________")
            CoroutineScope(Dispatchers.IO).launch {
                val issueDef =
                    CoroutineScope(Dispatchers.IO).async { issueDao.getIssueSus(issueId) }
                Log.d(TAG, "MADE IT THIS FAR!")
                issueDef.await()?.let { issue ->
                    if (issue.coverUri == null) {
                        Log.d(TAG, "COVER UPDATER needsCover... starting")
                        CoroutineScope(Dispatchers.IO).launch {
                            kotlin.runCatching {
                                Log.d(TAG, "COVER UPDATER Starting connection.....")
                                val doc = Jsoup.connect(issue.issue.url).get()
                                val noCover = doc.getElementsByClass("no_cover").size == 1

                                val coverImgElements = doc.getElementsByClass("cover_img")
                                val wraparoundElements =
                                    doc.getElementsByClass("wraparound_cover_img")
                                val elements = if (coverImgElements.size > 0) {
                                    coverImgElements
                                } else if (wraparoundElements.size > 0) {
                                    wraparoundElements
                                } else {
                                    null
                                }

                                val src = elements?.get(0)?.attr("src")

                                val url = src?.let { URL(it) }

                                if (!noCover && url != null) {
                                    val image = CoroutineScope(Dispatchers.IO).async {
                                        url.toBitmap()
                                    }
                                    CoroutineScope(Dispatchers.Default).launch {
                                        val bitmap = image.await().also {
                                            Log.d(TAG, "COVER UPDATER Got an image!")
                                        }

                                        bitmap?.apply {
                                            val savedUri: Uri? =
                                                saveToInternalStorage(
                                                    context,
                                                    issue.issue.coverFileName
                                                )

                                            Log.d(TAG, "COVER UPDATER Saving image $savedUri")
                                            val cover =
                                                Cover(issueId = issueId, coverUri = savedUri)
                                            coverDao.upsertSus(listOf(cover))
                                        }
                                    }
                                } else {
                                    Log.d(TAG, "COVER UPDATER No Cover Found")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun URL.toBitmap(): Bitmap? {
        return try {
            BitmapFactory.decodeStream(openStream())
        } catch (e: IOException) {
            null
        }
    }

    fun Bitmap.saveToInternalStorage(context: Context, uri: String): Uri? {
        val wrapper = ContextWrapper(context)

        var file = wrapper.getDir("images", Context.MODE_PRIVATE)

        file = File(file, uri)

        return try {
            val stream = FileOutputStream(file)

            compress(Bitmap.CompressFormat.JPEG, 100, stream)

            stream.flush()

            stream.close()

            Uri.parse(file.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    inner class CreatorUpdater {

        internal fun updateAll(creatorIds: List<Int>) {
            creatorIds.forEach { update(it) }
        }

        internal fun update(creatorId: Int) {
            if (checkIfStale(CREATOR_TAG(creatorId), CREATOR_LIFETIME)) {
                Log.d(TAG, "CreatorUpdater update $creatorId")
                refreshCredits(creatorId)
            }
        }

        private fun refreshCredits(creatorId: Int) {

            val nameDetailCall = CoroutineScope(Dispatchers.IO).async {
                nameDetailDao.getNameDetailByCreatorIdSus(creatorId)
            }

            val creditsCall = CoroutineScope(Dispatchers.IO).async {
                nameDetailCall.await()?.let { nameDetails ->
                    apiService.getCreditsByNameDetail(nameDetails.map { it.nameDetailId })
                }
            }

            val storiesCall = CoroutineScope(Dispatchers.IO).async {
                creditsCall.await()?.let { gcdCredits ->
                    val storyIds = gcdCredits.map { item -> item.toRoomModel().storyId }
                    if (storyIds.isNotEmpty()) {
                        Log.d(TAG, "Found stories")
                        apiService.getStories(storyIds)
                    } else {
                        Log.d(TAG, "No find stories?")
                        null
                    }
                }
            }

            val issuesCall = CoroutineScope(Dispatchers.IO).async {
                storiesCall.await()?.let { gcdStories ->
                    val issueIds = gcdStories.map { item -> item.toRoomModel().issueId }
                    if (issueIds.isNotEmpty()) {
                        apiService.getIssues(issueIds)
                    } else {
                        null
                    }
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                issuesCall.await()?.let {
                    IssueCreditUpdater().updateAll( it.map { item -> item.pk })
                }
            }

//            val variantsCall = CoroutineScope(Dispatchers.IO).async {
//                issuesCall.await()?.let {
//                    val issueIds = it.mapNotNull { item -> item.toRoomModel().variantOf }
//                    if (issueIds.isNotEmpty()) {
//                        apiService.getIssues(issueIds)
//                    } else {
//                        null
//                    }
//                }
//            }
//
//            val extractedCreditsCall = CoroutineScope(Dispatchers.IO).async {
//                nameDetailCall.await()?.let { nameDetails ->
//                    apiService.getExtractedCreditsByNameDetail(nameDetails.map {
//                        Log.d(TAG, "Refreshing extracts by name detail ${it.name}")
//                        it.nameDetailId
//                    })
//                }
//            }
//
//
//            val extractedStoriesCall = CoroutineScope(Dispatchers.IO).async {
//                extractedCreditsCall.await()?.let {
//                    val credits = it.map { item -> item.toRoomModel() }
//                    val storyIds = credits.map { credit -> credit.storyId }
//                    if (storyIds.isNotEmpty()) {
//                        Log.d(TAG, "Found extracts")
//                        apiService.getStories(storyIds)
//                    } else {
//                        Log.d(TAG, "No ex stories found")
//                        null
//                    }
//                }
//            }
//
//            val extractedIssuesCall: Deferred<List<Item<GcdIssue, Issue>>?> =
//                CoroutineScope(Dispatchers.IO).async {
//                    extractedStoriesCall.await()?.let {
//                        val issueIds = it.map { item -> item.toRoomModel().issueId }
//                        if (issueIds.isNotEmpty()) {
//                            Log.d(TAG, "Extract Issues FOUND ${issueIds.size}")
//                            apiService.getIssues(issueIds)
//                        } else {
//                            Log.d(TAG, "Extract Issues EMPTY")
//                            null
//                        }
//                    }
//                }
//
//            val extractedVariantsCall: Deferred<List<Item<GcdIssue, Issue>>?> =
//                CoroutineScope(Dispatchers.IO).async {
//                    extractedIssuesCall.await()?.let {
//                        val issueIds = it.mapNotNull { item -> item.toRoomModel().variantOf }
//                        if (issueIds.isNotEmpty()) {
//                            Log.d(TAG, "Extract Variants FOUND ${issueIds.size}")
//                            apiService.getIssues(issueIds)
//                        } else {
//                            Log.d(TAG, "Extract Variants EMPTY")
//                            null
//                        }
//                    }
//                }
//
//            CoroutineScope(Dispatchers.IO).launch {
//                val stories = storiesCall.await()?.map { it.toRoomModel() } ?: emptyList()
//                val exStories =
//                    extractedStoriesCall.await()?.map { it.toRoomModel() } ?: emptyList()
//                val variants =
//                    variantsCall.await()?.map { it.toRoomModel() } ?: emptyList() ?: emptyList()
//                val exVariants =
//                    extractedVariantsCall.await()?.map { it.toRoomModel() } ?: emptyList()
//                val issues = issuesCall.await()?.map { it.toRoomModel() } ?: emptyList()
//                val exIssues =
//                    extractedIssuesCall.await()?.map { it.toRoomModel() } ?: emptyList()
//                val credits = creditsCall.await()?.map { it.toRoomModel() } ?: emptyList()
//                val exCredits =
//                    extractedCreditsCall.await()?.map { it.toRoomModel() } ?: emptyList()
//                val nameDetails: List<NameDetail>? = nameDetailCall.await()
//                database.transactionDao().upsertSus(
//                    stories = stories + exStories,
//                    issues = variants + issues + exVariants + exIssues,
//                    nameDetails = nameDetails,
//                    credits = credits,
//                    exCredits = exCredits
//                )
//            }
        }
    }

    inner class IssueUpdater {
        internal fun update(seriesId: Int) {
            if (checkIfStale(SERIES_TAG(seriesId), ISSUE_LIFETIME))
                CoroutineScope(Dispatchers.IO).launch {
                    apiService.getIssuesBySeries(seriesId).let { issueItems ->
                        issueDao.upsertSus(issueItems.map { it.toRoomModel() })
                    }
                    saveTime(prefs, SERIES_TAG(seriesId))
                }
        }
    }

    inner class CharacterExtractor {
        suspend fun extractCharacters(stories: List<Item<GcdStory, Story>>?) {
            stories?.forEach { gcdStory ->
                val story = gcdStory.fields

                val characters = story.characters.split("; ")

                characters.forEach { character ->
                    CoroutineScope(Dispatchers.Default).launch {
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

            val character = CoroutineScope(Dispatchers.IO).async {
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

    //    inner class CreditExtractor {
//        suspend fun extractCredits(stories: List<Item<GcdStory, Story>>?) {
//            stories?.forEach { gcdStory ->
//                val story = gcdStory.fields
//
//                checkField(story.script, gcdStory.pk, Role.Companion.Name.SCRIPT.value)
//                checkField(story.pencils, gcdStory.pk, Role.Companion.Name.PENCILS.value)
//                checkField(story.inks, gcdStory.pk, Role.Companion.Name.INKS.value)
//                checkField(story.colors, gcdStory.pk, Role.Companion.Name.COLORS.value)
//                checkField(story.letters, gcdStory.pk, Role.Companion.Name.LETTERS.value)
//                checkField(story.editing, gcdStory.pk, Role.Companion.Name.EDITING.value)
//            }
//        }
//
//        private suspend fun checkField(value: String, storyId: Int, roleId: Int) {
//            if (value != "" && value != "?") {
//                value.split("; ").map { name ->
//                    var res = name.replace(Regex("\\s*\\([^)]*\\)\\s*"), "")
//                    res = res.replace(Regex("\\s*\\[[^]]*]\\s*"), "")
//                    makeCredit(res, storyId, roleId)
//                }
//            }
//        }
//
//        private suspend fun makeCredit(extracted_name: String, storyId: Int, roleId: Int) {
//
//            val checkLocalCreators = GlobalScope.async {
//                creatorDao.getCreatorByNameSus(extracted_name)
//            }
//
//            checkLocalCreators.await().let { localCreators: List<Creator>? ->
//                if (localCreators == null || localCreators.isEmpty()) {
//                    val nameDetails = GlobalScope.async {
//                        apiService.getNameDetailByName(extracted_name)
//                            .map { it.toRoomModel() }
//                    }
//
//                    nameDetails.await().let { nameDetailItems1: List<NameDetail> ->
//                        if (nameDetailItems1.isNotEmpty()) {
//                            val creatorIds = nameDetailItems1.map { it.creatorId }
//
//                            val creators = GlobalScope.async {
//                                if (creatorIds.isNotEmpty()) {
//                                    apiService.getCreator(creatorIds)
//                                        .map { it.toRoomModel() }
//                                } else {
//                                    null
//                                }
//                            }
//
//                            creators.await()?.let { it: List<Creator> ->
//                                if (it.size > 1) {
//                                    Log.d(
//                                        TAG,
//                                        "Multiple creator matches: $extracted_name ${it.size}"
//                                    )
//                                }
//                                withContext(Dispatchers.Default) {
//                                    creatorDao.upsertSus(it)
//                                }.let {
//                                    withContext(Dispatchers.Default) {
//                                        nameDetailDao.upsertSus(nameDetailItems1)
//                                    }.let {
//                                        withContext(Dispatchers.Default) {
//                                            apiService.getNameDetailsByCreatorIds(creatorIds)
//                                        }
//                                            .let { ndItems: List<Item<GcdNameDetail, NameDetail>> ->
//                                                withContext(Dispatchers.Default) {
//                                                    nameDetailDao.upsertSus(ndItems.map { it.toRoomModel() })
//                                                }.let {
//                                                    creditDao.upsertSus(
//                                                        listOf(
//                                                            Credit(
//                                                                storyId = storyId,
//                                                                nameDetailId = ndItems[0].pk,
//                                                                roleId = roleId
//                                                            )
//                                                        )
//                                                    )
//                                                }
//                                            }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                } else {
//                    GlobalScope.launch {
//                        val nameDetail = GlobalScope.async {
//                            localCreators[0].creatorId.let { id ->
//                                nameDetailDao.getNameDetailByCreatorIdSus(id)
//                            }
//                        }
//
//                        nameDetail.await().let {
//                            it?.let {
//                                creditDao.upsertSus(
//                                    listOf(
//                                        Credit(
//                                            storyId = storyId,
//                                            nameDetailId = it.nameDetailId,
//                                            roleId = roleId
//                                        )
//                                    )
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
// TODO: This should probably get moved out of SharedPreferences and stored with each record.
//  The tradeoff: an extra local db query vs. having a larger prefs which will end up having
//  a value for every item in the database.
    private fun checkIfStale(prefsKey: String, shelfLife: Long): Boolean {
        val lastUpdated = LocalDate.parse(prefs.getString(prefsKey, "${LocalDate.MIN}"))
        Log.d(TAG, "$prefsKey ${lastUpdated.plusDays(14)}")
        return DEBUG || lastUpdated.plusDays(shelfLife) < LocalDate.now()
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

    fun saveCover(vararg cover: Cover) {
        executor.execute {
            try {
                coverDao.upsert(cover.asList())
            } catch (e: SQLiteConstraintException) {
                Log.d(TAG, "addCover: $e")
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

    fun deleteCover(cover: Cover) {
        executor.execute {
            coverDao.delete(cover)
        }
    }

    fun inCollection(issueId: Int): LiveData<Count> = collectionDao.inCollection(issueId)

    fun updateIssue(issue: FullIssue?) {
        if (issue != null) {
            IssueCoverUpdater().update(issue.issue.issueId)
            IssueCreditUpdater().update(issue.issue.issueId)
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