package com.wtb.comiccollector.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.DUMMY_ID
import com.wtb.comiccollector.Series
import java.time.LocalDate

@Dao
abstract class SeriesDao : BaseDao<Series>() {
    @Query("SELECT * FROM series WHERE seriesId != $DUMMY_ID ORDER BY sortName ASC")
    abstract fun getAllSeries(): LiveData<List<Series>>

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
        SELECT DISTINCT ss.*
        FROM series ss
        JOIN issue ie on ie.seriesId = ss.seriesId 
        JOIN story sy on sy.issueId = ie.issueId
        JOIN credit ct on ct.storyId = sy.storyId
        JOIN namedetail nd on nd.nameDetailId = ct.nameDetailId
        JOIN creator cr on cr.creatorId = nd.creatorId
        WHERE cr.creatorId = :creatorId
        ORDER BY ss.sortName ASC
           """
    )
    abstract fun getSeriesByCreator(creatorId: Int): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT series.*
        FROM series
        NATURAL JOIN issue
        NATURAL JOIN story
        NATURAL JOIN credit
        WHERE series.startDate < :endDate AND series.endDate > :startDate 
        ORDER BY startDate
           """
    )
    abstract fun getSeriesByDates(startDate: LocalDate, endDate: LocalDate): LiveData<List<Series>>

    @Query(
        """
        SELECT DISTINCT series.*
        FROM series
        NATURAL JOIN issue
        NATURAL JOIN credit
        WHERE nameDetailId = :creatorId
        AND series.startDate < :endDate AND series.endDate > :startDate 
        ORDER BY startDate
           """
    )
    abstract fun getSeriesByCreatorAndDates(
        creatorId: Int,
        startDate: LocalDate = LocalDate.MIN,
        endDate: LocalDate = LocalDate.MAX
    ): LiveData<List<Series>>

    @Query("SELECT * FROM series WHERE seriesId=:seriesId")
    abstract fun getSeriesById(seriesId: Int): LiveData<Series?>
}