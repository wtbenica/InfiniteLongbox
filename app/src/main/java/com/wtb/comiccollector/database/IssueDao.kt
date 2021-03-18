package com.wtb.comiccollector.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.wtb.comiccollector.*
import java.time.LocalDate

@Dao
interface IssueDao {

    @Transaction
    @Query("SELECT * FROM issue WHERE issueId = :issueId")
    fun getFullIssue(issueId: Int): LiveData<IssueAndSeries?>

    @Transaction
    @Query(
        """
            SELECT cr.*
            FROM credit cr
            JOIN story sr on cr.storyId = sr.storyId
            JOIN storytype st on st.typeId = sr.storyType
            WHERE sr.issueId = :issueId
            AND (sr.storyType = 19 OR sr.storyType= 6)
            ORDER BY st.sortCode, sr.sequenceNumber
        """
    )
    fun getIssueCredits(issueId: Int): LiveData<List<FullCredit>>

    @Query(
        """
            SELECT issue.*, series.seriesName, publisher.publisher 
            FROM issue NATURAL JOIN series NATURAL JOIN publisher 
            WHERE seriesId=:seriesId
            """
    )
    fun getIssuesBySeries(seriesId: Int): LiveData<List<FullIssue>>

    @Query(
        """
            SELECT issue.*, series.seriesName, publisher.publisher 
            FROM issue NATURAL JOIN series NATURAL JOIN publisher 
            WHERE seriesId=:seriesId and issueNum=:issueNum
            """
    )
    fun getIssueByDetails(seriesId: Int, issueNum: Int): LiveData<List<FullIssue>>

    @Query("SELECT * FROM issue WHERE issueId=:issueId")
    fun getIssue(issueId: Int): LiveData<Issue?>

    @Query("SELECT * FROM creator WHERE creatorId = :creatorId")
    fun getCreator(vararg creatorId: Int): LiveData<List<Creator>>?

    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    fun getPublisher(publisherId: Int): LiveData<Publisher?>

    @Query("SELECT * FROM series WHERE seriesId=:seriesId")
    fun getSeriesById(seriesId: Int): LiveData<Series?>

    @Query("SELECT issue.* FROM issue NATURAL JOIN credit WHERE creatorId=:creatorId")
    fun getIssuesByCreator(creatorId: Int): LiveData<List<Issue>>

    @Query("SELECT * FROM role WHERE roleName = :roleName")
    fun getRoleByName(roleName: String): Role

    @Query("SELECT * FROM series WHERE seriesId != ${DUMMY_ID} ORDER BY seriesName ASC")
    fun getAllSeries(): LiveData<List<Series>>

    @Query("SELECT * FROM publisher WHERE publisherId != ${DUMMY_ID} ORDER BY publisher ASC")
    fun getPublishersList(): LiveData<List<Publisher>>

    @Query("SELECT * FROM creator ORDER BY sortName ASC")
    fun getCreatorsList(): LiveData<List<Creator>>

    @Query("SELECT * FROM role")
    fun getRoleList(): LiveData<List<Role>>

    @Query(
        """SELECT creator.* FROM creator natural join credit natural join role where roleName = 'Writer'"""
    )
    fun getWritersList(): LiveData<List<Creator>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertIssue(vararg issue: Issue?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSeries(vararg series: Series?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCreator(vararg creator: Creator)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPublisher(vararg publisher: Publisher?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRole(vararg role: Role)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCredit(vararg credit: Credit)

    @Update
    fun updateIssue(issue: Issue)

    @Update
    fun updateSeries(vararg series: Series)

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
        creatorId: Int? = null,
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
    fun getSeriesByCreator(creatorId: Int): LiveData<List<Series>>

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
        creatorId: Int,
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
        seriesId: Int, startDate: LocalDate = LocalDate.MIN, endDate: LocalDate =
            LocalDate.MAX
    ): LiveData<List<Creator>>

    @Query(
        """
            SELECT *
            FROM creator cr
            WHERE cr.name = :creator
        """
    )
    fun getCreatorByName(creator: String): LiveData<Creator?>

    @Query(
        """
            SELECT st.*
            FROM story st
            NATURAL JOIN issue iss
            JOIN storytype type ON type.typeId = st.storyType
            WHERE iss.issueId = :issueId
            AND (st.storyType = 19 OR st.storyType= 6)
            ORDER BY type.sortCode, storyType
        """
    )
    fun getStories(issueId: Int): LiveData<List<Story>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStory(vararg story: Story)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStoryType(vararg storyType: StoryType)

    @Transaction
    fun insertCreditTransaction(
        stories: Array<out Story>,
        creators: Array<out Creator>,
        credits: Array<out Credit>
    ) {
        insertStory(*stories)
        insertCreator(*creators)
        insertCredit(*credits)
    }
}