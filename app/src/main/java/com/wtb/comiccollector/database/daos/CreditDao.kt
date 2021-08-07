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
            JOIN story sy on cr.story = sy.storyId
            JOIN storytype st on st.storyTypeId = sy.storyType
            JOIN role ON cr.role = role.roleId
            WHERE sy.issue = :issueId
            ORDER BY st.sortCode, sy.sequenceNumber, role.sortOrder
        """
    )
    abstract fun getIssueCredits(issueId: Int): Flow<List<FullCredit>>
}