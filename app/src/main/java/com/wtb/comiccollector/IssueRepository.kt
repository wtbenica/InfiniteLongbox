package com.wtb.comiccollector

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.comiccollector.database.IssueDatabase
import java.io.File
import java.util.*
import java.util.concurrent.Executors

private const val DATABASE_NAME = "issue-database"

class IssueRepository private constructor(context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private val database: IssueDatabase = Room.databaseBuilder(
        context.applicationContext,
        IssueDatabase::class.java,
        DATABASE_NAME
    ).addCallback(object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            executor.execute {
                issueDao.addSeries(Series(seriesId = NEW_SERIES_ID, publisherId = NEW_SERIES_ID))
                issueDao.addPublishers(
                    Publisher(publisher = "DC"),
                    Publisher(publisher = "Marvel"),
                    Publisher(publisher = "Image"),
                    Publisher(publisher = "Dark Horse"),
                    Publisher(publisher = "Valiant"),
                    Publisher(publisher = "Fantagraphics"),
                    Publisher(publisher = "Aftershock"),
                    Publisher(publisher = "DC/Vertigo")
                )
            }
        }
    }).build()

    private val issueDao = database.issueDao()

    private val filesDir = context.applicationContext.filesDir
    val allSeries: LiveData<List<Series>> = issueDao.getSeriesList()
    val allPublishers: LiveData<List<Publisher>> = issueDao.getPublishersList()

    val newSeries: LiveData<Series?> = issueDao.getSeriesById(UUID(0, 0))

    fun getIssues(): LiveData<List<FullIssue>> = issueDao.getIssues()

    fun getIssue(issueId: UUID): LiveData<Issue?> = issueDao.getIssue(issueId)

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

//    fun getNewSeries(): LiveData<Series?> = issueDao.getSeriesById(UUID(0, 0))

    fun getCoverImage(issue: Issue): File = File(filesDir, issue.coverFileName)

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