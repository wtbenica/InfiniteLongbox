package com.wtb.comiccollector.database.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.wtb.comiccollector.database.models.ExCredit
import com.wtb.comiccollector.database.models.FullCredit

@Dao
abstract class ExCreditDao : BaseDao<ExCredit>() {

    @Transaction
    @Query(
        """
            SELECT exc.*, c.*, st.sortCode
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
    abstract fun getIssueExtractedCredits(issueId: Int): LiveData<List<FullCredit>>
}