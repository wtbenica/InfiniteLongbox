package com.wtb.comiccollector.views

import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.material.chip.Chip
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.FilterOption

private const val TAG = APP + "Chippy"

class Chippy(context: Context?) : Chip(context) {

    lateinit var item: FilterOption
    private lateinit var caller: ChipCallbacks

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