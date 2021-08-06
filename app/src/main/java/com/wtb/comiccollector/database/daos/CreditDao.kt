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

    @Query("SELECT * FROM credit WHERE story IN (:storyIds)")
    abstract fun getCreditsByStoryIds(storyIds: List<Int>): List<Credit>

    @Transaction
    @Query(
        """
            SELECT cr.*, st.sortCode
            FROM credit cr
            JOIN story sr on cr.story = sr.storyId
            JOIN storytype st on st.storyTypeId = sr.storyType
            JOIN role ON cr.role = role.roleId
            JOIN namedetail nd ON nd.nameDetailId = cr.nameDetail
            JOIN creator c on c.creatorId = nd.creator
            WHERE sr.issueId = :issueId
            ORDER BY st.sortCode, sr.sequenceNumber, role.sortOrder
        """
    )
    abstract fun getIssueCredits(issueId: Int): Flow<List<FullCredit>>
}