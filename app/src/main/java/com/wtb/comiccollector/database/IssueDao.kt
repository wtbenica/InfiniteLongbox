package com.wtb.comiccollector.database

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import com.wtb.comiccollector.*
import java.time.LocalDate

/**
 * BaseDao provides generic insert, update, delete, and upsert (insert if not exist, else update)
 * I was having a problem where insert(REPLACE) is actually "try insert, if exists, delete then
 * insert," which, along with "on delete cascade" resulted in lost records
 */
@Dao
abstract class BaseDao<T: DataModel> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(obj: T): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(obj: List<T>): List<Long>

    @Update
    abstract fun update(obj: T)

    @Update
    abstract fun update(obj: List<T>)

    @Transaction
    open fun upsert(obj: T) {
        val id = insert(obj)
        if (id == -1L) update(obj)
    }

    @Transaction
    open fun upsert(objList: List<T>) {
        val insertResult = insert(objList)
        val updateList = mutableListOf<T>()

        for (i in insertResult.indices) {
            if (insertResult[i] == -1L) updateList.add(objList[i])
        }

        if (!updateList.isEmpty()) update(updateList)
    }
}


// TODO: Figure out this mess. Separate daos for each entity might be necessary. There's lots of
//  duplicate code that could be removed that way. Current type parameter is Creator because
//  that's what i needed to upsert
@Dao
abstract class IssueDao: BaseDao<Creator>() {

    @Transaction
    @Query("SELECT * FROM issue WHERE issueId = :issueId")
    abstract fun getFullIssue(issueId: Int): LiveData<IssueAndSeries?>

    @Transaction
    @Query(
        """
            SELECT cr.*
            FROM credit cr
            JOIN story sr on cr.storyId = sr.storyId
            JOIN storytype st on st.typeId = sr.storyType
            JOIN role ON cr.roleId = role.roleId
            WHERE sr.issueId = :issueId
            AND (sr.storyType = 19 OR sr.storyType= 6)
            ORDER BY st.sortCode, sr.sequenceNumber, role.sortOrder
        """
    )
    abstract fun getIssueCredits(issueId: Int): LiveData<List<FullCredit>>

    @Query(
        """
            SELECT issue.*, series.seriesName, publisher.publisher 
            FROM issue NATURAL JOIN series NATURAL JOIN publisher 
            WHERE seriesId=:seriesId
            """
    )
    abstract fun getIssuesBySeries(seriesId: Int): LiveData<List<FullIssue>>

    @Query(
        """
            SELECT issue.*, series.seriesName, publisher.publisher 
            FROM issue NATURAL JOIN series NATURAL JOIN publisher 
            WHERE seriesId=:seriesId and issueNum=:issueNum
            """
    )
    abstract fun getIssueByDetails(seriesId: Int, issueNum: Int): LiveData<List<FullIssue>>

    @Query("SELECT * FROM issue WHERE issueId=:issueId")
    abstract fun getIssue(issueId: Int): LiveData<Issue?>

    @Query("SELECT * FROM creator WHERE creatorId = :creatorId")
    abstract fun getCreator(vararg creatorId: Int): LiveData<List<Creator>>?

    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    abstract fun getPublisher(publisherId: Int): LiveData<Publisher?>

    @Query("SELECT * FROM series WHERE seriesId=:seriesId")
    abstract fun getSeriesById(seriesId: Int): LiveData<Series?>

    @Query("SELECT issue.* FROM issue NATURAL JOIN credit WHERE creatorId=:creatorId")
    abstract fun getIssuesByCreator(creatorId: Int): LiveData<List<Issue>>

    @Query("SELECT * FROM role WHERE roleName = :roleName")
    abstract fun getRoleByName(roleName: String): Role

    @Query("SELECT * FROM series WHERE seriesId != ${DUMMY_ID} ORDER BY seriesName ASC")
    abstract fun getAllSeries(): LiveData<List<Series>>

    @Query("SELECT * FROM publisher WHERE publisherId != ${DUMMY_ID} ORDER BY publisher ASC")
    abstract fun getPublishersList(): LiveData<List<Publisher>>

    @Query("SELECT * FROM creator ORDER BY sortName ASC")
    abstract fun getCreatorsList(): LiveData<List<Creator>>

    @Query("SELECT * FROM role")
    abstract fun getRoleList(): LiveData<List<Role>>

    @Query(
        """SELECT creator.* FROM creator natural join credit natural join role where roleName = 'Writer'"""
    )
    abstract fun getWritersList(): LiveData<List<Creator>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertIssue(vararg issue: Issue?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertSeries(vararg series: Series?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertCreator(vararg creator: Creator): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertPublisher(vararg publisher: Publisher?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertRole(vararg role: Role)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertCredit(vararg credit: Credit)

    @Update
    abstract fun updateIssue(issue: Issue)

    @Update
    abstract fun updateSeries(vararg series: Series)

    @Update
    abstract fun updateCreator(vararg creator: Creator)

    @Update
    abstract fun updatePublisher(publisher: Publisher)

    @Update
    abstract fun updateRole(role: Role)

    @Update
    abstract fun updateCredit(credit: Credit)

    @Delete
    abstract fun deleteIssue(issue: Issue)

    @Delete
    abstract fun deleteSeries(series: Series)

    @Delete
    abstract fun deleteCreator(creator: Creator)

    @Delete
    abstract fun deletePublisher(publisher: Publisher)

    @Delete
    abstract fun deleteRole(role: Role)

    @Delete
    abstract fun deleteCredit(credit: Credit)

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
    abstract fun getSeriesByCreator(creatorId: Int): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT series.*
        FROM series
        NATURAL JOIN issue
        NATURAL JOIN credit
        WHERE series.startDate < :endDate AND series.endDate > :startDate 
           """
    )
    abstract fun getSeriesByDates(startDate: LocalDate, endDate: LocalDate): LiveData<List<Series>>

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
    abstract fun getSeriesByCreatorAndDates(
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
    abstract fun getCreatorList(
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
    abstract fun getCreatorByName(creator: String): LiveData<Creator?>

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
    abstract fun getStories(issueId: Int): LiveData<List<Story>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertStory(vararg story: Story)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertStoryType(vararg storyType: StoryType)

    @Transaction
    open fun insertCreditTransaction(
        stories: Array<out Story>,
        creators: Array<out Creator>,
        credits: Array<out Credit>
    ) {
        insertStory(*stories)
        upsert(listOf(*creators))
        insertCredit(*credits)
    }

    @Transaction
    open fun insertCreatorCreditTransaction(
        creator: Creator,
        credit: Credit
    ) {
        insertCreator(creator)
        insertCredit(credit)

        Log.d(
            "INS:", "${credit.creditId.format(10)} ${credit.creatorId.format(10)} ${
                credit.roleId.format
                    (10)
            } ${credit.storyId.format(10)}"
        )
    }
}