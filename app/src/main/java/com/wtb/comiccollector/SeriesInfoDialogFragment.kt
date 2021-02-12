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
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import java.time.LocalDate
import java.util.*

private const val TAG = "NewSeriesDialogFragment"

private const val RESULT_START_DATE = 33
private const val RESULT_END_DATE = 34

private const val DIALOG_START_DATE = "DialogStartDate"
private const val DIALOG_END_DATE = "DialogEndDate"

const val ARG_SERIES_ID = "seriesId"

class SeriesInfoDialogFragment private constructor() : DialogFragment(),
    DatePickerFragment.Callbacks {

    private val seriesViewModel by lazy {
        ViewModelProvider(this).get(SeriesInfoViewModel::class.java)
    }

    private lateinit var listener: SeriesInfoDialogListener
    private lateinit var series: Series
    private lateinit var publisher: Publisher

    private lateinit var seriesNameEditText: EditText
    private lateinit var volumeNumberEditText: EditText
    private lateinit var publisherSpinner: Spinner
    private lateinit var startDateEditText: TextView
    private lateinit var endDateEditText: TextView
    private lateinit var okayButton: Button

    private lateinit var cancelButton: Button

    interface SeriesInfoDialogListener {
        fun onSaveSeriesClick(dialog: DialogFragment, series: Series)
        fun onCancelClick(dialog: DialogFragment)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as SeriesInfoDialogListener
        } catch (e: ClassCastException) {
            throw java.lang.ClassCastException(("$context must implement NewSeriesDialogFragment"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        series = Series()
        publisher = Publisher()

        seriesViewModel.loadSeries(arguments?.getSerializable(ARG_SERIES_ID) as UUID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_fragment_new_series, container, false)

        seriesNameEditText = view.findViewById(R.id.series_title)
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

        seriesViewModel.allPublishersLiveData.observe(
            viewLifecycleOwner,
            { publisherList ->
                publisherList?.let {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        publisherList
                    )

                    publisherSpinner.adapter = adapter
                }
            }
        )

        seriesViewModel.seriesLiveData.observe(
            viewLifecycleOwner,
            {
                it?.let {
                    series = it
                    updateUI()
                }
            }
        )

        seriesViewModel.publisherLiveData.observe(
            viewLifecycleOwner,
            {
                it?.let {
                    publisher = it
                    updateUI()
                }
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == RESULT_START_DATE && data != null -> {
                series.startDate = data.getSerializableExtra(ARG_DATE) as LocalDate
            }
            requestCode == RESULT_END_DATE && data != null -> {
                series.endDate = data.getSerializableExtra(ARG_DATE) as LocalDate
            }
        }
        updateUI()
    }

    override fun onStart() {
        super.onStart()

        seriesNameEditText.addTextChangedListener(
            SimpleTextWatcher {
                series.seriesName = it.toString()
            }
        )

        volumeNumberEditText.addTextChangedListener(
            SimpleTextWatcher {
                series.volume = try {
                    it.toString().toInt()
                } catch (e: Exception) {
                    1
                }
            }
        )

        startDateEditText.setOnClickListener {
            DatePickerFragment.newInstance(LocalDate.now()).apply {
                setTargetFragment(this@SeriesInfoDialogFragment, RESULT_START_DATE)
                show(this@SeriesInfoDialogFragment.parentFragmentManager, DIALOG_START_DATE)
            }
        }

        endDateEditText.setOnClickListener {
            DatePickerFragment.newInstance(LocalDate.now()).apply {
                setTargetFragment(this@SeriesInfoDialogFragment, RESULT_END_DATE)
                show(this@SeriesInfoDialogFragment.parentFragmentManager, DIALOG_END_DATE)
            }
        }

        publisherSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                parent?.let {
                    publisher = it.getItemAtPosition(position) as Publisher
                    series.publisherId = publisher.publisherId
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        okayButton.setOnClickListener { view ->
            seriesViewModel.updateSeries(series)

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

        window?.setLayout((size.x * .9).toInt(), WRAP_CONTENT)
        window?.setGravity(Gravity.CENTER)
    }

    override fun onDateSelected(date: LocalDate) {

    }

    private fun updateUI() {
        seriesNameEditText.setText(series.seriesName)
        volumeNumberEditText.setText(series.volume.toString())
        startDateEditText.text = series.startDate.toString()
        endDateEditText.text = series.endDate.toString()
    }

    companion object {
        @JvmStatic
        fun newInstance(seriesId: UUID): SeriesInfoDialogFragment {
            return SeriesInfoDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_SERIES_ID, seriesId)
                }
            }
        }
    }
}