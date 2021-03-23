package com.wtb.comiccollector.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.Creator
import java.time.LocalDate

@Dao
abstract class CreatorDao : BaseDao<Creator>() {
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
        seriesId: Int,
        startDate: LocalDate = LocalDate.MIN,
        endDate: LocalDate = LocalDate.MAX
    ): LiveData<List<Creator>>

    @Query("SELECT * FROM creator ORDER BY sortName ASC")
    abstract fun getCreatorsList(): LiveData<List<Creator>>

    @Query(
        """
            SELECT *
            FROM creator cr
            WHERE cr.name = :creator
        """
    )
    abstract fun getCreatorByName(creator: String): LiveData<Creator?>

    @Query("SELECT * FROM creator WHERE creatorId = :creatorId")
    abstract fun getCreator(vararg creatorId: Int): LiveData<List<Creator>>?


}