package com.wtb.comiccollector

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class SeriesListFragment : Fragment() {
    interface Callbacks {
        fun onSeriesSelected(seriesId: UUID)
        fun onNewIssue(issueId: UUID)
    }

    private var callbacks: Callbacks? = null

    private val seriesListViewModel by lazy { ViewModelProvider(this).get(SeriesListViewModel::class.java) }
    private lateinit var seriesRecyclerView: RecyclerView
    private var adapter: SeriesAdapter? = SeriesAdapter(emptyList())

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_issue_list, container, false)

        seriesRecyclerView = view.findViewById(R.id.issue_recycler_view)
        seriesRecyclerView.layoutManager = LinearLayoutManager(context)
        seriesRecyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        seriesListViewModel.seriesListLiveData.value?.let { updateUI(it) }
        seriesListViewModel.seriesListLiveData.observe(
            viewLifecycleOwner,
            { series ->
                series?.let {
                    updateUI(series)
                }
            }
        )

    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_issue_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_issue -> {
                val issue = Issue(seriesId = NEW_SERIES_ID)
                seriesListViewModel.addIssue(issue)
                callbacks?.onNewIssue(issue.issueId)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(seriesList: List<Series>) {
        adapter = SeriesAdapter(seriesList)
        seriesRecyclerView.adapter = adapter
    }

    private inner class SeriesAdapter(var seriesList: List<Series>) :
        RecyclerView.Adapter<SeriesHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesHolder {
            val view = layoutInflater.inflate(R.layout.list_item_series, parent, false)
            return SeriesHolder(view)
        }

        override fun onBindViewHolder(holder: SeriesHolder, position: Int) {
            val series = seriesList[position]
            holder.bind(series)
        }

        override fun getItemCount(): Int = seriesList.size

    }

    // TODO: Make this look prettier
    private inner class SeriesHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        private lateinit var series: Series

        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val seriesTextView: TextView = itemView.findViewById(R.id.list_item_title)
        private val seriesDateRangeTextView: TextView = itemView.findViewById(R.id.list_item_dates)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(series: Series) {
            this.series = series
            seriesTextView.text = this.series.seriesName
            seriesDateRangeTextView.text = this.series.dateRange
        }

        override fun onClick(v: View?) {
            callbacks?.onSeriesSelected(series.seriesId)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SeriesListFragment()
    }
}