package com.wtb.comiccollector.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.wtb.comiccollector.FullIssue
import com.wtb.comiccollector.Issue
import com.wtb.comiccollector.IssueAndSeries

@Dao
abstract class IssueDao : BaseDao<Issue>() {
    @Transaction
    @Query("SELECT * FROM issue WHERE issueId = :issueId")
    abstract fun getFullIssue(issueId: Int): LiveData<IssueAndSeries?>

    @Query("SELECT issue.* FROM issue NATURAL JOIN credit WHERE nameDetailId=:creatorId")
    abstract fun getIssuesByCreator(creatorId: Int): LiveData<List<Issue>>


    @Query(
        """
            SELECT issue.*, series.seriesName, publisher.publisher 
            FROM issue NATURAL JOIN series NATURAL JOIN publisher 
            WHERE seriesId=:seriesId
            AND variantOf IS NULL
            """
    )
    abstract fun getIssuesBySeries(seriesId: Int): LiveData<List<FullIssue>>

    @Query(
        """
            SELECT issue.*, series.seriesName, publisher.publisher 
            FROM issue NATURAL JOIN series NATURAL JOIN publisher 
            WHERE seriesId=:seriesId and issueNum=:issueNum
            """
    )
    abstract fun getIssueByDetails(seriesId: Int, issueNum: Int): LiveData<List<FullIssue>>

    @Query("SELECT * FROM issue WHERE issueId=:issueId")
    abstract fun getIssue(issueId: Int): LiveData<Issue?>

    @Query("SELECT * FROM issue WHERE issueId=:issueId OR variantOf=:issueId ORDER BY sortCode")
    abstract suspend fun getVariants(issueId: Int) : List<Issue>
}