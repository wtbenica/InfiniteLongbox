package com.wtb.comiccollector.network

import com.wtb.comiccollector.repository.BASE_URL
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitAPIClient {
    companion object {
        private var retrofit: Retrofit? = null

        fun getRetrofitClient(): Retrofit {

            if (retrofit == null) {
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                val retro = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                retrofit = retro

                return retro
            } else {
                return retrofit!!
            }
        }
    }
}