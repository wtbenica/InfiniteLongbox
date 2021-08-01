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

    @Query("SELECT * FROM excredit WHERE storyId IN (:storyIds)")
    abstract fun getExCreditsByStoryIds(storyIds: List<Int>): List<ExCredit>

    @Transaction
    @Query(
        """
            SELECT exc.*, st.sortCode
            FROM excredit exc
            JOIN story sr on exc.storyId = sr.storyId
            JOIN storytype st on st.typeId = sr.storyType
            JOIN role ON exc.roleId = role.roleId
            JOIN namedetail nd ON nd.nameDetailId = exc.nameDetailId
            JOIN creator c on c.creatorId = nd.creatorId
            WHERE sr.issueId = :issueId
            ORDER BY st.sortCode, sr.sequenceNumber, role.sortOrder
        """
    )
    abstract fun getIssueExtractedCredits(issueId: Int): Flow<List<FullCredit>>
}