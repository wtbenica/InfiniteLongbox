package com.wtb.comiccollector.views

import android.content.Context
import com.google.android.material.chip.Chip
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.FilterOption

private const val TAG = APP + "FilterChip"

class FilterChip(context: Context?) : Chip(context) {

    var item: FilterOption? = null
    private var caller: FilterChipCallbacks? = null

    constructor(context: Context?, item: FilterOption, caller: FilterChipCallbacks) : this(context) {
        this.item = item
        this.caller = caller
        this.text = item.toString()
        this.isClickable = false
        this.setOnCloseIconClickListener { caller.chipClosed(item) }
    }

    interface FilterChipCallbacks {
        fun chipClosed(item: FilterOption)
    }
}