package com.wtb.comiccollector

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.time.LocalDate
import java.util.*

const val ARG_DATE = "date"
const val ARG_MIN_DATE = "min_date"
const val ARG_MAX_DATE = "max_date"
const val ARG_REQUEST_KEY = "request_key"

class DatePickerFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        var date: LocalDate = LocalDate.now()
        var minDate: LocalDate = LocalDate.MIN
        var maxDate: LocalDate = LocalDate.MAX

        var reqKey: String? = null

        arguments?.let { bundle ->
            bundle.getSerializable(ARG_DATE)?.let {
                date = it as LocalDate
            }
            bundle.getSerializable(ARG_MIN_DATE)?.let {
                minDate = it as LocalDate
            }
            bundle.getSerializable(ARG_MAX_DATE)?.let {
                maxDate = it as LocalDate
            }
            bundle.getCharSequence(ARG_REQUEST_KEY)?.let {
                reqKey = it.toString()
            }
        }

        val dateListener =
            DatePickerDialog.OnDateSetListener { _: DatePicker, year: Int, month: Int, day: Int ->

                // Add one to month bc. datePicker months are zero-indexed, while LocalDate is 1-indexed
                val resultDate: LocalDate = LocalDate.of(year, month + 1, day)
                val resultBundle = Bundle().apply {
                    putSerializable(ARG_DATE, resultDate)
                }

                reqKey?.let { parentFragmentManager.setFragmentResult(it, resultBundle) }
            }

        return DatePickerDialog(
            requireContext(),
            dateListener,
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        ).apply {
            datePicker.maxDate = when (maxDate) {
                LocalDate.MAX -> Date().getTime()
                else          -> Calendar.getInstance().apply {
                    set(maxDate.year, maxDate.monthValue - 1, maxDate.dayOfMonth)
                }.timeInMillis
            }
            // Remember zero-indexed months, but not dates
            datePicker.minDate = when (minDate) {
                LocalDate.MIN -> Calendar.getInstance().apply {
                    set(1900, 0, 1)
                }.timeInMillis
                else          -> Calendar.getInstance().apply {
                    set(minDate.year, minDate.monthValue - 1, minDate.dayOfMonth)
                }.timeInMillis
            }
        }
    }

    companion object {
        fun newInstance(
            initDate: LocalDate?,
            minDate: LocalDate? = null,
            maxDate: LocalDate? = null,
            reqKey: String,
        ): DatePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DATE, initDate)
                putSerializable(ARG_MIN_DATE, minDate ?: LocalDate.MIN)
                putSerializable(ARG_MAX_DATE, maxDate ?: LocalDate.MAX)
                putSerializable(ARG_REQUEST_KEY, reqKey)
            }

            return DatePickerFragment().apply {
                arguments = args
            }
        }
    }
}