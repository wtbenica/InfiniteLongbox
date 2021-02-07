package com.wtb.comiccollector

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
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
import com.wtb.comiccollector.database.migration_1_2
import java.time.LocalDate
import java.util.*
import java.util.concurrent.Executors

private const val DATABASE_NAME = "issue-database"
private const val TAG = "IssueRepository"

class IssueRepository private constructor(context: Context) {

    private val executor = Executors.newSingleThreadExecutor()

    private val database: IssueDatabase = buildDatabase(context)

    private val issueDao = database.issueDao()

    private val filesDir = context.applicationContext.filesDir

    val allSeries: LiveData<List<Series>> = issueDao.getSeriesList()

    val allPublishers: LiveData<List<Publisher>> = issueDao.getPublishersList()

    val allCreators: LiveData<List<Creator>> = issueDao.getCreatorsList()

    val allWriters: LiveData<List<Creator>> = issueDao.getWritersList()

    fun getIssues(): LiveData<List<FullIssue>> = issueDao.getIssues()

    fun getIssue(issueId: UUID): LiveData<Issue?> = issueDao.getIssue(issueId)

    fun getIssuesBySeries(seriesId: UUID) = issueDao.getIssuesBySeries(seriesId)

    fun getIssueCredits(issueId: UUID): LiveData<List<IssueCredits>> =
        issueDao.getIssueCredits(issueId)

    private fun buildDatabase(context: Context) = Room.databaseBuilder(
        context.applicationContext,
        IssueDatabase::class.java,
        DATABASE_NAME
    ).addCallback(
        object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                executor.execute {
                    val publisherDC = Publisher(publisherId = UUID.randomUUID(), publisher = "DC")
                    issueDao.insertPublisher(
                        publisherDC,
                        Publisher(publisher = "Marvel"),
                        Publisher(publisher = "Image"),
                        Publisher(publisher = "Dark Horse"),
                        Publisher(publisher = "Valiant"),
                        Publisher(publisher = "Fantagraphics"),
                        Publisher(publisher = "Aftershock"),
                        Publisher(publisher = "DC/Vertigo")
                    )
                    val writer = Role(roleName = "Writer")
                    val penciller = Role(roleName = "Penciller")
                    issueDao.insertRole(
                        writer,
                        Role(roleName = "Scripter"),
                        Role(roleName = "Plotter"),
                        penciller,
                        Role(roleName = "Artist"),
                        Role(roleName = "Inker"),
                        Role(roleName = "Colorist"),
                        Role(roleName = "Letterer"),
                        Role(roleName = "Cover Artist"),
                        Role(roleName = "Editor"),
                        Role(roleName = "Assistant Editor")
                    )
                    val grantMorrison = Creator(firstName = "Grant", lastName = "Morrison")
                    val philipBond = Creator(firstName = "Philip", lastName = "Bond")
                    val johnNyberg = Creator(firstName = "John", lastName = "Nyberg")
                    val richardCase = Creator(firstName = "Richard", lastName = "Case")
                    issueDao.insertCreator(
                        grantMorrison,
                        philipBond,
                        johnNyberg,
                        richardCase,
                        Creator(firstName = "Neil", lastName = "Gaiman"),
                        Creator(firstName = "Jason", lastName = "Aaron")
                    )
                    val seriesDoomPatrol =
                        Series(seriesName = "Doom Patrol", publisherId = publisherDC.publisherId)
                    issueDao.insertSeries(
                        Series(
                            seriesId = NEW_SERIES_ID,
                            seriesName = "New Series",
                            publisherId = publisherDC.publisherId,
                            startDate = LocalDate.of(1995, 5, 13),
                            endDate = LocalDate.of(2000, 3, 25)
                        ),
                        seriesDoomPatrol
                    )
                    val dp35 = Issue(
                        seriesId = seriesDoomPatrol.seriesId,
                        writerId = grantMorrison.creatorId,
                        pencillerId = philipBond.creatorId,
                        inkerId = johnNyberg.creatorId,
                        issueNum = 35
                    )
                    issueDao.insertIssue(
                        Issue(
                            seriesId = seriesDoomPatrol.seriesId,
                            writerId = grantMorrison.creatorId,
                            pencillerId = philipBond.creatorId,
                            inkerId = johnNyberg.creatorId,
                            issueNum = 33
                        ),
                        dp35
                    )
                    issueDao.insertCredit(
                        Credit(
                            issueId = dp35.issueId,
                            creatorId = grantMorrison.creatorId,
                            roleId = writer.roleId
                        ),
                        Credit(
                            issueId = dp35.issueId,
                            creatorId = richardCase.creatorId,
                            roleId = penciller.roleId
                        )
                    )
                }
            }
        }
    ).addMigrations(migration_1_2)
        .build()

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
            issueDao.insertCredit(credit)
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
        return issueDao.getSeriesList()
    }

    fun getSeries(seriesId: UUID): LiveData<SeriesDetail?> = issueDao.getSeriesById(seriesId)

    fun getCreator(creatorId: UUID): LiveData<Creator> {
        return issueDao.getCreator(creatorId)
    }

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
            return INSTANCE ?: throw IllegalStateException("IssueRepository must be initialized")
        }
    }
}