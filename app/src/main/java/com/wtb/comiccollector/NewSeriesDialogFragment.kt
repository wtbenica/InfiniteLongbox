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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider

private const val TAG = "NewSeriesDialogFragment"

class NewSeriesDialogFragment : DialogFragment() {
    private lateinit var listener: NewSeriesDialogListener

    private lateinit var seriesNameEditText: EditText
    private lateinit var volumeNumberEditText: EditText
    private lateinit var publisherSpinner: Spinner
    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
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
        // TODO: add issueDetailViewModel
        // TODO: add DatePickerDialogs to date pickers
        super.onAttach(context)

        try {
            listener = context as NewSeriesDialogListener
        } catch (e: ClassCastException) {
            throw java.lang.ClassCastException(("$context must implement NewSeriesDialogFragment"))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        val view = inflater.inflate(R.layout.dialog_fragment_new_series_full, container, false)

        seriesNameEditText = view.findViewById(R.id.series_title)
        volumeNumberEditText = view.findViewById(R.id.volume_num)
        publisherSpinner = view.findViewById(R.id.publisher_spinner) as Spinner
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

    override fun onStart() {
        super.onStart()

        okayButton.setOnClickListener { view ->
            val publisher = publisherSpinner.selectedItem as Publisher

            val series = Series(
                seriesName = seriesNameEditText.text.toString(),
                volume = volumeNumberEditText.text.toString().toInt(),
                publisher = publisherSpinner.selectedItem.toString(),
                publisherId = publisher.publisherId,
//                startDate = LocalDate.parse(startDateEditText.text),
//                endDate = LocalDate.parse(endDateEditText.text)
            )

            issueDetailViewModel.addSeries(series)

            val bundle = Bundle()
            bundle.putSerializable("seriesId", series.seriesId)
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

    companion object
}