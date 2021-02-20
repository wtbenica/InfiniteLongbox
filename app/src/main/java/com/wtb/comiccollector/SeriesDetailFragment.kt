package com.wtb.comiccollector

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.util.*

private const val TAG = "SeriesDetailFragment"
private const val RESULT_SERIES_INFO = 312
private const val DIALOG_SERIES_INFO = "DIALOG_EDIT_SERIES"

/**
 * A simple [Fragment] subclass.
 * Use the [SeriesDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SeriesDetailFragment() : Fragment() {

    private val seriesViewModel by lazy {
        ViewModelProvider(this).get(SeriesInfoViewModel::class.java)
    }

    private lateinit var seriesId: UUID
    private lateinit var series: Series
    private lateinit var publisher: Publisher

    private lateinit var seriesNameTextView: TextView
    private lateinit var editSeriesButton: ImageButton
    private lateinit var volumeNumTextView: TextView
    private lateinit var publisherTextView: TextView
    private lateinit var dateRangeTextview: TextView
    private lateinit var descriptionLabelTextView: TextView
    private lateinit var descriptionTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        seriesId = arguments?.getSerializable(ARG_SERIES_ID) as UUID
        series = Series()
        publisher = Publisher()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_series_detail, container, false)
        // Inflate the layout for this fragment
        seriesNameTextView = view.findViewById(R.id.details_series_name)
        editSeriesButton = view.findViewById(R.id.details_edit_series_info)
        volumeNumTextView = view.findViewById(R.id.details_series_volume)
        publisherTextView = view.findViewById(R.id.details_publisher)
        dateRangeTextview = view.findViewById(R.id.details_date_range)
        descriptionLabelTextView = view.findViewById(R.id.label_description)
        descriptionTextView = view.findViewById(R.id.details_description)

        seriesViewModel.loadSeries(seriesId)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "View Created")

        seriesViewModel.seriesLiveData.observe(
            viewLifecycleOwner,
            {
                Log.d(TAG, "onViewCreated: ${it?.seriesName ?: "None"}")
                it?.let { series = it }
                seriesViewModel.loadPublisher(series.publisherId)
                updateUI()
            }
        )

        seriesViewModel.publisherLiveData.observe(
            viewLifecycleOwner,
            {
                it?.let { publisher = it }
                updateUI()
            }
        )

        editSeriesButton.setOnClickListener {
            val d = SeriesInfoDialogFragment.newInstance(seriesId)
            d.setTargetFragment(this, RESULT_SERIES_INFO)
            d.show(parentFragmentManager, DIALOG_SERIES_INFO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == RESULT_SERIES_INFO && data != null -> {
                val seriesId = data.getSerializableExtra(ARG_SERIES_ID) as UUID
                seriesViewModel.loadSeries(seriesId)
            }
        }
    }

    private fun updateUI() {
        Log.d(TAG, "updateUI: ${series.seriesName}")
        seriesNameTextView.text = series.seriesName
        volumeNumTextView.text = series.volume.toString()
        publisherTextView.text = publisher.publisher
        dateRangeTextview.text = series.dateRange

        series.description?.let {
            descriptionTextView.text = it
            descriptionLabelTextView.visibility = TextView.VISIBLE
            descriptionTextView.visibility = TextView.VISIBLE
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment SeriesDetailFragment.
         */
        @JvmStatic
        fun newInstance(seriesId: UUID) = SeriesDetailFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_SERIES_ID, seriesId)
            }
        }
    }
}