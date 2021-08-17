package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.google.android.material.chip.Chip
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.SearchFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

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
            val dateString = when (field) {
                LocalDate.of(1900, 1, 1) -> "Start Date"
                LocalDate.now() -> "End Date"
                else          -> field.toString()
            }
            Log.d(TAG, "SETTING TEXT ON DATE CHIP TO: $dateString")
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