package com.wtb.comiccollector.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.wtb.comiccollector.*
import java.util.*

private val NEW_SERIES_UUID = UUID(0, 0)

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

    @Query("SELECT * FROM creator WHERE creatorId = :creatorId")
    fun getCreator(creatorId: UUID): LiveData<Creator?>

    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    fun getPublisher(publisherId: UUID): LiveData<Publisher?>

    @Query(
        """
        SELECT roleName, name FROM credit 
            INNER JOIN role ON credit.roleId = role.roleId 
            INNER JOIN creator on creator.creatorId = credit.creatorId
            WHERE credit.issueId = :issueId"""
    )
    fun getIssueCredits(issueId: UUID): LiveData<List<IssueCredits>>

    @Query("SELECT * FROM series WHERE seriesId=:seriesId")
    fun getSeriesById(seriesId: UUID): LiveData<Series?>

    @Query("SELECT issue.* FROM issue NATURAL JOIN credit WHERE creatorId=:creatorId")
    fun getIssuesByCreator(creatorId: UUID): LiveData<List<Issue>>

    @Query("SELECT * FROM series WHERE seriesId != '00000000-0000-0000-0000-000000000000' ORDER BY seriesName ASC")
    fun getSeriesList(): LiveData<List<Series>>

    @Query("SELECT * FROM publisher WHERE publisherId != '00000000-0000-0000-0000-000000000000' ORDER BY publisher ASC")
    fun getPublishersList(): LiveData<List<Publisher>>

    @Query("SELECT * FROM creator ORDER BY lastName ASC")
    fun getCreatorsList(): LiveData<List<Creator>>

    @Query(
        """SELECT creator.* FROM creator natural join credit natural join role where roleName = 'Writer'"""
    )
    fun getWritersList(): LiveData<List<Creator>>

    @Insert
    fun insertIssue(vararg issue: Issue)

    @Insert
    fun insertSeries(vararg series: Series)

    @Insert
    fun insertCreator(vararg creator: Creator)

    @Insert
    fun insertPublisher(vararg publisher: Publisher)

    @Insert
    fun insertRole(vararg role: Role)

    @Insert
    fun insertCredit(vararg credit: Credit)

    @Update
    fun updateIssue(issue: Issue)

    @Update
    fun updateSeries(series: Series)

    @Update
    fun updateCreator(creator: Creator)

    @Update
    fun updatePublisher(publisher: Publisher)

    @Update
    fun updateRole(role: Role)

    @Update
    fun updateCredit(credit: Credit)

    @Delete
    fun deleteIssue(issue: Issue)

    @Delete
    fun deleteSeries(series: Series)

    @Delete
    fun deleteCreator(creator: Creator)

    @Delete
    fun deletePublisher(publisher: Publisher)

    @Delete
    fun deleteRole(role: Role)

    @Delete
    fun deleteCredit(credit: Credit)

}