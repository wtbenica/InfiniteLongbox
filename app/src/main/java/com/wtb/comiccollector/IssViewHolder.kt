package com.wtb.comiccollector

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.database.models.FullIssue

class IssViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.list_item_issue, parent, false)
) {
    private var fullIssue: FullIssue? = null
        private set

    private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
    private val issueNumTextView: TextView = itemView.findViewById(R.id.list_item_issue)

    fun bind(issue: FullIssue?) {
        this.fullIssue = issue
        this.coverImageView.scaleType = ImageView.ScaleType.FIT_CENTER
        val coverUri = this.fullIssue?.coverUri
        if (coverUri != null) {
            this.coverImageView.setImageURI(coverUri)
        } else {
            coverImageView.setImageResource(R.drawable.ic_issue_add_cover)
        }

        issueNumTextView.text = this.fullIssue?.issue.toString()
    }

}