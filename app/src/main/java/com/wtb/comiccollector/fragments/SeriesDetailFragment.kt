package com.wtb.comiccollector.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.ARG_SERIES_ID
import com.wtb.comiccollector.R
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.fragments_view_models.SeriesInfoViewModel
import com.wtb.comiccollector.views.SeriesLink
import com.wtb.comiccollector.views.SeriesLinkCallback
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "SeriesDetailFragment"
private const val RESULT_SERIES_INFO = 312
private const val DIALOG_SERIES_INFO = "DIALOG_EDIT_SERIES"

/**
 * A simple [Fragment] subclass.
 * Use the [SeriesDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@ExperimentalCoroutinesApi
class SeriesDetailFragment : Fragment(), SeriesLinkCallback {

    private var listFragmentCallback: ListFragment.ListFragmentCallback? = null
    private val seriesViewModel: SeriesInfoViewModel by viewModels()

    private var seriesId: Int? = null
    private lateinit var series: FullSeries

    private lateinit var volumeNumTextView: TextView
    private lateinit var continuesFromBox: LinearLayout
    private lateinit var continuesToBox: LinearLayout
    private lateinit var continuesFrom: SeriesLink
    private lateinit var continuesAs: SeriesLink
    private lateinit var publisherTextView: TextView
    private lateinit var dateRangeTextview: TextView
    private lateinit var trackingNotesHeader: LinearLayout
    private lateinit var trackingNotesTextView: TextView
    private lateinit var trackingDropdownButton: ImageButton
    private lateinit var notesLabelHeader: LinearLayout
    private lateinit var notesTextView: TextView
    private lateinit var notesDropdownButton: ImageButton
    private lateinit var notesBox: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        seriesId = arguments?.getSerializable(ARG_SERIES_ID) as Int?
        series = FullSeries()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_series_detail, container, false)

        volumeNumTextView = view.findViewById(R.id.details_series_volume)
        continuesFromBox = view.findViewById(R.id.continues_from_box)
        continuesToBox = view.findViewById(R.id.continues_to_box)
        continuesFrom = view.findViewById<SeriesLink>(R.id.details_continues_from).apply {
            callback = this@SeriesDetailFragment
            setTextAppearance(R.style.SeriesDetailText)
        }
        continuesAs = view.findViewById<SeriesLink>(R.id.details_continues_as).apply {
            callback = this@SeriesDetailFragment
            setTextAppearance(R.style.SeriesDetailText)
        }
        publisherTextView = view.findViewById(R.id.details_publisher)
        dateRangeTextview = view.findViewById(R.id.details_date_range)
        trackingNotesHeader = view.findViewById(R.id.header_tracking)
        trackingNotesTextView = view.findViewById(R.id.details_tracking_notes)
        trackingDropdownButton = view.findViewById(R.id.tracking_dropdown_button)
        notesLabelHeader = view.findViewById(R.id.header_notes)
        notesTextView = view.findViewById(R.id.details_notes)
        notesDropdownButton = view.findViewById(R.id.notes_dropdown_button)
        notesBox = view.findViewById(R.id.notes_box)

        trackingDropdownButton.setOnClickListener {
            trackingNotesTextView.toggleVisibility()
            (it as ImageButton).toggleIcon(trackingNotesTextView)
        }

        notesDropdownButton.setOnClickListener {
            notesBox.toggleVisibility()
            (it as ImageButton).toggleIcon(notesBox)
        }

        seriesId?.let { seriesViewModel.loadSeries(it) }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        seriesViewModel.seriesLiveData.observe(
            viewLifecycleOwner,
            {
                it?.let {
                    series = it
                    continuesFrom.series = it.seriesBondFrom?.originSeries
                    continuesAs.series = it.seriesBondTo?.targetSeries
                }
                updateUI()
            }
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listFragmentCallback = context as ListFragment.ListFragmentCallback?
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

    override fun onDetach() {
        super.onDetach()

        listFragmentCallback = null
    }

    private fun updateUI() {
        volumeNumTextView.text = series.series.volume.toString()
        publisherTextView.text = series.publisher.publisher
        dateRangeTextview.text = series.series.dateRange

        series.series.description.let {
            if (it != null && it != "") {
                trackingNotesTextView.text = it
                trackingNotesHeader.visibility = TextView.VISIBLE
                trackingNotesTextView.visibility = TextView.VISIBLE
            } else {
                trackingNotesHeader.visibility = TextView.GONE
                trackingNotesTextView.visibility = TextView.GONE
            }
        }

        series.series.notes.let {
            if (it != null && it != "") {
                notesTextView.text = it
                notesLabelHeader.visibility = TextView.VISIBLE
                notesTextView.visibility = TextView.VISIBLE
            } else {
                notesLabelHeader.visibility = TextView.GONE
                notesTextView.visibility = TextView.GONE
            }
        }

        continuesFromBox.visibility = if (series.seriesBondFrom == null) {
            GONE
        } else {
            VISIBLE
        }

        continuesToBox.visibility = if (series.seriesBondTo == null) {
            GONE
        } else {
            VISIBLE
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

    override fun seriesClicked(series: Series) {
        val filter = SearchFilter(series = series, myCollection = false)
        listFragmentCallback?.updateFilter(filter)
    }
}