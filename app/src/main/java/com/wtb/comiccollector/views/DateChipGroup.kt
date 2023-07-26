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
import com.google.android.material.chip.Chip
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ExperimentalCoroutinesApi
class DateChipGroup(context: Context?, attributeSet: AttributeSet) :
    FilterCardChipGroup(context, attributeSet), DateChip.DateChipCallback {

    var callback: DateChipGroupCallback? = null

    private val startDateChip = DateChip(
        context,
        LocalDate.MIN,
        this,
        true
    )

    private val endDateChip = DateChip(
        context,
        LocalDate.MAX,
        this,
        false
    )

    init {
        this.removeAllViews()
        this.addView(startDateChip)
        this.addView(endDateChip)
    }

    fun update(filter: SearchFilter) {
        startDateChip.mDate = filter.mStartDate
        endDateChip.mDate = filter.mEndDate
    }

    override fun setDate(date: LocalDate, isStart: Boolean) {
        callback?.setDate(date, isStart)
    }

    override fun getDate(chip: DateChip, currentDate: LocalDate) {
        val date = when (currentDate) {
            LocalDate.MIN -> LocalDate.of(1900, 1, 1)
            LocalDate.MAX -> LocalDate.now()
            else          -> currentDate
        }
        callback?.getDate(date, chip.mIsStart)
    }

    fun setStartDate(it: LocalDate) {
        startDateChip.mDate = it
        callback?.setDate(it, true)
    }

    fun setEndDate(it: LocalDate) {
        endDateChip.mDate = it
        callback?.setDate(it, false)
    }

    interface DateChipGroupCallback {
        fun getDate(currentSelection: LocalDate, isStart: Boolean)
        fun setDate(date: LocalDate, isStart: Boolean)
    }
}


@ExperimentalCoroutinesApi
class DateChip(context: Context?, attrs: AttributeSet? = null) : Chip(context, attrs) {
    private var mCaller: DateChipCallback? = null
    internal var mIsStart: Boolean = true
    private var mInitDate: LocalDate = LocalDate.MIN
    internal var mDate: LocalDate? = null
        set(value) {
            field = value
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")
            val DATE_START = LocalDate.of(1900, 1, 1)
            val dateString = when (field) {
                DATE_START      -> "Start Date"
                LocalDate.now() -> "End Date"
                else            -> field?.format(formatter)?.uppercase()

            }
            this.isCloseIconVisible = field != DATE_START && field != LocalDate.now()
            this.text = dateString
        }

    init {
        isChecked = true
        isCheckable = false

        setOnClickListener {
            mCaller?.getDate(this, this.mDate!!)
        }

        setOnCloseIconClickListener {
            mCaller?.setDate(mInitDate, mIsStart)
        }
    }

    constructor(
        context: Context?,
        date: LocalDate,
        caller: DateChipCallback,
        isStart: Boolean,
    ) : this(context) {
        mInitDate = date
        mDate = date
        this.mCaller = caller
        this.mIsStart = isStart
    }

    interface DateChipCallback {
        fun setDate(date: LocalDate, isStart: Boolean)
        fun getDate(chip: DateChip, currentDate: LocalDate)
    }

    companion object {
        private const val TAG = APP + "DateChip"
    }
}