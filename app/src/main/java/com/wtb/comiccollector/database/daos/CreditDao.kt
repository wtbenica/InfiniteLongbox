package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.wtb.comiccollector.database.models.Credit
import com.wtb.comiccollector.database.models.FullCredit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class CreditDao : BaseDao<Credit>("credit") {

    @Query("SELECT * FROM credit WHERE storyId IN (:storyIds)")
    abstract fun getCreditsByStoryIds(storyIds: List<Int>): List<Credit>

    @Transaction
    @Query(
        """
            SELECT cr.*, st.sortCode
            FROM credit cr
            JOIN story sr on cr.storyId = sr.storyId
            JOIN storytype st on st.typeId = sr.storyType
            JOIN role ON cr.roleId = role.roleId
            JOIN namedetail nd ON nd.nameDetailId = cr.nameDetailId
            JOIN creator c on c.creatorId = nd.creatorId
            WHERE sr.issueId = :issueId
            ORDER BY st.sortCode, sr.sequenceNumber, role.sortOrder
        """
    )
    abstract fun getIssueCredits(issueId: Int): Flow<List<FullCredit>>
}