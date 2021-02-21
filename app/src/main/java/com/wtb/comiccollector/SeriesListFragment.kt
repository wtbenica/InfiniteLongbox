@file:Suppress("LeakingThis")

package com.wtb.comiccollector

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*


class SeriesListFragment : Fragment() {
    interface Callbacks {
        fun onSeriesSelected(seriesId: UUID)
        fun onCreatorSelected(creatorId: UUID)
        fun onNewIssue(issueId: UUID)
    }

    private var callbacks: Callbacks? = null

    private val seriesListViewModel by lazy { ViewModelProvider(this).get(SeriesListViewModel::class.java) }
    private lateinit var seriesRecyclerView: RecyclerView
    private lateinit var groupingSpinner: Spinner
    private lateinit var groupingRow: LinearLayout
    private var grouping: Grouping = Grouping.SERIES
    private lateinit var adapter: MyAdapter<*>

    private lateinit var seriesList: List<Series>
    private lateinit var creatorList: List<Creator>

    private fun getAdapter(): MyAdapter<*> {
        return when (grouping) {
            Grouping.SERIES -> SeriesAdapter(seriesList)
            Grouping.CREATOR -> CreatorAdapter(creatorList)
        }
    }

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

        seriesList = emptyList()
        creatorList = emptyList()
        adapter = getAdapter()

        seriesRecyclerView = view.findViewById(R.id.issue_recycler_view) as RecyclerView
        seriesRecyclerView.layoutManager = LinearLayoutManager(context)
        seriesRecyclerView.adapter = adapter

        groupingRow = view.findViewById(R.id.grouping_row) as LinearLayout
        groupingSpinner = view.findViewById(R.id.grouping_spinner) as Spinner

        groupingRow.visibility = View.VISIBLE


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        seriesListViewModel.seriesListLiveData.observe(
            viewLifecycleOwner,
            { seriesList ->
                seriesList?.let {
                    this.seriesList = it
                    updateUI()
                }
            }
        )

        seriesListViewModel.creatorListLiveData.observe(
            viewLifecycleOwner,
            { creatorList ->
                creatorList?.let {
                    this.creatorList = it
                    updateUI()
                }
            }
        )

        groupingSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (parent?.selectedItem) {
                    Grouping.SERIES.s -> {
                        grouping = Grouping.SERIES
                    }
                    Grouping.CREATOR.s -> {
                        grouping = Grouping.CREATOR
                    }
                }
                updateUI()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
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
                val issue = Issue()
                seriesListViewModel.addIssue(issue)
                callbacks?.onNewIssue(issue.issueId)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI() {
        adapter = getAdapter()
        seriesRecyclerView.adapter = adapter
    }

    private abstract inner class MyAdapter<T>(var itemList: List<T>) :
        RecyclerView.Adapter<MyHolder<T>>() {

        private var lastPosition = -1

        override fun onBindViewHolder(holder: MyHolder<T>, position: Int) {
            val item = itemList[position]
            holder.bind(item)
        }

        override fun getItemCount(): Int = itemList.size
    }

    private inner class SeriesAdapter(seriesList: List<Series>) :
        MyAdapter<Series>(itemList = seriesList) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesHolder {
            val view = layoutInflater.inflate(R.layout.list_item_series, parent, false)
            return SeriesHolder(view)
        }
    }

    private inner class CreatorAdapter(creatorlist: List<Creator>) :
        MyAdapter<Creator>(itemList = creatorlist) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreatorHolder {
            val view = layoutInflater.inflate(R.layout.list_item_series, parent, false)
            return CreatorHolder(view)
        }
    }

    private abstract inner class MyHolder<T>(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        abstract var item: T

        init {
            itemView.setOnClickListener(this)
        }

        abstract fun bind(item: T)
    }

    // TODO: Make this look prettier
    private inner class SeriesHolder(view: View) : MyHolder<Series>(view),
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

    private inner class CreatorHolder(view: View) : MyHolder<Creator>(view) {
        override lateinit var item: Creator

        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val seriesTextView: TextView = itemView.findViewById(R.id.list_item_name)

        override fun onClick(v: View?) {
            callbacks?.onCreatorSelected(item.creatorId)
        }

        override fun bind(item: Creator) {
            this.item = item
            seriesTextView.text = this.item.sortName
        }

    }

    enum class Grouping(val s: String) {
        SERIES("Series"), CREATOR("Creator")
    }

    companion object {
        @JvmStatic
        fun newInstance() = SeriesListFragment()
    }
}