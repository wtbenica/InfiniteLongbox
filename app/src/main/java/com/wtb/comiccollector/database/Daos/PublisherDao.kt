package com.wtb.comiccollector.database.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.DUMMY_ID
import com.wtb.comiccollector.database.models.Publisher

@Dao
abstract class PublisherDao : BaseDao<Publisher>() {
    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    abstract fun getPublisher(publisherId: Int): LiveData<Publisher?>

    @Query("SELECT * FROM publisher WHERE publisherId != $DUMMY_ID ORDER BY publisher ASC")
    abstract fun getPublishersList(): LiveData<List<Publisher>>

    @Query("SELECT * FROM publisher WHERE publisherId IN (:publisherIds)")
    abstract fun getPublishers(publisherIds: List<Int>?): LiveData<List<Publisher>?>
}