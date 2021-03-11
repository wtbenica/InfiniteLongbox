package com.wtb.comiccollector.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.wtb.comiccollector.*
import java.time.LocalDate
import java.util.*

private val NEW_SERIES_UUID = UUID(0, 0)

@Dao
interface IssueDao {

    @Transaction
    @Query("SELECT * FROM issue WHERE issueId = :issueId")
    fun getNewFullIssue(issueId: UUID): LiveData<IssueAndSeries?>

    @Transaction
    @Query(
        """
            SELECT *
            FROM credit
                NATURAL JOIN creator
                NATURAL JOIN role
            WHERE issueId = :issueId
            ORDER BY sortOrder
        """
    )
    fun getNewIssueCredits(issueId: UUID): LiveData<List<FullCredit>>

    @Transaction
    @Query(
        """
            SELECT issue.*, series.seriesName, publisher.publisher 
            FROM issue NATURAL JOIN series NATURAL JOIN publisher
         """
    )
    fun getIssues(): LiveData<List<FullIssue>>

    @Query(
        """
            SELECT issue.*, series.seriesName, publisher.publisher 
            FROM issue NATURAL JOIN series NATURAL JOIN publisher 
            WHERE seriesId=:seriesId
            """
    )
    fun getIssuesBySeries(seriesId: UUID): LiveData<List<FullIssue>>

    @Query(
        """
            SELECT issue.*, series.seriesName, publisher.publisher 
            FROM issue NATURAL JOIN series NATURAL JOIN publisher 
            WHERE seriesId=:seriesId and issueNum=:issueNum
            """
    )
    fun getIssueByDetails(seriesId: UUID, issueNum: Int): LiveData<List<FullIssue>>

    @Query("SELECT * FROM issue WHERE issueId=:issueId")
    fun getIssue(issueId: UUID): LiveData<Issue?>

    @Query("SELECT * FROM creator WHERE creatorId = :creatorId")
    fun getCreator(creatorId: UUID): LiveData<Creator?>

    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    fun getPublisher(publisherId: UUID): LiveData<Publisher?>

    @Query("SELECT * FROM series WHERE seriesId=:seriesId")
    fun getSeriesById(seriesId: UUID): LiveData<Series?>

    @Query("SELECT issue.* FROM issue NATURAL JOIN credit WHERE creatorId=:creatorId")
    fun getIssuesByCreator(creatorId: UUID): LiveData<List<Issue>>

    @Query("SELECT * FROM role WHERE roleName = :roleName")
    fun getRoleByName(roleName: String): Role

    @Query("SELECT * FROM series WHERE seriesId != '00000000-0000-0000-0000-000000000000' ORDER BY seriesName ASC")
    fun getAllSeries(): LiveData<List<Series>>

    @Query("SELECT * FROM publisher WHERE publisherId != '00000000-0000-0000-0000-000000000000' ORDER BY publisher ASC")
    fun getPublishersList(): LiveData<List<Publisher>>

    @Query("SELECT * FROM creator ORDER BY lastName ASC")
    fun getCreatorsList(): LiveData<List<Creator>>

    @Query("SELECT * FROM role")
    fun getRoleList(): LiveData<List<Role>>

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

    fun getSeriesList(
        creatorId: UUID? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): LiveData<List<Series>> {
        return if (creatorId == null) {
            if (startDate == null && endDate == null) {
                getAllSeries()
            } else {
                getSeriesByDates(startDate ?: LocalDate.MIN, endDate ?: LocalDate.MAX)
            }
        } else {
            if (startDate == null && endDate == null) {
                getSeriesByCreator(creatorId)
            } else {
                getSeriesByCreatorAndDates(
                    creatorId,
                    startDate ?: LocalDate.MIN,
                    endDate ?: LocalDate.MAX
                )
            }
        }
    }

    @Query(
        """
        SELECT DISTINCT series.*
        FROM series
        NATURAL JOIN issue
        NATURAL JOIN credit
        WHERE creatorId = :creatorId
           """
    )
    fun getSeriesByCreator(creatorId: UUID): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT series.*
        FROM series
        NATURAL JOIN issue
        NATURAL JOIN credit
        WHERE series.startDate < :endDate AND series.endDate > :startDate 
           """
    )
    fun getSeriesByDates(startDate: LocalDate, endDate: LocalDate): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT series.*
        FROM series
        NATURAL JOIN issue
        NATURAL JOIN credit
        WHERE creatorId = :creatorId
        AND series.startDate < :endDate AND series.endDate > :startDate 
           """
    )
    fun getSeriesByCreatorAndDates(
        creatorId: UUID,
        startDate: LocalDate = LocalDate.MIN,
        endDate: LocalDate = LocalDate.MAX
    ): LiveData<List<Series>>

    @Query(
        """
            SELECT DISTINCT creator.*
            FROM creator
            NATURAL JOIN issue
            NATURAL JOIN series
            NATURAL JOIN credit
            WHERE seriesId = :seriesId
            AND issue.releaseDate < :endDate AND issue.releaseDate > :startDate
        """
    )
    fun getCreatorList(
        seriesId: UUID, startDate: LocalDate = LocalDate.MIN, endDate: LocalDate =
            LocalDate.MAX
    ): LiveData<List<Creator>>
}