package com.wtb.comiccollector

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.comiccollector.database.IssueDatabase
import java.time.LocalDate
import java.util.*
import java.util.concurrent.Executors

private const val DATABASE_NAME = "issue-database"

class IssueRepository private constructor(context: Context) {

    private val executor = Executors.newSingleThreadExecutor()

    private val database: IssueDatabase = buildDatabase(context)

    private val issueDao = database.issueDao()

    private val filesDir = context.applicationContext.filesDir

    val allSeries: LiveData<List<Series>> = issueDao.getSeriesList()

    val allPublishers: LiveData<List<Publisher>> = issueDao.getPublishersList()

    val allCreators: LiveData<List<Creator>> = issueDao.getCreatorsList()

    val allWriters: LiveData<List<Creator>> = issueDao.getWritersList()

    val newSeries: LiveData<Series?> = issueDao.getSeriesById(UUID(0, 0))

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
                    issueDao.addPublishers(
                        publisherDC,
                        Publisher(publisher = "Marvel"),
                        Publisher(publisher = "Image"),
                        Publisher(publisher = "Dark Horse"),
                        Publisher(publisher = "Valiant"),
                        Publisher(publisher = "Fantagraphics"),
                        Publisher(publisher = "Aftershock"),
                        Publisher(publisher = "DC/Vertigo")
                    )
                    issueDao.addRoles(
                        Role(roleName = "Writer"),
                        Role(roleName = "Scripter"),
                        Role(roleName = "Plotter"),
                        Role(roleName = "Penciller"),
                        Role(roleName = "Artist"),
                        Role(roleName = "Inker"),
                        Role(roleName = "Colorist"),
                        Role(roleName = "Letterer"),
                        Role(roleName = "Cover Artist"),
                        Role(roleName = "Editor"),
                        Role(roleName = "Assistant Editor")
                    )
                    val grantMorrison = Creator(firstName = "Grant", lastName = "Morrison")
                    issueDao.addCreator(
                        grantMorrison,
                        Creator(firstName = "Neil", lastName = "Gaiman"),
                        Creator(firstName = "Jason", lastName = "Aaron")
                    )
                    val seriesDoomPatrol = Series(seriesName = "Doom Patrol", publisherId = publisherDC.publisherId)
                    issueDao.addSeries(
                        Series(
                            seriesId = NEW_SERIES_ID,
                            seriesName = "New Series",
                            publisherId = publisherDC.publisherId,
                            startDate = LocalDate.of(1995, 5, 13),
                            endDate = LocalDate.of(2000, 3, 25)
                        ),
                        seriesDoomPatrol
                    )
                    issueDao.addIssue(
                        Issue(seriesId = seriesDoomPatrol.seriesId, writerId = grantMorrison.creatorId)
                    )
                }
            }
        }
    )
        .build()

    fun updateIssue(issue: Issue) {
        executor.execute {
            issueDao.updateIssue(issue)
        }
    }

    fun updateSeries(series: Series) {
        executor.execute {
            issueDao.updateSeries(series)
        }
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

    fun updateCredit(credit: Credit) {
        executor.execute {
            issueDao.updateCredit(credit)
        }
    }

    fun addIssue(issue: Issue) {
        executor.execute {
            issueDao.addIssue(issue)
        }
    }

    fun addSeries(series: Series) {
        executor.execute {
            issueDao.addSeries(series)
        }
    }

    fun addCreator(creator: Creator) {
        executor.execute {
            issueDao.addCreator(creator)
        }
    }

    fun addRole(role: Role) {
        executor.execute {
            issueDao.addRole(role)
        }
    }

    fun addCredit(credit: Credit) {
        executor.execute {
            issueDao.addCredit(credit)
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

    fun getSeriesList(): LiveData<List<Series>> {
        return issueDao.getSeriesList()
    }

    fun getSeries(seriesId: UUID): LiveData<Series?> = issueDao.getSeriesById(seriesId)

    fun getCreator(creatorId: UUID): LiveData<Creator> {
        return issueDao.getCreator(creatorId)
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