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
private const val RESULT_NEW_SERIES = 312
private const val DIALOG_NEW_SERIES = "DIALOG_EDIT_SERIES"
/**
 * A simple [Fragment] subclass.
 * Use the [SeriesDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SeriesDetailFragment(val seriesId: UUID) : Fragment() {

    private val issueListViewModel by lazy {
        ViewModelProvider(this).get(IssueListViewModel::class.java)
    }

    private lateinit var seriesNameTextView: TextView
    private lateinit var editSeriesButton: ImageButton
    private lateinit var volumeNumTextView: TextView
    private lateinit var publisherTextView: TextView
    private lateinit var dateRangeTextview: TextView
    private lateinit var descriptionLabelTextView: TextView
    private lateinit var descriptionTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "View Created")

        seriesId.let {
            issueListViewModel.loadSeries(seriesId)
        }

        issueListViewModel.seriesDetailLiveData.observe(
            viewLifecycleOwner,
            {
                Log.d(TAG, "onViewCreated: ${it?.series?.seriesName ?: "None"}")
                updateUI(it)
            }
        )

        editSeriesButton.setOnClickListener {
            val d = NewSeriesDialogFragment.newInstance(seriesId)
            d.setTargetFragment(this, RESULT_NEW_SERIES)
            d.show(parentFragmentManager, DIALOG_NEW_SERIES)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == RESULT_NEW_SERIES && data != null -> {
                val seriesId = data.getSerializableExtra(ARG_SERIES_ID) as UUID
                issueListViewModel.loadSeries(seriesId)
            }
        }
    }

    private fun updateUI(seriesDetail: SeriesDetail?) {
        Log.d(TAG, "updateUI: ${seriesDetail?.series?.seriesName ?: "None"}")
        seriesNameTextView.setText(seriesDetail?.series?.seriesName)
        volumeNumTextView.setText(seriesDetail?.series?.volume.toString())
        publisherTextView.setText(seriesDetail?.publisher)
        dateRangeTextview.setText(seriesDetail?.series?.dateRange)

        seriesDetail?.series?.description?.let {
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
        fun newInstance(seriesId: UUID) = SeriesDetailFragment(seriesId)
    }
}