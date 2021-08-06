package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.wtb.comiccollector.database.models.ExCredit
import com.wtb.comiccollector.database.models.FullCredit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class ExCreditDao : BaseDao<ExCredit>("excredit") {

    @Query("SELECT * FROM excredit WHERE story IN (:storyIds)")
    abstract fun getExCreditsByStoryIds(storyIds: List<Int>): List<ExCredit>

    @Transaction
    @Query(
        """
            SELECT exc.*, st.sortCode
            FROM excredit exc
            JOIN story sr on exc.story = sr.storyId
            JOIN storytype st on st.storyTypeId = sr.storyType
            JOIN role ON exc.role = role.roleId
            JOIN namedetail nd ON nd.nameDetailId = exc.nameDetail
            JOIN creator c on c.creatorId = nd.creator
            WHERE sr.issueId = :issueId
            ORDER BY st.sortCode, sr.sequenceNumber, role.sortOrder
        """
    )
    abstract fun getIssueExtractedCredits(issueId: Int): Flow<List<FullCredit>>
}