@file:Suppress("unused")

package com.wtb.comiccollector

import android.app.Application

class ComicCollectorApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        NewIssueRepository.initialize(this)
    }
}