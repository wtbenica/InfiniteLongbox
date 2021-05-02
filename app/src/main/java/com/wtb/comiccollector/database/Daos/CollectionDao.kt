package com.wtb.comiccollector.database.Daos

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.MyCollection
import com.wtb.comiccollector.database.models.Series
import java.time.LocalDate

private const val TAG = APP + "CollectionDao"

@Dao
abstract class CollectionDao : BaseDao<MyCollection>() {

    @Query(
        """
            SELECT DISTINCT ss.* 
            FROM series ss
            JOIN issue ie ON ie.seriesId = ss.seriesId
            JOIN mycollection mc ON mc.issueId = ie.issueId
            ORDER BY ss.sortName ASC
            """
    )
    abstract fun getAllSeries(): LiveData<List<Series>>

    fun getSeriesByFilter(filter: Filter): LiveData<List<Series>> {
        Log.d(TAG, "getSeriesByFilter")
        return if (filter.hasCreator()) {
            if (filter.hasPublisher()) {
                if (filter.hasDateFilter()) {
                    getSeriesByCreatorPublisherDates(
                        filter.mCreators.map { it.creatorId }.toMutableSet(),
                        filter.mPublishers.map { it.publisherId }.toMutableSet(),
                        filter.mStartDate,
                        filter.mEndDate
                    )
                } else {
                    getSeriesByCreatorPublisher(filter.mCreators.map { it.creatorId }
                        .toMutableSet(), filter
                        .mPublishers.map { it.publisherId }.toMutableSet()
                    )
                }
            } else {
                if (filter.hasDateFilter()) {
                    getSeriesByCreatorDates(
                        filter.mCreators.map { it.creatorId }.toMutableSet(),
                        filter.mStartDate,
                        filter.mEndDate
                    )
                } else {
                    getSeriesByCreator(filter.mCreators.map { it.creatorId }.toMutableSet())
                }
            }
        } else {
            if (filter.hasPublisher()) {
                if (filter.hasDateFilter()) {
                    getSeriesByPublisherDate(
                        filter.mPublishers.map { it.publisherId }.toMutableSet(), filter
                            .mStartDate,
                        filter
                            .mEndDate
                    )
                } else {
                    getSeriesByPublisher(filter.mPublishers.map { it.publisherId }.toMutableSet())
                }
            } else {
                if (filter.hasDateFilter()) {
                    getSeriesByDates(filter.mStartDate, filter.mEndDate)
                } else {
                    getAllSeries()
                }
            }
        }
    }

    @Query(
        """
        SELECT DISTINCT ss.*
        FROM series ss
        JOIN issue ie ON ie.seriesId = ss.seriesId
        JOIN mycollection mc ON mc.issueId = ie.issueId
        JOIN story sy ON sy.issueId = ie.issueId
        JOIN credit ct ON ct.storyId = sy.storyId
        JOIN namedetail nl ON nl.nameDetailId = ct.nameDetailId
        WHERE nl.creatorId IN (:creatorIds)
        AND ss.publisherId IN (:publishers)
        AND ss.startDate < :endDate AND ss.endDate > :startDate 
        ORDER BY startDate ASC, sortName ASC
           """
    )
    abstract fun getSeriesByCreatorPublisherDates(
        creatorIds: MutableSet<Int>,
        publishers: MutableSet<Int>,
        startDate: LocalDate,
        endDate: LocalDate
    ): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT ss.*
        FROM series ss
        JOIN issue ie ON ie.seriesId = ss.seriesId
        JOIN mycollection mc ON mc.issueId = ie.issueId
        JOIN story sy ON sy.issueId = ie.issueId
        JOIN credit ct ON ct.storyId = sy.storyId
        JOIN namedetail nl ON nl.nameDetailId = ct.nameDetailId
        WHERE nl.creatorId IN (:creatorIds)
        AND ss.publisherId IN (:publishers)
        ORDER BY sortName ASC
           """
    )
    abstract fun getSeriesByCreatorPublisher(
        creatorIds: MutableSet<Int>,
        publishers: MutableSet<Int>
    ): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT ss.*
        FROM series ss
        JOIN issue ie ON ie.seriesId = ss.seriesId
        JOIN mycollection mc ON mc.issueId = ie.issueId
        JOIN story sy ON sy.issueId = ie.issueId
        JOIN credit ct ON ct.storyId = sy.storyId
        JOIN namedetail nl ON nl.nameDetailId = ct.nameDetailId
        WHERE nl.creatorId IN (:creatorIds)
        AND ss.startDate < :endDate AND ss.endDate > :startDate 
        ORDER BY ss.startDate
           """
    )
    abstract fun getSeriesByCreatorDates(
        creatorIds: MutableSet<Int>,
        startDate: LocalDate,
        endDate: LocalDate
    ): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT ss.*
        FROM series ss
        JOIN issue ie on ie.seriesId = ss.seriesId 
        JOIN mycollection mc ON mc.issueId = ie.issueId
        JOIN story sy on sy.issueId = ie.issueId
        JOIN credit ct on ct.storyId = sy.storyId
        JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId
        WHERE nl.creatorId IN (:creatorIds)
        ORDER BY ss.sortName ASC
           """
    )
    abstract fun getSeriesByCreator(creatorIds: MutableSet<Int>): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT ss.*
        FROM series ss
        JOIN issue ie on ie.seriesId = ss.seriesId
        JOIN mycollection mc ON mc.issueId = ie.issueId
        WHERE publisherId IN (:publishers)
        AND startDate < :endDate AND endDate > :startDate 
        ORDER BY startDate ASC, sortName ASC
           """
    )
    abstract fun getSeriesByPublisherDate(
        publishers: MutableSet<Int>,
        startDate: LocalDate,
        endDate: LocalDate
    ): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT ss.*
        FROM series ss
        JOIN issue ie ON ie.seriesId = ss.seriesId
        JOIN mycollection mc ON mc.issueId = ie.issueId
        JOIN story sy ON sy.issueId = ie.issueId
        JOIN credit ct ON ct.storyId = sy.storyId
        WHERE ss.publisherId IN (:publishers)
        ORDER BY ss.sortName ASC
           """
    )
    abstract fun getSeriesByPublisher(publishers: MutableSet<Int>): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT ss.*
        FROM series ss
        JOIN issue ie on ie.seriesId = ss.seriesId
        JOIN mycollection mc ON mc.issueId = ie.issueId
        JOIN story sy ON sy.issueId = ie.issueId
        JOIN credit ct ON ct.storyId = sy.storyId
        WHERE ss.startDate < :endDate AND ss.endDate > :startDate 
        ORDER BY ss.startDate ASC
           """
    )
    abstract fun getSeriesByDates(startDate: LocalDate, endDate: LocalDate): LiveData<List<Series>>

    @Query(
        """
            SELECT DISTINCT ie.*, ss.seriesName, pr.publisher
            FROM issue ie 
            JOIN mycollection mc ON mc.issueId = ie.issueId
            JOIN story sy ON sy.issueId = ie.issueId
            JOIN credit ct ON ct.storyId = sy.storyId
            JOIN series ss ON ss.seriesId = ie.seriesId
            JOIN publisher pr ON pr.publisherId = ss.publisherId
            JOIN namedetail nl ON nl.nameDetailId = ct.nameDetailId
            WHERE nl.creatorId in (:creatorIds) 
            AND ss.seriesId = :seriesId
            ORDER BY ie.sortCode"""
    )
    abstract fun getIssuesBySeriesCreator(
        seriesId: Int,
        creatorIds: List<Int>
    ): LiveData<List<FullIssue>>

    @Query(
        """
            SELECT ie.*, ss.seriesName, pr.publisher 
            FROM issue ie
            JOIN mycollection mc ON mc.issueId = ie.issueId
            JOIN series ss ON ss.seriesId = ie.seriesId
            JOIN publisher pr ON pr.publisherId = ss.publisherId
            WHERE ss.seriesId=:seriesId
            """
    )

    abstract fun getIssuesBySeries(seriesId: Int): LiveData<List<FullIssue>>

    @Query(
        """
            SELECT COUNT(*) as count
            FROM mycollection mc
            WHERE mc.issueId = :issueId
        """
    )
    abstract fun inCollection(issueId: Int): LiveData<Count>

    @Query(
        """
            DELETE FROM mycollection
            WHERE issueId = :issueId
        """
    )
    abstract fun deleteById(issueId: Int)
}

data class Count(
    val count: Int
)