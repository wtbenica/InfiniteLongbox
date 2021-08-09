package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.MyCollection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "CollectionDao"

@ExperimentalCoroutinesApi
@Dao
abstract class CollectionDao : BaseDao<MyCollection>("collection") {

    @Query(
        """
            SELECT COUNT(*) as count
            FROM mycollection mc
            WHERE mc.issue = :issueId
        """
    )
    abstract fun inCollection(issueId: Int): Flow<Count>

    @Query(
        """
            DELETE FROM mycollection
            WHERE issue = :issueId
        """
    )
    abstract fun deleteById(issueId: Int)
}

data class Count(
    val count: Int
)