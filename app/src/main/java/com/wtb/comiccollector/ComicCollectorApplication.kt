@file:Suppress("unused")

package com.wtb.comiccollector

import android.app.Application
import com.wtb.comiccollector.repository.IssueRepository

class ComicCollectorApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        IssueRepository.initialize(this)
    }
}