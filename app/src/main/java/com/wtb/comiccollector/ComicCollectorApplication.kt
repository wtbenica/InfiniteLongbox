/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package com.wtb.comiccollector

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.wtb.comiccollector.network.RetrofitAPIClient
import com.wtb.comiccollector.repository.*
import com.wtb.comiccollector.repository.SHARED_PREFS
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ComicCollectorApplication: Application() {

    companion object {
        private const val TAG = APP + "ComicCollector"

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
        Expander.initialize(webservice, prefs)
        Log.d(TAG, "DONE INITIALIZING")
    }
}