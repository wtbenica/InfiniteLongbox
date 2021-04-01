package com.wtb.comiccollector.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.DUMMY_ID
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.Series
import java.time.LocalDate

@Dao
abstract class SeriesDao : BaseDao<Series>() {
    @Query("SELECT * FROM series WHERE seriesId != $DUMMY_ID ORDER BY sortName ASC")
    abstract fun getAllSeries(): LiveData<List<Series>>

    //    fun getSeriesList(
//        creatorId: Int? = null,
//        text: String? = null
//    ): LiveData<List<Series>> {
//        return getSeriesByFilter(Filter(creatorId, text))
//    }
//
    fun getSeriesByFilter(filter: Filter): LiveData<List<Series>> {
        return if (filter.hasCreator()) {
            if (filter.hasPublisher()) {
                if (filter.hasDateFilter()) {
                    getSeriesByCreatorPublisherDates(
                        filter.mCreators,
                        filter.mPublishers,
                        filter.mStartDate,
                        filter.mEndDate
                    )
                } else {
                    getSeriesByCreatorPublisher(filter.mCreators, filter.mPublishers)
                }
            } else {
                if (filter.hasDateFilter()) {
                    getSeriesByCreatorDates(filter.mCreators, filter.mStartDate, filter.mEndDate)
                } else {
                    getSeriesByCreator(filter.mCreators)
                }
            }
        } else {
            if (filter.hasPublisher()) {
                if (filter.hasDateFilter()) {
                    getSeriesByPublisherDate(filter.mPublishers, filter.mStartDate, filter.mEndDate)
                } else {
                    getSeriesByPublisher(filter.mPublishers)
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
        SELECT DISTINCT ss.*
        FROM series ss
        NATURAL JOIN issue ie
        NATURAL JOIN credit ct
        JOIN namedetail nl ON nl.nameDetailId = ct.nameDetailId
        AND ss.publisherId IN (:publishers)
        AND ss.startDate < :endDate AND ss.endDate > :startDate 
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
        NATURAL JOIN issue ie
        NATURAL JOIN credit ct
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
        NATURAL JOIN issue ie
        NATURAL JOIN credit ct
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
        LEFT OUTER JOIN issue ie on ie.seriesId = ss.seriesId
        LEFT OUTER JOIN story sy on sy.issueId = ie.issueId
        LEFT OUTER JOIN credit ct on ct.storyId = sy.storyId
        LEFT OUTER JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId
        LEFT OUTER JOIN creator cr on cr.creatorId = nl.creatorId
        WHERE nl.creatorId = :filterId
        AND (
                ss.seriesName LIKE :text 
                OR cr.name LIKE :text
                OR sy.characters LIKE :text
            )
    """
    )
    abstract fun getSeriesByCreatorAndPartial(filterId: Int, text: String): LiveData<List<Series>>

    @Query(
        """
            SELECT DISTINCT ss.*
            FROM series ss
            LEFT OUTER JOIN issue ie ON ie.seriesId = ss.seriesId
            LEFT OUTER JOIN story sy ON sy.issueId = ie.issueId
            LEFT OUTER JOIN credit ct ON ct.storyId = sy.storyId
            LEFT OUTER JOIN namedetail nl ON nl.nameDetailId = ct.nameDetailId
            LEFT OUTER JOIN creator cr on cr.creatorId = nl.creatorId
            WHERE ss.seriesName LIKE :text
            OR cr.name LIKE :text
            OR sy.characters LIKE :text
        """
    )
    abstract fun getSeriesByPartial(text: String): LiveData<List<Series>>


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
        SELECT DISTINCT series.*
        FROM series
        NATURAL JOIN issue
        NATURAL JOIN story
        NATURAL JOIN credit
        WHERE series.startDate < :endDate AND series.endDate > :startDate 
        ORDER BY startDate ASC
           """
    )
    abstract fun getSeriesByDates(startDate: LocalDate, endDate: LocalDate): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT ss.*
        FROM series ss
        NATURAL JOIN issue ie
        NATURAL JOIN credit ct
        JOIN namedetail nl ON nl.nameDetailId = ct.nameDetailId
        WHERE nl.creatorId IN (:creatorIds)
        AND ss.startDate < :endDate AND ss.endDate > :startDate 
        ORDER BY startDate
           """
    )
    abstract fun getSeriesByCreatorDates(
        creatorIds: MutableSet<Int>,
        startDate: LocalDate,
        endDate: LocalDate
    ): LiveData<List<Series>>

    @Query("SELECT * FROM series WHERE seriesId=:seriesId")
    abstract fun getSeriesById(seriesId: Int): LiveData<Series?>
}