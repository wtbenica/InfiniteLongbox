package com.wtb.comiccollector.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.DUMMY_ID
import com.wtb.comiccollector.GroupListViewModels.GroupListViewModel
import com.wtb.comiccollector.Series
import java.time.LocalDate

@Dao
abstract class SeriesDao : BaseDao<Series>() {
    @Query("SELECT * FROM series WHERE seriesId != $DUMMY_ID ORDER BY sortName ASC")
    abstract fun getAllSeries(): LiveData<List<Series>>

    fun getSeriesList(
        creatorId: Int? = null,
        text: String? = null
    ): LiveData<List<Series>> {
        return getSeriesByFilter(GroupListViewModel.Filter(creatorId, text))
    }

    fun getSeriesByFilter(filter: GroupListViewModel.Filter): LiveData<List<Series>> {
        return if (filter.filterId == null) {
            if (filter.text == null || filter.text == "") {
                getAllSeries()
            } else {
                getSeriesByPartial("%" + filter.text + "%")
            }
        } else {
            if (filter.text == null || filter.text == "") {
                getSeriesByCreator(filter.filterId)
            } else {
                getSeriesByCreatorAndPartial(filter.filterId, "%" + filter.text + "%")
            }
        }
    }

    @Query(
        """
        SELECT DISTINCT ss.*
        FROM series ss
        LEFT OUTER JOIN issue ie on ie.seriesId = ss.seriesId
        LEFT OUTER JOIN story sy on sy.issueId = ie.issueId
        LEFT OUTER JOIN credit ct on ct.storyId = sy.storyId
        LEFT OUTER JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId
        LEFT OUTER JOIN creator cr on cr.creatorId = nl.creatorId
        WHERE cr.creatorId = :filterId
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