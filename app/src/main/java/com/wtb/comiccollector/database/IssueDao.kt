package com.wtb.comiccollector.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.wtb.comiccollector.*
import java.util.*

@Dao
interface IssueDao {
    @Transaction
    @Query("SELECT issue.*, seriesName FROM issue JOIN series ON issue.seriesId = series.seriesId")
    fun getIssues(): LiveData<List<FullIssue>>

    @Query("SELECT * FROM issue WHERE issueId=(:issueId)")
    fun getIssue(issueId: UUID): LiveData<Issue?>

    @Query(
        """
        SELECT roleName, name FROM credit 
            INNER JOIN role ON credit.roleId = role.roleId 
            INNER JOIN creator on creator.creatorId = credit.creatorId
            WHERE credit.issueId = :issueId"""
    )
    fun getIssueCredits(issueId: UUID): LiveData<IssueCredits>

    @Update
    fun updateIssue(issue: Issue)

    @Insert
    fun addIssue(issue: Issue)

    @Delete
    fun deleteIssue(issue: Issue)

    @Query("SELECT * FROM series ORDER BY seriesName ASC")
    fun getSeriesList(): LiveData<List<Series>>

    @Query("SELECT * FROM series WHERE seriesId=(:seriesId)")
    fun getSeriesList(seriesId: UUID): LiveData<Series?>

    @Update
    fun updateSeries(series: Series)

    @Insert
    fun addSeries(series: Series)

    @Delete
    fun deleteSeries(series: Series)

    @Update
    fun updateCreator(creator: Creator)

    @Insert
    fun addCreator(creator: Creator)

    @Delete
    fun deleteCreator(creator: Creator)

    @Update
    fun updateRole(role: Role)

    @Insert
    fun addRole(role: Role)

    @Delete
    fun deleteRole(role: Role)

    @Update
    fun updateCredit(credit: Credit)

    @Insert
    fun addCredit(credit: Credit)

    @Delete
    fun deleteCredit(credit: Credit)
}