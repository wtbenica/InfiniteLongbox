package com.wtb.comiccollector

import com.wtb.comiccollector.database.models.*
import retrofit2.http.GET
import retrofit2.http.Path

interface Webservice {
    // GET Issues
    @GET("/db_query/series/{seriesId}/issues")
    suspend fun getIssuesBySeries(@Path("seriesId") seriesId: Int): List<Item<GcdIssue, Issue>>

    @GET("/db_query/issues/{issueIds}")
    suspend fun getIssues(@Path("issueIds") issueIds: List<Int>): List<Item<GcdIssue, Issue>>

    // GET Credits
    @GET("/db_query/creator/{nameDetailIds}/credits")
    suspend fun getCreditsByNameDetail(@Path("nameDetailIds") nameDetailIds: List<Int>): List<Item<GcdCredit, Credit>>

    @GET("/db_query/stories/{storyIds}/credits")
    suspend fun getCreditsByStories(@Path("storyIds") storyIds: List<Int>): List<Item<GcdCredit, Credit>>

    @GET("/db_query/stories/{storyIds}/extracts")
    suspend fun getExtractedCreditsByStories(@Path("storyIds") storyIds: List<Int>): List<Item<GcdCredit, Credit>>

    // GET Stories
    @GET("/db_query/issue/{issueId}/stories")
    suspend fun getStoriesByIssue(@Path("issueId") issueId: Int): List<Item<GcdStory, Story>>

    @GET("/db_query/creator_name/{name}/stories")
    suspend fun getStoriesByName(@Path("name") name: String): List<Item<GcdStory, Story>>

    // GET Creator
    @GET("/db_query/creator_list/{creatorIds}")
    suspend fun getCreator(@Path("creatorIds") creatorId: List<Int>): List<Item<GcdCreator, Creator>>

    // GET NameDetails
    @GET("/db_query/name_detail/{nameDetailIds}")
    suspend fun getNameDetailsByIds(@Path("nameDetailIds") nameDetailIds: List<Int>):
            List<Item<GcdNameDetail, NameDetail>>

    @GET("/db_query/name_detail/name/{name}")
    suspend fun getNameDetailByName(@Path("name") name: String): List<Item<GcdNameDetail,
            NameDetail>>

    // GET Publishers, Roles, Series, StoryTypes, Stories
    @GET("/db_query/publisher")
    suspend fun getPublishers(): List<Item<GcdPublisher, Publisher>>

    @GET("/db_query/role")
    suspend fun getRoles(): List<Item<GcdRole, Role>>

    @GET("/db_query/story_types")
    suspend fun getStoryTypes(): List<Item<GcdStoryType, StoryType>>

    @GET("/db_query/series_list/{page}")
    suspend fun getSeries(@Path("page") page: Int): List<Item<GcdSeries, Series>>

    @GET("/db_query/story/{storyIds}")
    suspend fun getStories(@Path("storyIds") storyIds: List<Int>): List<Item<GcdStory, Story>>

    @GET("/db_query/name_details/creator_ids/{creatorIds}")
    suspend fun getNameDetailsByCreatorIds(@Path("creatorIds") creatorIds: List<Int>):
            List<Item<GcdNameDetail, NameDetail>>

}

