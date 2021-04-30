package com.wtb.comiccollector

import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.material.chip.Chip
import com.wtb.comiccollector.database.models.Filterable

private const val TAG = APP + "Chippy"

class Chippy(context: Context?) : Chip(context) {

    var item: Filterable = item
    private lateinit var caller: ChipCallbacks

    constructor(context: Context?, item: Filterable, caller: ChipCallbacks) : this(context) {
        this.caller = caller
        Log.d(TAG, "Makin Chippy")
        this.text = item.toString()
        this.isClickable = false
        this.setOnCloseIconClickListener { caller.chipClosed(this@Chippy, item) }
    }

    interface ChipCallbacks {
        fun chipClosed(view: View, item: Filterable)
    }
}