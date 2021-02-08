package com.wtb.comiccollector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val TAG = "NewSeriesDialogFragment"

private const val RESULT_START_DATE = 33
private const val RESULT_END_DATE = 34

private const val DIALOG_START_DATE = "DialogStartDate"
private const val DIALOG_END_DATE = "DialogEndDate"

const val ARG_SERIES_ID = "seriesId"
const val ARG_SERIES_NAME = "Series Name"

class NewSeriesDialogFragment private constructor() : DialogFragment(),
    DatePickerFragment.Callbacks {

    private val issueListViewModel by lazy {
        ViewModelProvider(this).get(IssueListViewModel::class.java)
    }

    private lateinit var listener: NewSeriesDialogListener

    private lateinit var seriesName: String

    private lateinit var seriesNameEditText: EditText
    private lateinit var volumeNumberEditText: EditText
    private lateinit var publisherSpinner: Spinner
    private lateinit var startDateEditText: TextView
    private lateinit var endDateEditText: TextView
    private lateinit var okayButton: Button
    private lateinit var cancelButton: Button

    private val issueDetailViewModel: IssueDetailViewModel by lazy {
        ViewModelProvider(this).get(IssueDetailViewModel::class.java)
    }

    interface NewSeriesDialogListener {
        fun onSaveSeriesClick(dialog: DialogFragment, series: Series)
        fun onCancelClick(dialog: DialogFragment)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as NewSeriesDialogListener
        } catch (e: ClassCastException) {
            throw java.lang.ClassCastException(("$context must implement NewSeriesDialogFragment"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seriesName = arguments?.getSerializable(ARG_SERIES_NAME) as String
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_fragment_new_series, container, false)

        seriesNameEditText = view.findViewById(R.id.series_title)
        seriesNameEditText.setText(seriesName)
        seriesNameEditText.requestFocus()
        volumeNumberEditText = view.findViewById(R.id.volume_num)
        publisherSpinner = view.findViewById(R.id.publisher_spinner) as Spinner
        startDateEditText = view.findViewById(R.id.start_date_text_view) as TextView
        endDateEditText = view.findViewById(R.id.end_date_text_view) as TextView
        okayButton = view.findViewById(R.id.button2) as Button
        cancelButton = view.findViewById(R.id.button)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")


        issueDetailViewModel.allPublishersLiveData.observe(viewLifecycleOwner,
            { publisherList ->
                publisherList?.let {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        publisherList
                    )

                    publisherSpinner.adapter = adapter
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == RESULT_START_DATE && data != null -> {
                startDateEditText.text =
                    (data.getSerializableExtra(ARG_DATE) as LocalDate).toString()
            }
            requestCode == RESULT_END_DATE && data != null -> {
                endDateEditText.text = (data.getSerializableExtra(ARG_DATE) as LocalDate).toString()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        startDateEditText.setOnClickListener {
            DatePickerFragment.newInstance(LocalDate.now()).apply {
                setTargetFragment(this@NewSeriesDialogFragment, RESULT_START_DATE)
                show(this@NewSeriesDialogFragment.parentFragmentManager, DIALOG_START_DATE)
            }
        }

        endDateEditText.setOnClickListener {
            DatePickerFragment.newInstance(LocalDate.now()).apply {
                setTargetFragment(this@NewSeriesDialogFragment, RESULT_END_DATE)
                show(this@NewSeriesDialogFragment.parentFragmentManager, DIALOG_END_DATE)
            }
        }

        okayButton.setOnClickListener { view ->
            val publisher = publisherSpinner.selectedItem as Publisher

            val series = Series(
                seriesName = seriesNameEditText.text.toString(),
                volume = volumeNumberEditText.text.toString().toInt(),
                publisherId = publisher.publisherId,
                startDate = try {
                    LocalDate.parse(startDateEditText.text)
                } catch (e: DateTimeParseException) {
                    null
                },
                endDate = try {
                    LocalDate.parse(endDateEditText.text)
                } catch (e: DateTimeParseException) {
                    null
                }
            )

            issueDetailViewModel.addSeries(series)

            val bundle = Bundle()
            bundle.putSerializable(ARG_SERIES_ID, series.seriesId)
            val intent = Intent().putExtras(bundle)
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)

            listener.onSaveSeriesClick(this, series)
        }

        cancelButton.setOnClickListener { view ->
            listener.onCancelClick(this)
        }
    }

    override fun onResume() {
        super.onResume()

        val window = dialog?.window
        val size = Point()

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            context?.display?.getRealSize(size)
        } else {
            @Suppress("DEPRECATION")
            window?.windowManager?.defaultDisplay?.getRealSize(size)
        }

        window?.setLayout((size.x * .9).toInt(), (size.y * .9).toInt())
        window?.setGravity(Gravity.CENTER)
    }

    override fun onDateSelected(date: LocalDate) {

    }

    companion object {
        @JvmStatic
        fun newInstance(seriesName: String = ""): NewSeriesDialogFragment {
            return NewSeriesDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_SERIES_NAME, seriesName)
                }
            }
        }
    }
}