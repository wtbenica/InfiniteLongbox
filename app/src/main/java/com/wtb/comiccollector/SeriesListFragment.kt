package com.wtb.comiccollector

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.util.*

private const val TAG = "SeriesListFragment"

class SeriesListFragment : GroupListFragment<Series>() {

    override lateinit var recyclerView: RecyclerView

    override var callbacks: Callbacks? = null

    override var filterId: UUID? = null
    override var dateFilterStart: LocalDate? = null
    override var dateFilterEnd: LocalDate? = null

    override val viewModel by lazy {
        ViewModelProvider(this).get(SeriesListViewModel::class.java)
    }

    override lateinit var itemList: List<Series>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_issue_list, container, false)

        itemList = emptyList()

        recyclerView = view.findViewById(R.id.issue_recycler_view) as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = SeriesAdapter(itemList)

        return view
    }

    override fun updateUI() {
        recyclerView.adapter = SeriesAdapter(itemList)
        runLayoutAnimation(recyclerView)
    }

    private inner class SeriesAdapter(seriesList: List<Series>) :
        GroupListFragment.MyAdapter<Series>(itemList = seriesList) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesHolder {
            val view = layoutInflater.inflate(R.layout.list_item_series, parent, false)
            return SeriesHolder(view)
        }
    }

    private inner class SeriesHolder(view: View) : GroupListFragment.MyHolder<Series>(view),
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