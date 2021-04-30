package com.wtb.comiccollector.database.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.Issue
import com.wtb.comiccollector.database.models.IssueAndSeries

@Dao
abstract class IssueDao : BaseDao<Issue>() {
    @Query("SELECT * FROM issue WHERE issueId=:issueId")
    abstract fun getIssue(issueId: Int): LiveData<Issue?>

    @Query("SELECT * FROM issue WHERE issueId=:issueId")
    abstract suspend fun getIssueSus(issueId: Int): Issue?

    @Query("SELECT * FROM issue WHERE issueId=:issueId OR variantOf=:issueId ORDER BY sortCode")
    abstract suspend fun getVariants(issueId: Int): List<Issue>

    @Transaction
    @Query("SELECT * FROM issue WHERE issueId = :issueId")
    abstract fun getFullIssue(issueId: Int): LiveData<IssueAndSeries?>

    @Query(
        """
            SELECT DISTINCT ie.* 
            FROM issue ie 
            JOIN story sy ON sy.issueId = ie.issueId
            JOIN credit ct on ct.storyId = sy.storyId
            JOIN namedetail nl on nl.nameDetailId = ct.nameDetailId
            WHERE nl.creatorId=:creatorId"""
    )
    abstract fun getIssuesByCreator(creatorId: Int): LiveData<List<Issue>>


    @Transaction
    @Query(
        """
            SELECT ie.*, ss.seriesName, pr.publisher
            FROM issue ie
            JOIN series ss ON ss.seriesId = ie.seriesId
            JOIN publisher pr ON pr.publisherId = ss.publisherId
            WHERE ss.seriesId=:seriesId
            AND ie.variantOf IS NULL
            """
    )
    abstract fun getIssuesBySeries(seriesId: Int): LiveData<List<FullIssue>>

    @Transaction
    @Query(
        """
            SELECT ie.*, ss.seriesName, pr.publisher
            FROM issue ie
            JOIN series ss ON ss.seriesId = ie.seriesId
            JOIN publisher pr ON pr.publisherId = ss.publisherId
            WHERE ss.seriesId=:seriesId and ie.issueNum=:issueNum
            """
    )
    abstract fun getIssueByDetails(seriesId: Int, issueNum: Int): LiveData<List<FullIssue>>

    @Transaction
    @Query(
        """
            SELECT DISTINCT ie.*, ss.seriesName, pr.publisher
            FROM issue ie 
            JOIN story sy ON sy.issueId = ie.issueId
            JOIN credit ct ON ct.storyId = sy.storyId
            JOIN series ss ON ss.seriesId = ie.seriesId
            JOIN publisher pr ON pr.publisherId = ss.publisherId
            JOIN namedetail nl ON nl.nameDetailId = ct.nameDetailId
            WHERE nl.creatorId in (:creatorIds) 
            AND ss.seriesId = :seriesId
            ORDER BY ie.sortCode"""
    )
    abstract fun getIssuesBySeriesCreator(
        seriesId: Int,
        creatorIds: List<Int>
    ): LiveData<List<FullIssue>>
}