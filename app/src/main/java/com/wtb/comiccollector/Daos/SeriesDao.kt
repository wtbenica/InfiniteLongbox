package com.wtb.comiccollector.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.DUMMY_ID
import com.wtb.comiccollector.Series
import java.time.LocalDate

@Dao
abstract class SeriesDao : BaseDao<Series>() {
    @Query("SELECT * FROM series WHERE seriesId != $DUMMY_ID ORDER BY seriesName ASC")
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

    @Query("SELECT * FROM series WHERE seriesId=:seriesId")
    abstract fun getSeriesById(seriesId: Int): LiveData<Series?>
}