package com.wtb.comiccollector

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface Webservice {
    @GET("/db_query/series_list/{page}")
    fun getSeries(@Path("page") page: Int): Call<List<Item<GcdSeries, Series>>>

    @GET("/db_query/publisher")
    fun getPublishers(): Call<List<Item<GcdPublisher, Publisher>>>

    @GET("/db_query/role")
    fun getRoles(): Call<List<Item<GcdRole, Role>>>

    @GET("/db_query/series/{seriesId}/issues")
    fun getIssuesBySeries(@Path("seriesId") seriesId: Int): Call<List<Item<GcdIssue, Issue>>>

    @GET("/db_query/issue/{issueId}/credits")
    fun getCreditsByIssue(@Path("issueId") issueId: Int): Call<List<Item<GcdCredit, Credit>>>

    @GET("/db_query/issue/{issueId}/stories")
    fun getStoriesByIssue(@Path("issueId") issueId: Int): Call<List<Item<GcdStory, Story>>>

    @GET("/db_query/creator/{creatorId}")
    fun getCreator(@Path("creatorId") creatorId: Int): Call<List<Item<GcdCreator, Creator>>>

    @GET("/db_query/creator_list/{creatorIds}")
    fun getCreator(@Path("creatorIds") creatorId: List<Int>): Call<List<Item<GcdCreator, Creator>>>

    @GET("/db_query/story_types")
    fun getStoryTypes(): Call<List<Item<GcdStoryType, StoryType>>>

    @GET("/db_query/issue/{issueId}/creators")
    fun getCreatorsByIssue(@Path("issueId") issueId: Int): Call<List<Item<GcdCreator, Creator>>>

    @GET("/db_query/creator/{creatorId}/credits")
    fun getCreditsByCreator(@Path("creatorId") creatorId: Int): Call<List<Item<GcdCredit, Credit>>>

    @GET("/db_query/name_detail/{nameDetailId}/stories")
    fun getStoriesByNameDetail(@Path("nameDetailId") nameDetailId: List<Int>): Call<List<Item<GcdStory,
            Story>>>

    @GET("/db_query/name_detail/{nameDetailIds}")
    fun getNameDetails(@Path("nameDetailIds") nameDetailIds: List<Int>): Call<List<Item<GcdNameDetail,
            NameDetail>>>

    @GET("/db_query/creators/{creatorIds}/name_details")
    fun getNameDetailsByCreatorIds(@Path("creatorIds") creatorIds: List<Int>):
            Call<List<Item<GcdNameDetail, NameDetail>>>

    @GET("/db_query/name_detail/name/{name}")
    fun getCreatorByName(@Path("name") name: String): Call<List<Item<GcdNameDetail,
            NameDetail>>>

    @GET("/db_query/issues/{issueIds}")
    fun getIssues(@Path("issueIds") issueIds: List<Int>): Call<List<Item<GcdIssue, Issue>>>

    @GET("/db_query/stories/{storyIds}/credits")
    fun getCreditsByStories(@Path("storyIds") storyIds: List<Int>): Call<List<Item<GcdCredit, Credit>>>

    @GET("/db_query/creators/{creatorIds}/name_details")
    fun getNameDetailsByCreator(@Path("creatorIds") creatorIds: List<Int>):
            Call<List<Item<GcdNameDetail, NameDetail>>>
}

