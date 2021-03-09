package com.wtb.comiccollector

import retrofit2.Call
import retrofit2.http.GET

interface Webservice {
    @GET("/db_query/series")
    fun getSeries(): Call<List<JsonRead.Item>>
}
