package com.wtb.comiccollector

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface Webservice {
    @GET("/db_query/series/{page}")
    fun getSeries(@Path("page") page: Int): Call<List<Item<GcdSeriesJson, Series>>>

    @GET("/db_query/publisher")
    fun getPublishers(): Call<List<Item<GcdPublisherJson, Publisher>>>

    @GET("/db_query/role")
    fun getRoles(): Call<List<Item<GcdRoleJson, Role>>>

    @GET("/db_query/series/{seriesId}/issues")
    fun getIssuesBySeries(@Path("seriesId") seriesId: Int): Call<List<Item<GcdIssueJson, Issue>>>

    @GET("/db_query/issue/{issueId}/credits")
    fun getCreditsByIssue(@Path("issueId") issueId: Int): Call<List<Item<GcdIssueCredit, Credit>>>

    @GET("/db_query/creator/{creatorId}")
    fun getCreator(@Path("creatorId") creatorId: Int): Call<List<Item<GcdCreator, Creator>>>
}
