package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.database.models.Story
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import org.intellij.lang.annotations.Language

@ExperimentalCoroutinesApi
@Dao
abstract class StoryDao : BaseDao<Story>("Story") {

    @Query(query)
    abstract fun getStoriesFlow(issueId: Int): Flow<List<Story>>

    @Query(query)
    abstract suspend fun getStories(issueId: Int): List<Story>

    @Query(
        """
            DELETE FROM story
            WHERE 0 = 0
        """
    )
    abstract fun dropAll()

    companion object {
        @Language("RoomSql")
        const val query = """
            SELECT sy.*
            FROM story sy
            JOIN storytype se ON se.storyTypeId = sy.storyType
            WHERE sy.issue = :issueId
            AND (sy.storyType = 19 OR sy.storyType= 6)
            ORDER BY sy.sequenceNumber
        """
    }
}