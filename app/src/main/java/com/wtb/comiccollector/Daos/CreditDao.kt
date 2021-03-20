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
            SELECT cr.*
            FROM credit cr
            JOIN story sr on cr.storyId = sr.storyId
            JOIN storytype st on st.typeId = sr.storyType
            JOIN role ON cr.roleId = role.roleId
            WHERE sr.issueId = :issueId
            AND (sr.storyType = 19 OR sr.storyType= 6)
            ORDER BY st.sortCode, sr.sequenceNumber, role.sortOrder
        """
    )
    abstract fun getIssueCredits(issueId: Int): LiveData<List<FullCredit>>
}