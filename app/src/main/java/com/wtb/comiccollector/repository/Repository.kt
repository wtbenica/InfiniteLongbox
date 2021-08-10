@file:Suppress("RemoveExplicitTypeArguments")

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
import com.wtb.comiccollector.database.IssueDatabase
import com.wtb.comiccollector.database.daos.Count
import com.wtb.comiccollector.database.daos.REQUEST_LIMIT
import com.wtb.comiccollector.database.models.*
import com.wtb.comiccollector.network.RetrofitAPIClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
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
const val BASE_URL = NIGHTWING

internal const val UPDATED_ROLES = "updated_roles"
internal const val UPDATED_STORY_TYPES = "updated_story_types"
internal const val UPDATED_PUBLISHERS = "updated_publishers"
internal const val UPDATED_BOND_TYPE = "update_bond_type"
internal const val UPDATED_BONDS = "update_series_bonds"
internal const val UPDATED_CREATORS = "updated_creators"
internal const val UPDATED_CREATORS_PAGE = "updated_creators_page"
internal const val UPDATED_SERIES = "updated_series"
internal const val UPDATED_SERIES_PAGE = "updated_series_page"
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

internal fun UPDATED_TAG(id: Int, type: String): String = "$type${id}_UPDATED"

internal fun SERIES_TAG(id: Int): String = UPDATED_TAG(id, "SERIES_")
internal fun ISSUE_TAG(id: Int) = UPDATED_TAG(id, "ISSUE_")
internal fun PUBLISHER_TAG(id: Int): String = UPDATED_TAG(id, "PUBLISHER_")
internal fun CREATOR_TAG(id: Int): String = UPDATED_TAG(id, "CREATOR_")
internal fun CHARACTER_TAG(id: Int): String = UPDATED_TAG(id, "CHARACTER_")

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

    private val webservice: Webservice by lazy {
        retrofit.create(Webservice::class.java)
    }

    private val updater: StaticUpdater by lazy {
        StaticUpdater(webservice, database, prefs)
    }

    init {
        MainActivity.hasConnection.observeForever {
            hasConnection = it
            if (checkConnectionStatus()) {
                isIdle = false

                // TODO: A lint inspection pointed out that update returns a Deferred, which
                //  means that this is async async await. Look into
                MainActivity.activeJob = CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.IO) {
                        updater.updateAsync()
                    }.let {
                        Log.d(TAG, "Static update done")
                        isIdle = true
                    }
                }
            }
        }
    }

    private fun checkConnectionStatus() = hasConnection && hasUnmeteredConnection && isIdle

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

    fun getFilterOptionsCharacter(filter: SearchFilter): Flow<List<Character>> {
        Log.d(TAG, "getCharactersByFilter")

        return if (!filter.hasCharacter()) {
            characterDao.getCharacterFilterOptions(filter)
        } else {
            emptyFlow()
        }
    }

    fun getFilterOptionsCreator(filter: SearchFilter): Flow<List<Creator>> {
        Log.d(TAG, "getCreatorsByFilter")
        return if (filter.mCreators.isEmpty()) {
            creatorDao.getCreatorsByFilter(filter)
        } else {
            emptyFlow()
        }
    }

    // SERIES METHODS
    fun getSeries(seriesId: Int): Flow<FullSeries?> = seriesDao.getSeries(seriesId)

    fun getSeriesByFilterPaged(filter: SearchFilter): Flow<PagingData<FullSeries>> {
        val newFilter = SearchFilter(filter)
        Log.d(TAG, "getSeriesByFilterPaged")

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
    fun getIssue(issueId: Int): Flow<FullIssue?> {
        if (issueId != AUTO_ID) {
            updateIssueCover(issueId)
            updater.updateIssue(issueId)
        }

        return issueDao.getFullIssue(issueId = issueId)
    }

    fun getIssuesByFilter(filter: SearchFilter): Flow<List<FullIssue>> {
        return issueDao.getIssuesByFilter(filter = filter)
    }

    fun getIssuesByFilterPaged(filter: SearchFilter): Flow<PagingData<FullIssue>> {
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
    fun getStoriesByIssue(issueId: Int): Flow<List<Story>> {

        return storyDao.getStoriesFlow(issueId)
    }

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
    fun getFilterOptionsPublisher(filter: SearchFilter): Flow<List<Publisher>> {
        Log.d(TAG, "getPublishersByFilter")
        return if (filter.mPublishers.isEmpty()) {
            publisherDao.getPublishersByFilter(filter)
        } else {
            flow { emit(emptyList<Publisher>()) }
        }
    }

    fun updateIssueCover(issueId: Int) {
        if (hasConnection) {
            UpdateIssueCover(webservice, database, prefs, context).update(issueId)
        }
    }

    fun addToCollection(issue: FullIssue) {
        executor.execute {
            collectionDao.insert(MyCollection(issue = issue.issue.issueId,
                                              series = issue.series.seriesId))
        }
    }

    fun removeFromCollection(issueId: Int) {
        executor.execute {
            collectionDao.deleteById(issueId)
        }
    }

    fun inCollection(issueId: Int): Flow<Count> = collectionDao.inCollection(issueId)

    /**
     * Builds database and adds dummy publisher and series, which are used for creating new empty
     * issue objects
     */
    @Language("RoomSql")
    private fun buildDatabase(context: Context): IssueDatabase {
        return Room.databaseBuilder(
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
                                publisher = DUMMY_ID,
                                startDate = LocalDate.MIN,
                                endDate = LocalDate.MIN,
                            )
                        )
                    }
                }
            }
        ).addMigrations(
//            migration_1_2, migration_2_3, migration_3_4, migration_4_5,
//            migration_5_6, migration_6_7, migration_7_8, migration_8_9, migration_9_10,
//            migration_10_11, migration_11_12, migration_12_13
        )
            .build()
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

        fun savePrefValue(prefs: SharedPreferences, key: String, value: Any) {
            val editor = prefs.edit()
            when (value) {
                is String  -> editor.putString(key, value)
                is Int     -> editor.putInt(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Float   -> editor.putFloat(key, value)
                is Long    -> editor.putLong(key, value)
            }
            editor.apply()
        }

        fun saveTime(prefs: SharedPreferences, key: String) {
            savePrefValue(prefs, key, LocalDate.now().toString())
        }

        /*
        I'm leaving these here as templates
         */
//        @Language("RoomSql")
//        val migration_1_2 = SimpleMigration(
//            1,
//            2,
//            """ALTER TABLE issue ADD COLUMN coverDateLong TEXT""",
//            "ALTER TABLE issue ADD COLUMN onSaleDateUncertain INTEGER NOT NULL DEFAULT 1",
//            "ALTER TABLE issue ADD COLUMN coverDate TEXT",
//            "ALTER TABLE issue ADD COLUMN notes TEXT",
//        )
//
//        @Language("RoomSql")
//        val migration_2_3 = SimpleMigration(
//            2, 3,
//            """ALTER TABLE series ADD COLUMN notes TEXT""",
//            """ALTER TABLE series ADD COLUMN issueCount INTEGER NOT NULL DEFAULT 0"""
//        )
//
//        @Language("RoomSql")
//        val migration_3_4 = SimpleMigration(
//            3, 4,
//            """ALTER TABLE namedetail ADD COLUMN sortName TEXT"""
//        )
//
//        @Language("RoomSql")
//        val migration_4_5 = SimpleMigration(
//            4, 5,
//            """ALTER TABLE creator ADD COLUMN bio TEXT"""
//        )
//
//        @Language("RoomSql")
//        val migration_5_6 = SimpleMigration(
//            5, 6,
//            """ALTER TABLE publisher ADD COLUMN yearBegan TEXT""",
//            """ALTER TABLE publisher ADD COLUMN yearBeganUncertain INTEGER NOT NULL DEFAULT 1""",
//            """ALTER TABLE publisher ADD COLUMN yearEnded TEXT""",
//            """ALTER TABLE publisher ADD COLUMN yearEndedUncertain INTEGER NOT NULL DEFAULT 1""",
//            """ALTER TABLE publisher ADD COLUMN url TEXT"""
//        )
//
//        @Language("RoomSql")
//        val migration_6_7 = SimpleMigration(
//            6, 7,
//            """CREATE TABLE 'BondType' (
//                'bondTypeId' INTEGER NOT NULL,
//                'name' TEXT NOT NULL,
//                'description' TEXT NOT NULL,
//                'notes' TEXT,
//                PRIMARY KEY('bondTypeId')
//                )""",
//            """CREATE TABLE 'SeriesBond' (
//                    'bondId' INTEGER NOT NULL,
//                    'originId' INTEGER NOT NULL,
//                    'targetId' INTEGER NOT NULL,
//                    'originIssueId' INTEGER,
//                    'targetIssueId' INTEGER,
//                    'bondTypeId' INTEGER NOT NULL,
//                    'notes' TEXT,
//                    PRIMARY KEY ('bondId'),
//                    FOREIGN KEY ('originId') REFERENCES 'Series'('seriesId') ON DELETE CASCADE,
//                    FOREIGN KEY ('targetId') REFERENCES 'Series'('seriesId') ON DELETE CASCADE,
//                    FOREIGN KEY ('originIssueId') REFERENCES 'Issue'('issueId') ON DELETE CASCADE,
//                    FOREIGN KEY ('targetIssueId') REFERENCES 'Issue'('issueId') ON DELETE CASCADE,
//                    FOREIGN KEY ('bondTypeId') REFERENCES 'BondType'('bondTypeId') ON DELETE RESTRICT
//                )""",
//            """CREATE INDEX IF NOT EXISTS 'index_SeriesBond_targetIssueId' ON 'SeriesBond'('targetIssueId')""",
//            """CREATE INDEX IF NOT EXISTS 'index_SeriesBond_originId' ON 'SeriesBond'('originId')""",
//            """CREATE INDEX IF NOT EXISTS 'index_SeriesBond_targetId' ON 'SeriesBond'('targetId')""",
//            """CREATE INDEX IF NOT EXISTS 'index_SeriesBond_originIssueId' ON 'SeriesBond'('originIssueId')""",
//            """CREATE INDEX IF NOT EXISTS 'index_SeriesBond_bondTypeId' ON 'SeriesBond'('bondTypeId')""",
//        )
//
//        @Language("RoomSql")
//        val migration_7_8 = SimpleMigration(
//            7, 8,
//            """CREATE TABLE Brand (
//                brandId INTEGER NOT NULL,
//                name TEXT NOT NULL,
//                yearBegan TEXT,
//                yearEnded TEXT,
//                notes TEXT,
//                url TEXT,
//                issueCount INTEGER NOT NULL,
//                yearBeganUncertain INTEGER NOT NULL,
//                yearEndedUncertain INTEGER NOT NULL,
//                lastUpdated TEXT NOT NULL,
//                PRIMARY KEY(brandId)
//                )
//            """,
//            """ALTER TABLE issue ADD COLUMN brandId INTEGER""",
//            """ALTER TABLE seriesbond ADD COLUMN lastUpdated TEXT NOT NULL DEFAULT '1900-01-01'""",
//            """ALTER TABLE bondtype ADD COLUMN lastUpdated TEXT NOT NULL DEFAULT '1900-01-01'"""
//        )
//
//        @Language("RoomSql")
//        val migration_8_9 = SimpleMigration(
//            8, 9,
//            """ALTER TABLE character ADD COLUMN publisher INTEGER"""
//        )
//
//        @Language("RoomSql")
//        val migration_9_10 = SimpleMigration(
//            9, 10,
//            """DROP TABLE Appearance""",
//            """DROP TABLE Character""",
//            """CREATE TABLE Character (
//                    characterId INTEGER NOT NULL,
//                    name TEXT NOT NULL,
//                    alterEgo TEXT,
//                    publisher INTEGER NOT NULL,
//                    lastUpdated TEXT NOT NULL,
//                    PRIMARY KEY (characterId),
//                    FOREIGN KEY (publisher) REFERENCES Publisher(publisherId) ON DELETE CASCADE
//                )""",
//            """CREATE INDEX IF NOT EXISTS index_Character_publisher ON Character(publisher)""",
//            """CREATE TABLE Appearance (
//                    appearanceId INTEGER NOT NULL,
//                    story INTEGER NOT NULL,
//                    character INTEGER NOT NULL,
//                    details TEXT,
//                    notes TEXT,
//                    membership TEXT,
//                    lastUpdated TEXT NOT NULL,
//                    PRIMARY KEY (appearanceId),
//                    FOREIGN KEY (story) REFERENCES Story(storyId) ON DELETE CASCADE,
//                    FOREIGN KEY (character) REFERENCES Character(characterId) ON DELETE CASCADE
//                )""",
//            """CREATE INDEX IF NOT EXISTS index_Appearance_story ON Appearance(story)""",
//            """CREATE INDEX IF NOT EXISTS index_Appearance_character ON Appearance(character)""",
//        )
//
//        @Language("RoomSql")
//        val migration_10_11 = SimpleMigration(
//            10, 11,
//            """CREATE INDEX IF NOT EXISTS index_Character_name ON Character(name)""",
//            """CREATE INDEX IF NOT EXISTS index_Character_alterEgo ON Character(alterEgo)""",
//            """CREATE INDEX IF NOT EXISTS index_Creator_name ON Creator(name)""",
//            """CREATE INDEX IF NOT EXISTS index_Creator_alterEgo ON Creator(sortName)""",
//            """CREATE INDEX IF NOT EXISTS index_NameDetail_name ON NameDetail(name)""",
//            """CREATE INDEX IF NOT EXISTS index_NameDetail_sortName ON NameDetail(sortName)""",
//        )
//
//        val migration_11_12 = SimpleMigration(
//            11, 12,
//            """ALTER TABLE credit
//                ADD COLUMN issue INTEGER REFERENCES Issue(issueId) ON DELETE CASCADE
//                """,
//            """ALTER TABLE credit
//                ADD COLUMN series INTEGER REFERENCES Series(seriesId) ON DELETE CASCADE
//            """,
//            """ALTER TABLE excredit
//                ADD COLUMN issue INTEGER REFERENCES Issue(issueId) ON DELETE CASCADE
//            """,
//            """ALTER TABLE excredit
//                ADD COLUMN series INTEGER REFERENCES Series(seriesId) ON DELETE CASCADE
//            """,
//            """ALTER TABLE appearance
//                ADD COLUMN issue INTEGER REFERENCES Issue(issueId) ON DELETE CASCADE
//            """,
//            """ALTER TABLE appearance
//                ADD COLUMN series INTEGER REFERENCES Series(seriesId) ON DELETE CASCADE
//            """,
//            """CREATE INDEX IF NOT EXISTS index_Credit_series ON Credit(series)""",
//            """CREATE INDEX IF NOT EXISTS index_Credit_issue ON Credit(issue)""",
//            """CREATE INDEX IF NOT EXISTS index_ExCredit_series ON ExCredit(series)""",
//            """CREATE INDEX IF NOT EXISTS index_ExCredit_issue ON ExCredit(issue)""",
//            """CREATE INDEX IF NOT EXISTS index_Appearance_series ON Appearance(series)""",
//            """CREATE INDEX IF NOT EXISTS index_Appearance_issue ON Appearance(issue)""",
//        )
//
//        val migration_12_13 = SimpleMigration(
//            12, 13,
//            """ALTER TABLE mycollection
//                ADD FOREIGN KEY (series) REFERENCES Series(seriesId) ON DELETE CASCADE"""
//        )
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

    fun updateCharacter(characterId: Int) = updater.updateCharacter(characterId)
    fun updateSeries(seriesId: Int) = updater.updateSeries(seriesId)
    fun updateCreators(creatorIds: List<Int>) = updater.updateCreators(creatorIds)

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