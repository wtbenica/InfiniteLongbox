package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.SortOption

private const val TAG = APP + "SortChipGroup"

class SortChipGroup(context: Context, attributeSet: AttributeSet) :
    ChipGroup(context, attributeSet) {

    init {
        isSingleLine = true
        isSingleSelection = true
        isSelectionRequired = true
    }

    internal var filter: Filter? = null
        set(value) {
            removeAllViews()
            value?.getSortOptions()?.forEach { sortOption ->
                addView(SortChip(context).also { sortChip ->
                    sortChip.sortOption = sortOption
                    if (value.mSortOption == sortOption) {
                        sortChip.isChecked = true
                    }
                })
            }

            field = value
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

