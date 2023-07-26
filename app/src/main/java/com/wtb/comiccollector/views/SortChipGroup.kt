/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
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

        toRemoveList.forEach {
            removeView(it)
        }

        toAddList.forEach { sortTypeIn ->
            val chip = SortChip(context)
            chip.callback = this@SortChipGroup
            chip.isChecked = filter.mSortType == sortTypeIn
            chip.sortType = sortTypeIn
            Log.d(
                TAG,
                "Added a sort chip. It ${if (chip.isChecked) "is" else "isn't"} checked. " +
                        "${filter.mSortType} ${filter.getSortOptions()} ${chip.sortType}"
            )
            addView(chip)
        }

        sortTypes = sortColumnsIn
    }

    override fun sortOrderChanged(sortType: SortType) = callback?.sortOrderChanged(sortType) ?: Unit


    interface SortChipGroupCallback {
        fun sortOrderChanged(sortType: SortType)
    }
}

@ExperimentalCoroutinesApi
class SortChip(context: Context?) : Chip(context) {

    init {
        isCloseIconVisible = true
        setOnClickListener {
            if (isChecked) {
                sortType = sortType?.toggle()
            } else {
                super.callOnClick()
            }
        }
    }

    internal var sortType: SortType? = null
        set(value) {
            field = value
            text = value?.tag
            value?.order?.icon?.let { setCloseIconResource(it) }
            value?.order?.contentDescription.let { closeIconContentDescription = it }
            field?.let {
                if (isChecked) {
                    callback?.sortOrderChanged(it)
                }
            }
        }

    var callback: SortChipCallback? = null

    interface SortChipCallback {
        fun sortOrderChanged(sortType: SortType)
    }
}

