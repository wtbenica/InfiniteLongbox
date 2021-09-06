@file:Suppress("unused")

package com.wtb.comiccollector

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.wtb.comiccollector.network.RetrofitAPIClient
import com.wtb.comiccollector.repository.Repository
import com.wtb.comiccollector.repository.SHARED_PREFS
import com.wtb.comiccollector.repository.StaticUpdater
import com.wtb.comiccollector.repository.UpdateIssueCover
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ComicCollectorApplication: Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        val retrofit = RetrofitAPIClient.getRetrofitClient()
        val webservice: Webservice = retrofit.create(Webservice::class.java)
        val prefs: SharedPreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)

        Repository.initialize(this)
        UpdateIssueCover.initialize(webservice, prefs, this)
        StaticUpdater.initialize(webservice, prefs)
    }
}