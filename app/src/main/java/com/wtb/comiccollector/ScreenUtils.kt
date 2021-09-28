package com.wtb.comiccollector

import android.app.Activity
import android.util.DisplayMetrics

class ScreenUtils {
    fun getScreenDimens(activity: Activity): Pair<Int, Int> {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }
}