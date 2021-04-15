package com.wtb.comiccollector

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.wtb.comiccollector.database.models.Publisher
import com.wtb.comiccollector.database.models.Series

private const val TAG = "SeriesDetailFragment"
private const val RESULT_SERIES_INFO = 312
private const val DIALOG_SERIES_INFO = "DIALOG_EDIT_SERIES"

/**
 * A simple [Fragment] subclass.
 * Use the [SeriesDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SeriesDetailFragment : Fragment() {

    private val seriesViewModel by lazy {
        ViewModelProvider(this).get(SeriesInfoViewModel::class.java)
    }

    private var seriesId: Int? = null
    private lateinit var series: Series
    private lateinit var publisher: Publisher

    private lateinit var seriesNameTextView: TextView
    private lateinit var volumeNumTextView: TextView
    private lateinit var publisherTextView: TextView
    private lateinit var dateRangeTextview: TextView
    private lateinit var descriptionLabelTextView: TextView
    private lateinit var descriptionTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        seriesId = arguments?.getSerializable(ARG_SERIES_ID) as Int?
        series = Series()
        publisher = Publisher()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_series_detail, container, false)

        seriesNameTextView = view.findViewById(R.id.details_series_name)
        volumeNumTextView = view.findViewById(R.id.details_series_volume)
        publisherTextView = view.findViewById(R.id.details_publisher)
        dateRangeTextview = view.findViewById(R.id.details_date_range)
        descriptionLabelTextView = view.findViewById(R.id.label_description)
        descriptionTextView = view.findViewById(R.id.details_description)

        seriesId?.let { seriesViewModel.loadSeries(it) }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        seriesViewModel.seriesLiveData.observe(
            viewLifecycleOwner,
            {
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == RESULT_SERIES_INFO && data != null -> {
                val seriesId = data.getSerializableExtra(ARG_SERIES_ID) as Int
                seriesViewModel.loadSeries(seriesId)
            }
        }
    }

    private fun updateUI() {
        Log.d(TAG, "updateUI: ${series.seriesName} ${publisher.publisher} ${series.volume}")
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
        fun newInstance(seriesId: Int?) = SeriesDetailFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_SERIES_ID, seriesId)
            }
        }
    }
}