package com.wtb.comiccollector

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.time.LocalDate

const val ARG_DATE = "date"

class DatePickerFragment : DialogFragment() {

    //    interface DatePickerCallback {
//        fun onDateSelected(date: LocalDate)
//    }
//
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        var date: LocalDate = LocalDate.now()
        var reqKey: String? = null

        arguments?.let { bundle ->
            bundle.getSerializable(ARG_DATE)?.let {
                date = it as LocalDate
            }
            bundle.getCharSequence("request_key")?.let {
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
        )
    }

    companion object {
        fun newInstance(date: LocalDate?): DatePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }

            return DatePickerFragment().apply {
                arguments = args
            }
        }
    }
}