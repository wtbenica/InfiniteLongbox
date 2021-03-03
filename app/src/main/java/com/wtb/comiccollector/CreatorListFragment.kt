package com.wtb.comiccollector

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import java.time.LocalDate
import java.util.*

private const val TAG = "CreatorListFragment"

class CreatorListFragment : GroupListFragment<Creator, CreatorListFragment.CreatorAdapter>() {

    override val viewModel by lazy {
        ViewModelProvider(this).get(CreatorListViewModel::class.java)
    }

    override fun getAdapter(): CreatorAdapter = CreatorAdapter(itemList)

    inner class CreatorAdapter(creatorlist: List<Creator>) :
        MyAdapter<Creator>(itemList = creatorlist) {
        override fun getHolder(view: View): MyHolder<Creator> = CreatorHolder(view)
    }

    inner class CreatorHolder(view: View) : GroupListFragment.MyHolder<Creator>(view) {
        override lateinit var item: Creator

        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val nameTextView: TextView = itemView.findViewById(R.id.list_item_name)

        override fun onClick(v: View?) {
            callbacks?.onCreatorSelected(item.creatorId)
        }

        override fun bind(item: Creator) {
            this.item = item
            nameTextView.text = this.item.sortName
        }

    }

    enum class SeriesFilter(val s: String, val onSelect: (viewModel: SeriesListViewModel) -> Unit) {
        NONE("None", { }),
        CREATOR("Creator", { }),
        DATE("Date Range", { });

        override fun toString(): String {
            return s
        }
    }

    enum class CreatorFilter(val s: String, val onSelect: () -> Unit) {
        NONE("None", { }),
        COCREATOR("Cocreator", { }),
        DATE("Date Range", { });

        override fun toString(): String {
            return s
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            seriesFilterId: UUID? = null,
            creatorFilterId: UUID? = null,
            dateFilterStart: LocalDate? = null,
            dateFilterEnd: LocalDate? = null
        ) =
            SeriesListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_FILTER_ID, seriesFilterId)
                    putSerializable(ARG_CREATOR_FILTER, creatorFilterId)
                    putSerializable(ARG_DATE_FILTER_START, dateFilterStart)
                    putSerializable(ARG_DATE_FILTER_END, dateFilterEnd)
                }
            }
    }
}