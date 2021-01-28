package com.wtb.comiccollector.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.wtb.comiccollector.*
import java.util.*

@Dao
interface IssueDao {
    @Transaction
    @Query(
        """SELECT issue.*, series.seriesName, publisher.publisher FROM issue NATURAL JOIN series NATURAL JOIN publisher"""
    )
    fun getIssues(): LiveData<List<FullIssue>>

    @Query("""SELECT issue.*, series.seriesName, publisher.publisher FROM issue NATURAL JOIN series NATURAL JOIN publisher WHERE seriesId=:seriesId""")
    fun getIssuesBySeries(seriesId: UUID): LiveData<List<FullIssue>>

    @Query("SELECT * FROM issue WHERE issueId=:issueId")
    fun getIssue(issueId: UUID): LiveData<Issue?>

    @Query(
        """
        SELECT roleName, name FROM credit 
            INNER JOIN role ON credit.roleId = role.roleId 
            INNER JOIN creator on creator.creatorId = credit.creatorId
            WHERE credit.issueId = :issueId"""
    )
    fun getIssueCredits(issueId: UUID): LiveData<IssueCredits>

    @Query("SELECT * FROM series WHERE seriesId != '00000000-0000-0000-0000-000000000000' ORDER BY seriesName ASC")
    fun getSeriesList(): LiveData<List<Series>>

    @Query("SELECT * FROM series WHERE seriesId=:seriesId")
    fun getSeriesById(seriesId: UUID): LiveData<Series?>

    @Query("SELECT * FROM publisher ORDER BY publisher ASC")
    fun getPublishersList(): LiveData<List<Publisher>>

    @Query("SELECT issue.* FROM issue NATURAL JOIN credit WHERE creatorId=:creatorId")
    fun getIssuesByCreator(creatorId: UUID): LiveData<List<Issue>>

    @Update
    fun updateIssue(issue: Issue)

    @Insert
    fun addIssue(issue: Issue)

    @Delete
    fun deleteIssue(issue: Issue)

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
    fun updatePublisher(publisher: Publisher)

    @Insert
    fun addPublisher(publisher: Publisher)

    @Insert
    fun addPublishers(vararg publisher: Publisher)

    @Insert
    fun addRoles(vararg role: Role)

    @Delete
    fun deletePublisher(publisher: Publisher)

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