package com.wtb.comiccollector.Daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.wtb.comiccollector.Story

@Dao
abstract class StoryDao : BaseDao<Story>() {
    @Query(
        """
            SELECT st.*
            FROM story st
            NATURAL JOIN issue iss
            JOIN storytype type ON type.typeId = st.storyType
            WHERE iss.issueId = :issueId
            AND (st.storyType = 19 OR st.storyType= 6)
            ORDER BY type.sortCode, storyType
        """
    )
    abstract fun getStories(issueId: Int): LiveData<List<Story>>
}