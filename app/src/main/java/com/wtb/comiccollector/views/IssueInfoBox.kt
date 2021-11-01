package com.wtb.comiccollector.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.wtb.comiccollector.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class IssueInfoBox(context: Context, attrs: AttributeSet? = null) : NestedScrollView(
    context,
    attrs, R.layout.issue_detail_issue_info_box
) {

    private var releaseDate: LocalDate? = null
    private var coverDate: LocalDate? = null
    private var notes: String? = null

    val view: View = inflate(context, R.layout.issue_detail_issue_info_box, this)
    val linearLayout: LinearLayout = view.findViewById(R.id.info_wrapper)

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
                        it
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
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    constructor(context: Context, attrs: AttributeSet) : this(context, "", attrs = attrs)

    private var labelView: TextView? = null
    private var infoTextView: TextView? = null
    private var infoRowLayout: Int = R.layout.issue_detail_info_row_vertical

    init {
        val view = LayoutInflater.from(context).inflate(infoRowLayout, this)
        labelView = view.findViewById(R.id.info_row_label)
        infoTextView = view.findViewById(R.id.info_row_text)
        this.labelView?.text = label
        this.infoTextView?.text = infoText
    }
}