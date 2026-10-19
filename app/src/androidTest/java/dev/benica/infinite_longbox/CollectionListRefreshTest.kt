package dev.benica.infinite_longbox

import android.content.Context
import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.benica.infinite_longbox.database.IssueDatabase
import dev.benica.infinite_longbox.database.models.CollectionItem
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.database.models.Issue
import dev.benica.infinite_longbox.database.models.Publisher
import dev.benica.infinite_longbox.database.models.Series
import dev.benica.infinite_longbox.database.models.UserCollection
import dev.benica.infinite_longbox.repository.importSeedFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The My Collection series list is a paged query over the database. It has to show what is added
 * to the collection while it is on screen, with no navigating away and back. This runs on a
 * throwaway in-memory database, never the app's real one.
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class CollectionListRefreshTest {
    private lateinit var db: IssueDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var differ: AsyncPagingDataDiffer<FullSeries>
    private var collecting: Job? = null

    private val noUpdates = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {}
        override fun onRemoved(position: Int, count: Int) {}
        override fun onMoved(fromPosition: Int, toPosition: Int) {}
        override fun onChanged(position: Int, count: Int, payload: Any?) {}
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, IssueDatabase::class.java).build()
        runBlocking {
            db.publisherDao().upsertSus(listOf(Publisher(publisherId = 1, publisher = "DC")))
            db.userCollectionDao().upsertSus(
                listOf(UserCollection(1, "My Collection", true), UserCollection(2, "Wish List", true))
            )
            db.seriesDao().upsertSus(
                listOf(Series(seriesId = 10, seriesName = "Doom Patrol", publisher = 1, firstIssue = 100))
            )
        }
        scope = CoroutineScope(Dispatchers.Main)
        differ = AsyncPagingDataDiffer(
            diffCallback = object : DiffUtil.ItemCallback<FullSeries>() {
                override fun areItemsTheSame(a: FullSeries, b: FullSeries) = a.series.seriesId == b.series.seriesId
                override fun areContentsTheSame(a: FullSeries, b: FullSeries) = a == b
            },
            updateCallback = noUpdates,
            mainDispatcher = Dispatchers.Main,
            workerDispatcher = Dispatchers.Default,
        )
        // As the list screen does: the pager's pages, newest wins, into the adapter
        val filter = SearchFilter(myCollection = true)
        collecting = scope.launch {
            Pager(PagingConfig(pageSize = 30, enablePlaceholders = true)) {
                db.seriesDao().getSeriesByFilterPagingSource(SearchFilter(filter))
            }.flow.collectLatest { differ.submitData(it) }
        }
    }

    @After
    fun tearDown() {
        collecting?.cancel()
        db.close()
    }

    private fun shown(): Int = runBlocking(Dispatchers.Main) { differ.itemCount }

    /** True if the list shows [expected] rows within [seconds], polling. */
    private fun showsWithin(expected: Int, seconds: Int): Boolean = runBlocking {
        withTimeoutOrNull(seconds * 1000L) {
            while (shown() != expected) delay(100)
            true
        } ?: false
    }

    @Test
    fun anEmptyCollectionListShowsNothing() {
        assertEquals(true, showsWithin(0, 3))
    }

    @Test
    fun aCollectionImportedWhileTheListIsOnScreenAppearsWithoutLeavingIt() {
        assertEquals(true, showsWithin(0, 3))

        // What an import does: the issues first, then the collection items
        runBlocking {
            db.issueDao().upsertSus(
                listOf(Issue(issueId = 100, series = 10, issueNumRaw = "1"), Issue(issueId = 101, series = 10, issueNumRaw = "2"))
            )
            db.collectionItemDao().insertSus(CollectionItem(issue = 100, series = 10, userCollection = 1))
            db.collectionItemDao().insertSus(CollectionItem(issue = 101, series = 10, userCollection = 1))
        }

        assertEquals("the series should have appeared", true, showsWithin(1, 5))
    }

    @Test
    fun anItemAddedLaterStillAppears() {
        runBlocking {
            db.issueDao().upsertSus(listOf(Issue(issueId = 100, series = 10, issueNumRaw = "1")))
        }
        assertEquals(true, showsWithin(0, 3))
        runBlocking { db.collectionItemDao().insertSus(CollectionItem(issue = 100, series = 10, userCollection = 1)) }
        assertEquals(true, showsWithin(1, 5))
    }

    /**
     * The list screen's pipeline as the view model builds it (filter -> pager -> live data ->
     * flow -> cachedIn), collected only while the screen is started, as the fragment does.
     */
    private fun screenPipeline(database: IssueDatabase = db): kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<FullSeries>> {
        val filter = MutableLiveData(SearchFilter(myCollection = true))
        return runBlocking(Dispatchers.Main) {
            filter.switchMap {
                Pager(PagingConfig(pageSize = 30, enablePlaceholders = true)) {
                    database.seriesDao().getSeriesByFilterPagingSource(SearchFilter(it))
                }.flow.asLiveData()
            }.asFlow().cachedIn(scope)
        }
    }

    @Test
    fun theViewModelPipelineRefreshesWhileTheScreenIsOnScreen() {
        collecting?.cancel()
        val itemList = screenPipeline()
        collecting = scope.launch { itemList.collectLatest { differ.submitData(it) } }
        assertEquals(true, showsWithin(0, 3))

        runBlocking {
            db.issueDao().upsertSus(listOf(Issue(issueId = 100, series = 10, issueNumRaw = "1")))
            db.collectionItemDao().insertSus(CollectionItem(issue = 100, series = 10, userCollection = 1))
        }
        assertEquals("the series should have appeared", true, showsWithin(1, 5))
    }

    @Test
    fun anImportWhileTheScreenIsStoppedShowsWhenItStartsAgain() {
        collecting?.cancel()
        val itemList = screenPipeline()
        collecting = scope.launch { itemList.collectLatest { differ.submitData(it) } }
        assertEquals(true, showsWithin(0, 3))

        // the file picker covers the screen: it stops collecting
        collecting?.cancel()
        runBlocking {
            db.issueDao().upsertSus(listOf(Issue(issueId = 100, series = 10, issueNumRaw = "1")))
            db.collectionItemDao().insertSus(CollectionItem(issue = 100, series = 10, userCollection = 1))
            delay(1500)
        }
        // the picker closes: it starts collecting again
        collecting = scope.launch { itemList.collectLatest { differ.submitData(it) } }
        assertEquals("the series should have appeared", true, showsWithin(1, 5))
    }

    @Test
    fun theListStillRefreshesAfterTheStarterDatabaseIsInstalledAndAnImportFollows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        collecting?.cancel()

        context.deleteDatabase("refresh-test.db")
        context.deleteDatabase("refresh-seed.db")

        // the seed: a database with the series in it
        val seedDb = Room.databaseBuilder(context, IssueDatabase::class.java, "refresh-seed.db").build()
        runBlocking {
            seedDb.publisherDao().upsertSus(listOf(Publisher(publisherId = 1, publisher = "DC")))
            seedDb.seriesDao().upsertSus(
                listOf(Series(seriesId = 10, seriesName = "Doom Patrol", publisher = 1, firstIssue = 100))
            )
        }
        seedDb.close()

        // the app's database, fresh, with the list already on screen
        val fileDb = Room.databaseBuilder(context, IssueDatabase::class.java, "refresh-test.db").build()
        var appeared = false
        try {
            runBlocking {
                fileDb.userCollectionDao().upsertSus(
                    listOf(UserCollection(1, "My Collection", true), UserCollection(2, "Wish List", true))
                )
            }
            val itemList = screenPipeline(fileDb)
            collecting = scope.launch { itemList.collectLatest { differ.submitData(it) } }
            assertEquals(true, showsWithin(0, 3))

            // the real installer: the series arrive, the list on screen shows them
            importSeedFile(fileDb, context.getDatabasePath("refresh-seed.db"))
            assertEquals("the seeded series is not in a collection, so not listed", true, showsWithin(0, 3))

            // and then an import, which goes through Room
            runBlocking {
                fileDb.issueDao().upsertSus(listOf(Issue(issueId = 100, series = 10, issueNumRaw = "1")))
                fileDb.collectionItemDao().insertSus(CollectionItem(issue = 100, series = 10, userCollection = 1))
            }
            appeared = showsWithin(1, 5)
        } finally {
            collecting?.cancel()
            runBlocking { delay(300) }
            fileDb.close()
            context.deleteDatabase("refresh-test.db")
            context.deleteDatabase("refresh-seed.db")
        }
        assertEquals("the series should have appeared once its issue was in the collection", true, appeared)
    }

    /**
     * A series card whose first issue has no cover falls back to the earliest cover of its other
     * issues. When that cover arrives the row must change, or the card is never redrawn.
     */
    @Test
    fun aCoverThatArrivesForAnotherIssueOfTheSeriesChangesTheRowSoTheCardIsRedrawn() {
        runBlocking {
            db.issueDao().upsertSus(
                listOf(
                    Issue(issueId = 100, series = 10, sortCode = 1, issueNumRaw = "1"),
                    Issue(issueId = 101, series = 10, sortCode = 2, issueNumRaw = "2"),
                    Issue(issueId = 102, series = 10, sortCode = 3, issueNumRaw = "3"),
                )
            )
            db.collectionItemDao().insertSus(CollectionItem(issue = 101, series = 10, userCollection = 1))
            // the first issue was looked for and has no cover
            db.coverDao().setCover(dev.benica.infinite_longbox.database.models.Cover(issue = 100, coverUri = null))
        }
        assertEquals(true, showsWithin(1, 5))
        assertEquals(null, runBlocking(Dispatchers.Main) { differ.getItem(0)?.earliestCoverUri })

        runBlocking {
            db.coverDao().setCover(
                dev.benica.infinite_longbox.database.models.Cover(
                    issue = 102, coverUri = android.net.Uri.parse("/images/COVER_102.jpg")
                )
            )
        }
        assertEquals("the later issue's cover shows", true, coverShowsWithin("/images/COVER_102.jpg", 5))

        // an earlier issue's cover wins once it is there
        runBlocking {
            db.coverDao().setCover(
                dev.benica.infinite_longbox.database.models.Cover(
                    issue = 101, coverUri = android.net.Uri.parse("/images/COVER_101.jpg")
                )
            )
        }
        assertEquals("the earlier issue's cover replaces it", true, coverShowsWithin("/images/COVER_101.jpg", 5))
    }

    private fun coverShowsWithin(path: String, seconds: Int): Boolean = runBlocking {
        withTimeoutOrNull(seconds * 1000L) {
            while (runBlocking(Dispatchers.Main) { differ.getItem(0)?.earliestCoverUri?.path } != path) delay(100)
            true
        } ?: false
    }
}
