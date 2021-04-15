package com.wtb.comiccollector.database.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.DUMMY_ID
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.database.models.Series
import java.time.LocalDate

@Dao
abstract class SeriesDao : BaseDao<Series>() {
    @Query("SELECT * FROM series WHERE seriesId=:seriesId")
    abstract fun getSeries(seriesId: Int): LiveData<Series?>

    @Query("SELECT * FROM series WHERE seriesId != $DUMMY_ID ORDER BY sortName ASC")
    abstract fun getAllSeries(): LiveData<List<Series>>

    fun getSeriesByFilter(filter: Filter): LiveData<List<Series>> {
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
        NATURAL JOIN issue ie
        NATURAL JOIN credit ct
        WHERE ss.publisherId IN (:publishers)
        ORDER BY ss.sortName ASC
           """
    )
    abstract fun getSeriesByPublisher(publishers: MutableSet<Int>): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT *
        FROM series 
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
        JOIN issue ie on ie.seriesId = ss.seriesId 
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
        JOIN story sy ON sy.issueId = ie.issueId
        JOIN credit ct ON ct.storyId = sy.storyId
        WHERE ss.startDate < :endDate AND ss.endDate > :startDate 
        ORDER BY ss.startDate ASC
           """
    )
    abstract fun getSeriesByDates(startDate: LocalDate, endDate: LocalDate): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT ss.*
        FROM series ss
        JOIN issue ie ON ie.seriesId = ss.seriesId
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
}