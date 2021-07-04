package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.SortType
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "SortChipGroup"

@ExperimentalCoroutinesApi
class SortChipGroup(context: Context, attributeSet: AttributeSet) :
    FilterCardChipGroup(context, attributeSet), SortChip.SortChipCallback {

    private var sortTypes: List<String> = emptyList()
    internal var callback: SortChipGroupCallback? = null

    init {
        isSingleSelection = true
        isSelectionRequired = true
    }

    fun update(filter: SearchFilter) {
        val sortOptionsIn: List<SortType> = filter.getSortOptions()
        val sortColumnsIn = sortOptionsIn.map { it.sortColumn }

        val toAddList: List<SortType> = sortOptionsIn.mapNotNull {
            if (it.sortColumn in sortTypes) {
                null
            } else {
                it
            }
        }

        val toRemoveList: List<View> = children.mapNotNull {
            if (it is SortChip && it.sortType?.sortColumn in sortColumnsIn) {
                null
            } else {
                it
            }
        }.toList()

        toAddList.forEach { sortOption ->
            addView(SortChip(context).apply {
                callback = this@SortChipGroup
                this.sortType = sortOption
                if (filter.mSortType == sortOption) {
                    isChecked = true
                }
            })
        }

        toRemoveList.forEach {
            removeView(it)
        }

        sortTypes = sortColumnsIn
    }

    override fun sortOrderChanged(sortType: SortType) {
        Log.d(TAG, "sortOrderChanged: ${sortType.order}")
        callback?.sortOrderChanged(sortType)
    }

    interface SortChipGroupCallback {
        fun sortOrderChanged(sortType: SortType)
    }
}

class SortChip(context: Context) : Chip(context) {

    init {
        setOnClickListener { view ->
            val chip = view as SortChip
            Log.d(TAG, "Chip ${if (chip.isChecked) "is" else "isn't"} checked")
            if (chip.isChecked) {
                sortType = sortType?.let { it: SortType ->
                    SortType(it).apply {
                        this.order = when (this.order) {
                            SortType.SortOrder.ASC  -> SortType.SortOrder.DESC
                            SortType.SortOrder.DESC -> SortType.SortOrder.ASC
                        }
                    }
                }
            } else {
                super.callOnClick()
            }
        }
    }

    internal var sortType: SortType? = null
        set(value) {
            field = value
            Log.d(TAG, "setting sortOption: ${value?.sortString}")
            text = value?.tag
            icon = value?.order?.icon
            field?.let {
                Log.d(TAG, "Chip is checked, changing sort order ${it.sortString}")
                callback?.sortOrderChanged(it)
            }
        }

    private var icon: Int? = null
        set(value) {
            field = value
            closeIcon = value?.let { ResourcesCompat.getDrawable(resources, it, null) }
        }

    var callback: SortChipCallback? = null

    interface SortChipCallback {
        fun sortOrderChanged(sortType: SortType)
    }
}

