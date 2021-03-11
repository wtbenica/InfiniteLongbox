package com.wtb.comiccollector

import retrofit2.Call
import retrofit2.http.GET

interface Webservice {
    @GET("/db_query/series")
    fun getSeries(): Call<List<Item<GcdSeriesJson>>>

    @GET("/db_query/publisher")
    fun getPublishers(): Call<List<Item<GcdPublisherJson>>>
}
