package com.wtb.comiccollector.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.wtb.comiccollector.FullIssue
import com.wtb.comiccollector.Issue
import com.wtb.comiccollector.Series
import java.util.*

@Dao
interface IssueDao {
    @Query("SELECT * FROM issue NATURAL JOIN series")
    fun getIssues(): LiveData<List<FullIssue>>

    @Query("SELECT * FROM issue WHERE issueId=(:issueId)")
    fun getIssue(issueId: UUID): LiveData<Issue?>

    @Update
    fun updateIssue(issue: Issue)

    @Insert
    fun addIssue(issue: Issue)

    @Delete
    fun deleteIssue(issue: Issue)

    @Query("SELECT * FROM series")
    fun getSeriesList(): LiveData<List<Series>>

    @Query("SELECT * FROM series WHERE seriesId=(:seriesId)")
    fun getSeries(seriesId: UUID): LiveData<Series?>

    @Update
    fun updateSeries(series: Series)

    @Insert
    fun addSeries(series: Series)

    @Delete
    fun deleteSeries(series: Series)
}