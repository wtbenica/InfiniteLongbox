package com.wtb.comiccollector

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.wtb.comiccollector.database.IssueDatabase
import java.io.File
import java.util.*
import java.util.concurrent.Executors

private const val DATABASE_NAME = "issue-database"

class IssueRepository private constructor(context: Context) {

    private val database: IssueDatabase = Room.databaseBuilder(
        context.applicationContext,
        IssueDatabase::class.java,
        DATABASE_NAME
    )
        .build()

    private val issueDao = database.issueDao()
    private val executor = Executors.newSingleThreadExecutor()
    private val filesDir = context.applicationContext.filesDir

    fun getIssues(): LiveData<List<Issue>> = issueDao.getIssues()

    fun getIssue(issueId: UUID): LiveData<Issue?> = issueDao.getIssue(issueId)

    fun updateIssue(issue: Issue) {
        executor.execute {
            issueDao.updateIssue(issue)
        }
    }

    fun addIssue(issue: Issue) {
        executor.execute {
            issueDao.addIssue(issue)
        }
    }

    fun deleteIssue(issue: Issue) {
        executor.execute {
            issueDao.deleteIssue(issue)
        }
    }

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