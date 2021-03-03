package com.wtb.comiccollector

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import java.time.LocalDate
import java.util.*

private const val TAG = "SeriesListFragment"

class SeriesListFragment : GroupListFragment<Series, SeriesListFragment.SeriesAdapter>() {

    override val viewModel by lazy {
        ViewModelProvider(this).get(SeriesListViewModel::class.java)
    }

    override fun getAdapter(): SeriesAdapter = SeriesAdapter(itemList)

    inner class SeriesAdapter(seriesList: List<Series>) :
        MyAdapter<Series>(itemList = seriesList) {
        override fun getHolder(view: View) = SeriesHolder(view)
    }

    inner class SeriesHolder(view: View) : GroupListFragment.MyHolder<Series>(view),
        View.OnClickListener {

        override lateinit var item: Series

        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val seriesTextView: TextView = itemView.findViewById(R.id.list_item_name)

        private val seriesDateRangeTextView: TextView = itemView.findViewById(R.id.list_item_dates)

        init {
            itemView.setOnClickListener(this)
        }

        override fun bind(item: Series) {
            this.item = item
            seriesTextView.text = this.item.seriesName
            seriesDateRangeTextView.text = this.item.dateRange
        }

        override fun onClick(v: View?) {
            callbacks?.onSeriesSelected(item.seriesId)
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
            creatorFilterId: UUID? = null,
            dateFilterStart: LocalDate? = null,
            dateFilterEnd: LocalDate? = null
        ) =
            SeriesListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_FILTER_ID, creatorFilterId)
                    putSerializable(ARG_DATE_FILTER_START, dateFilterStart)
                    putSerializable(ARG_DATE_FILTER_END, dateFilterEnd)
                }
            }
    }
}