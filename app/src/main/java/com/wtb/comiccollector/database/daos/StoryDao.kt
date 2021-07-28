package com.wtb.comiccollector.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.database.models.Story
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class StoryDao : BaseDao<Story>() {
    @Query(query)
    abstract fun getStoriesFlow(issueId: Int): Flow<List<Story>>

    @Query(query)
    abstract fun getStories(issueId: Int): List<Story>

    companion object {
        const val query = """
            SELECT st.*
            FROM story st
            JOIN issue iss on iss.issueId = st.issueId
            JOIN storytype type ON type.typeId = st.storyType
            WHERE st.issueId = :issueId
            AND (st.storyType = 19 OR st.storyType= 6)
            ORDER BY sequenceNumber
        """
    }
}