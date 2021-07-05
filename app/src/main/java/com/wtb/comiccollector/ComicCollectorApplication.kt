@file:Suppress("unused")

package com.wtb.comiccollector

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.wtb.comiccollector.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ComicCollectorApplication: Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        Repository.initialize(this)
        context = this
    }
}