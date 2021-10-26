package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.SearchFilter
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.fragments.ListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = APP + "SeriesDetailBox"
private const val RESULT_SERIES_INFO = 312
private const val DIALOG_SERIES_INFO = "DIALOG_EDIT_SERIES"

@ExperimentalCoroutinesApi
class SeriesDetailBox(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs, R.attr.styleSeriesDetail, R.style.SeriesDetailBackground),
    SeriesLinkCallback {

    constructor(context: Context, series: FullSeries) : this(context, null) {
        setSeries(series)
    }

    private var listFragmentCallback: ListFragment.ListFragmentCallback? = null

    private var series: FullSeries? = null

    private var volumeNumTextView: TextView
    private var continuesFromBox: LinearLayout
    private var continuesToBox: LinearLayout
    private var continuesFrom: SeriesLink
    private var continuesAs: SeriesLink
    private var publisherTextView: TextView
    private var dateRangeTextview: TextView
    private var trackingNotesHeader: LinearLayout
    private var trackingNotesTextView: TextView
    private var trackingDropdownButton: ExpandButton
    private var notesLabelHeader: LinearLayout
    private var notesTextView: TextView
    private var notesDropdownButton: ExpandButton
    private var notesBox: LinearLayout

    private fun setSeries(series: FullSeries) {
        if (this.series != series) {
            this.series = series
            updateUI()
        }
    }


    init {
        inflate(context, R.layout.view_series_detail, this)

        volumeNumTextView = findViewById(R.id.details_series_volume)
        continuesFromBox = findViewById(R.id.continues_from_box)
        continuesToBox = findViewById(R.id.continues_to_box)
        continuesFrom = findViewById<SeriesLink>(R.id.details_continues_from).apply {
            callback = this@SeriesDetailBox
            setTextAppearance(R.style.SeriesDetailText)
        }
        continuesAs = findViewById<SeriesLink>(R.id.details_continues_as).apply {
            callback = this@SeriesDetailBox
            setTextAppearance(R.style.SeriesDetailText)
        }
        publisherTextView = findViewById(R.id.details_publisher)
        dateRangeTextview = findViewById(R.id.details_date_range)
        trackingNotesHeader = findViewById(R.id.header_tracking)
        trackingNotesTextView = findViewById(R.id.details_tracking_notes)
        trackingDropdownButton = findViewById(R.id.tracking_dropdown_button)
        notesLabelHeader = findViewById(R.id.header_notes)
        notesTextView = findViewById(R.id.details_notes)
        notesDropdownButton = findViewById(R.id.notes_dropdown_button)
        notesBox = findViewById(R.id.notes_box)

        trackingDropdownButton.setOnClickListener(null)
        trackingDropdownButton.setOnClickListener {
            trackingNotesTextView.toggleVisibility(MATCH_PARENT, WRAP_CONTENT)
            (it as ExpandButton).toggleExpand()
        }

        notesDropdownButton.setOnClickListener(null)
        notesDropdownButton.setOnClickListener {
            notesBox.toggleVisibility(MATCH_PARENT, MATCH_PARENT)
            (it as ExpandButton).toggleExpand()
        }

        listFragmentCallback = context as ListFragment.ListFragmentCallback?
    }


    private fun updateUI() {
        volumeNumTextView.text = series?.series?.volume.toString()
        publisherTextView.text = series?.publisher?.publisher
        dateRangeTextview.text = series?.series?.dateRange

        series?.series?.description.let {
            if (it != null && it != "") {
                trackingNotesTextView.text = it
                trackingNotesHeader.visibility = TextView.VISIBLE
                trackingNotesTextView.visibility = TextView.VISIBLE
            } else {
                trackingNotesHeader.visibility = TextView.GONE
                trackingNotesTextView.visibility = TextView.GONE
            }
        }

        series?.series?.notes.let {
            if (it != null && it != "") {
                notesTextView.text = it
                notesLabelHeader.visibility = TextView.VISIBLE
                notesTextView.visibility = TextView.VISIBLE
            } else {
                notesLabelHeader.visibility = TextView.GONE
                notesTextView.visibility = TextView.GONE
            }
        }

        continuesFromBox.visibility = if (series?.seriesBondFrom == null) {
            GONE
        } else {
            VISIBLE
        }

        continuesToBox.visibility = if (series?.seriesBondTo == null) {
            GONE
        } else {
            VISIBLE
        }

        series?.seriesBondTo?.targetSeries?.let { continuesAs.series = FullSeries(series = it) }
        series?.seriesBondFrom?.originSeries?.let { continuesFrom.series = FullSeries(series = it) }
    }

    override fun seriesClicked(series: FullSeries) {
        val filter = SearchFilter(series = series, myCollection = false)
        listFragmentCallback?.updateFilter(filter)
    }
}