package com.wtb.comiccollector.Views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter

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
            if (value != null) {
                value.getSortOptions().forEach { sortOption ->
                    addView(SortChip(context).also { sortChip ->
                        sortChip.sortOption = sortOption
                        if (value.mSortOption == sortOption) {
                            sortChip.isChecked = true
                            Log.d(
                                TAG,
                                "MATCHY MATCHY ${value.mSortOption} $sortOption ${value.mSeries}"
                            )
                        } else {
                            Log.d(
                                TAG,
                                "NO MATCHY ${value.mSortOption} $sortOption ${value.mSeries}"
                            )
                        }
                    })
                }
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

