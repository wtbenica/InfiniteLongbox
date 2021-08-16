package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.chip.Chip
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.database.models.FilterItem
import kotlinx.coroutines.ExperimentalCoroutinesApi

class FilterChipGroup(context: Context?, attributeSet: AttributeSet) :
    FilterCardChipGroup(context, attributeSet)

@ExperimentalCoroutinesApi
class FilterChip(context: Context?) : Chip(context) {

    lateinit var item: FilterItem
    private var caller: FilterChipCallback? = null

    constructor(
        context: Context?,
        item: FilterItem,
        caller: FilterChipCallback
    ) : this(context) {
        this.item = item
        this.caller = caller
        this.text = item.toString()
        this.setOnCloseIconClickListener { caller.filterChipClosed(it as FilterChip) }
        this.setOnCheckedChangeListener { buttonView, isChecked ->
            caller.filterChipCheckChanged(buttonView as FilterChip, isChecked)
        }
    }

    init {
        this.isClickable = true
        this.isCheckedIconVisible = false
        this.isChecked = true
    }

    companion object {
        private const val TAG = APP + "FilterChip"
    }

    interface FilterChipCallback {
        fun filterChipClosed(chip: FilterChip)
        fun filterChipCheckChanged(buttonView: FilterChip, checked: Boolean)
    }
}