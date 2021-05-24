package com.wtb.comiccollector.views

import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.material.chip.Chip
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.FilterOption

private const val TAG = APP + "Chippy"

class Chippy(context: Context?) : Chip(context) {

    private var item: FilterOption? = null
    private var caller: ChipCallbacks? = null

    constructor(context: Context?, item: FilterOption, caller: ChipCallbacks) : this(context) {
        this.item = item
        this.caller = caller
        Log.d(TAG, "Makin Chippy")
        this.text = item.toString()
        this.isClickable = false
        this.setOnCloseIconClickListener { caller.chipClosed(this@Chippy, item) }
    }

    interface ChipCallbacks {
        fun chipClosed(view: View, item: FilterOption)
    }
}