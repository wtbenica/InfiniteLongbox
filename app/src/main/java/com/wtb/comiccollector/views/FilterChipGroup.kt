package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.chip.Chip
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.FilterOption

class FilterChipGroup(context: Context?, attributeSet: AttributeSet) :
    FilterCardChipGroup(context, attributeSet)

class FilterChip(context: Context?) : Chip(context) {

    var item: FilterOption? = null
    private var caller: FilterChipCallbacks? = null

    constructor(
        context: Context?,
        item: FilterOption,
        caller: FilterChipCallbacks
    ) : this(context) {
        this.item = item
        this.caller = caller
        this.text = item.toString()
        this.setOnCloseIconClickListener { caller.chipClosed(item) }
    }

    init {
        this.isClickable = true
        this.isCheckedIconVisible = false
    }

    companion object {
        private const val TAG = APP + "FilterChip"
    }

    interface FilterChipCallbacks {
        fun chipClosed(item: FilterOption)
    }
}