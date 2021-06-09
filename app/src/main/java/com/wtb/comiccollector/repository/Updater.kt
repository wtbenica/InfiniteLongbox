package com.wtb.comiccollector.repository

import android.content.SharedPreferences
import java.time.LocalDate

open class Updater() {

    // TODO: This should probably get moved out of SharedPreferences and stored with each record.
    //  The tradeoff: an extra local db query vs. having a larger prefs which will end up having
    //  a value for every item in the database.
    protected fun checkIfStale(
        prefsKey: String,
        shelfLife: Long,
        prefs: SharedPreferences
    ): Boolean {
        val lastUpdated = LocalDate.parse(prefs.getString(prefsKey, "${LocalDate.MIN}"))
        val isStale = LocalDate.now().compareTo(lastUpdated.plusDays(shelfLife)) > 0
        return DEBUG || isStale
    }
}