package com.wtb.comiccollector.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.wtb.comiccollector.Issue
import java.util.*

@Dao
interface IssueDao {
    @Query("SELECT * FROM issue")
    fun getIssues(): LiveData<List<Issue>>

    @Query("SELECT * FROM issue WHERE issueId=(:issueId)")
    fun getIssue(issueId: UUID): LiveData<Issue?>

    @Update
    fun updateIssue(issue: Issue)

    @Insert
    fun addIssue(issue: Issue)

    @Delete
    fun deleteIssue(issue: Issue)
}