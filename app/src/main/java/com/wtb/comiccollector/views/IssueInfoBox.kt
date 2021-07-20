package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.ScrollView
import android.widget.TextView
import com.wtb.comiccollector.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class IssueInfoBox(context: Context, attrs: AttributeSet? = null) : ScrollView(context, attrs) {

    private var releaseDate: LocalDate? = null
    private var coverDate: LocalDate? = null
    private var notes: String? = null

    val view = inflate(context, R.layout.issue_detail_issue_info_box, null)
    val linearLayout: LinearLayout = view.findViewById(R.id.issue_info_box)

    init {
        addView(linearLayout)
    }

    fun update(releaseDate: LocalDate?, coverDate: LocalDate?, notes: String?) {
        if (this.releaseDate != releaseDate || this.coverDate != coverDate || this.notes != notes) {
            Log.d(TAG, "Update!!")
            linearLayout.removeAllViews()
            releaseDate?.let {
                linearLayout.addView(
                    IssueInfoRow(
                        context,
                        context.getString(R.string.label_release_date),
                        it.format(DateTimeFormatter.ofPattern("MMM d, YYYY"))
                    )
                )
            }
            coverDate?.let {
                linearLayout.addView(
                    IssueInfoRow(
                        context,
                        context.getString(R.string.label_cover_date),
                        it.format(DateTimeFormatter.ofPattern("MMM YYYY"))
                    )
                )
            }
            notes?.let {
                linearLayout.addView(
                    IssueInfoRow(
                        context,
                        context.getString(R.string.label_notes),
                        it,
                        VERTICAL
                    )
                )
            }

            this.releaseDate = releaseDate
            this.coverDate = coverDate
            this.notes = notes
        }
    }

    companion object {
        private const val TAG = "IssueInfoBox"
    }
}


class IssueInfoRow(
    context: Context,
    label: String? = null,
    infoText: String? = null,
    direction: Int = HORIZONTAL,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    constructor(context: Context, attrs: AttributeSet) : this(context, "", attrs = attrs)

    private var labelView: TextView? = null
    private var infoTextView: TextView? = null
    private var infoRowLayout: Int = R.layout.issue_detail_info_row

    init {
        if (direction == VERTICAL)
            infoRowLayout = R.layout.issue_detail_info_row_vertical
        val view = LayoutInflater.from(context).inflate(infoRowLayout, this)
        labelView = view.findViewById(R.id.info_row_label)
        infoTextView = view.findViewById(R.id.info_row_text)
        this.labelView?.text = label
        this.infoTextView?.text = infoText
    }
}