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
    fun getCredits(@Path("issueId") issueId: Int): Call<List<Item<GcdCredit, Credit>>>

    @GET("/db_query/issue/{issueId}/stories")
    fun getStories(@Path("issueId") issueId: Int): Call<List<Item<GcdStory, Story>>>

    @GET("/db_query/creator/{creatorId}")
    fun getCreator(@Path("creatorId") creatorId: Int): Call<Item<GcdCreator, Creator>>

    @GET("/db_query/story_types")
    fun getStoryTypes(): Call<List<Item<GcdStoryType, StoryType>>>

    @GET("/db_query/creator/name/{name}")
    fun getCreatorByName(@Path("name") name: String): Call<List<Item<GcdCreator, Creator>>>

    @GET("/db_query/issue/{issueId}/creators")
    fun getCreatorsByIssue(@Path("issueId") issueId: Int): Call<List<Item<GcdCreator, Creator>>>
}

