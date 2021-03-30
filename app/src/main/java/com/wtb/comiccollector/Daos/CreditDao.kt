package com.wtb.comiccollector.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.wtb.comiccollector.Credit
import com.wtb.comiccollector.FullCredit

@Dao
abstract class CreditDao : BaseDao<Credit>() {
    @Transaction
    @Query(
        """
            SELECT cr.*, c.*
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
    abstract fun getIssueCredits(issueId: Int): LiveData<List<FullCredit>>

    @Transaction
    @Query(
        """
            SELECT cr.*, c.*
            FROM credit cr
            JOIN namedetail nd ON nd.nameDetailId = cr.nameDetailId
            JOIN creator c on c.creatorId = nd.creatorId
            WHERE cr.nameDetailId = :nameDetailId
        """
    )
    abstract suspend fun getIssueCreditsByNameDetailSus(nameDetailId: Int): List<FullCredit>
}