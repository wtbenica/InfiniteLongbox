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

package dev.benica.infinite_longbox

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dev.benica.infinite_longbox.network.RetrofitAPIClient
import dev.benica.infinite_longbox.repository.Expander
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.repository.SHARED_PREFS
import dev.benica.infinite_longbox.repository.StaticUpdater
import dev.benica.infinite_longbox.repository.UpdateIssueCover
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class InfiniteLongboxApplication: Application() {

    companion object {
        private const val TAG = APP + "InfiniteLongbox"

        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        dev.benica.infinite_longbox.InfiniteLongboxApplication.Companion.context = this
        val retrofit = RetrofitAPIClient.getRetrofitClient()
        val webservice: dev.benica.infinite_longbox.Webservice = retrofit.create(dev.benica.infinite_longbox.Webservice::class.java)
        val prefs: SharedPreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)

        Repository.initialize(this)
        UpdateIssueCover.initialize(webservice, prefs, this)
        StaticUpdater.initialize(webservice, prefs)
        Expander.initialize(webservice, prefs)
        Log.d(dev.benica.infinite_longbox.InfiniteLongboxApplication.Companion.TAG, "DONE INITIALIZING")
    }
}