package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.SortOption
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "SortChipGroup"

@ExperimentalCoroutinesApi
class SortChipGroup(context: Context, attributeSet: AttributeSet) :
    ChipGroup(context, attributeSet) {

    init {
        isSingleLine = true
        isSingleSelection = true
        isSelectionRequired = true
    }

    fun update(filter: SearchFilter) {
        removeAllViews()
        filter.getSortOptions().forEach { sortOption ->
            addView(SortChip(context).also { sortChip ->
                sortChip.sortOption = sortOption
                if (filter.mSortOption == sortOption) {
                    sortChip.isChecked = true
                }
            })
        }
    }

    inner class SortChip(context: Context) : Chip(context) {

        internal var sortOption: SortOption? = null
            set(value) {
                text = value?.tag
                field = value
            }

        init {
            isCloseIconVisible = false
        }
    }
}

