package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wtb.comiccollector.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class IssueInfoBox(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    fun update(releaseDate: LocalDate?, coverDate: LocalDate?, notes: String?) {
        releaseDate?.let {
            addView(
                IssueInfoRow(
                    context,
                    context.getString(R.string.label_release_date),
                    it.format(DateTimeFormatter.ofPattern("MMMM dd, YYYY"))
                )
            )
        }
        coverDate?.let {
            addView(
                IssueInfoRow(
                    context,
                    context.getString(R.string.label_cover_date),
                    it.format(DateTimeFormatter.ofPattern("MMMM dd, YYYY"))
                )
            )
        }
        notes?.let {
            addView(
                IssueInfoRow(
                    context,
                    context.getString(R.string.label_notes),
                    it
                )
            )
        }
    }
}

class IssueInfoRow(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private var label: TextView? = null
    private var infoText: TextView? = null

    constructor(context: Context, label: String, infoText: String, orientation: Int = HORIZONTAL) : this
        (context) {
        this.label?.text = label
        this.infoText?.text = infoText
        this.orientation = orientation
    }

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.issue_detail_info_row, this)
        label = view.findViewById(R.id.info_row_label)
        infoText = view.findViewById(R.id.info_row_text)
    }
}