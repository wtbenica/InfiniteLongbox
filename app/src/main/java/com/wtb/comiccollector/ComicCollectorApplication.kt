@file:Suppress("unused")

package com.wtb.comiccollector

import android.app.Application
import com.wtb.comiccollector.repository.Repository

class ComicCollectorApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Repository.initialize(this)
    }
}