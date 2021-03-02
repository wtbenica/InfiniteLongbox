@file:Suppress("LeakingThis")

package com.wtb.comiccollector

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.util.*

private const val TAG = "CreatorListFragment"

class CreatorListFragment : Fragment() {

    interface Callbacks {
        fun onSeriesSelected(seriesId: UUID)
        fun onCreatorSelected(creatorId: UUID)
        fun onNewIssue(issueId: UUID)
    }

    private var callbacks: Callbacks? = null

    private lateinit var creatorRecyclerView: RecyclerView

    private var seriesFilterId: UUID? = null
    private var dateFilterStart: LocalDate? = null
    private var dateFilterEnd: LocalDate? = null

    private val creatorListViewModel by lazy {
        ViewModelProvider(this).get(CreatorListViewModel::class.java)
    }

    private lateinit var creatorList: List<Creator>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        seriesFilterId = arguments?.getSerializable(ARG_SERIES_FILTER_ID) as UUID?
        dateFilterStart = arguments?.getSerializable(ARG_DATE_FILTER_START) as LocalDate?
        dateFilterEnd = arguments?.getSerializable(ARG_DATE_FILTER_END) as LocalDate?
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_issue_list, container, false)

        creatorList = emptyList()

        creatorRecyclerView = view.findViewById(R.id.issue_recycler_view) as RecyclerView
        creatorRecyclerView.layoutManager = LinearLayoutManager(context)
        creatorRecyclerView.adapter = CreatorAdapter(creatorList)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        seriesFilterId?.let { creatorListViewModel.filterByCreator(it) }

        creatorListViewModel.creatorListLiveData.observe(
            viewLifecycleOwner,
            { creatorList ->
                creatorList?.let {
                    this.creatorList = it
                    updateUI()
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
                val issue = Issue()
                creatorListViewModel.addIssue(issue)
                callbacks?.onNewIssue(issue.issueId)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI() {
        creatorRecyclerView.adapter = CreatorAdapter(creatorList)
        runLayoutAnimation(creatorRecyclerView)
    }

    private fun runLayoutAnimation(view: RecyclerView) {
        val context = view.context
        val controller: LayoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)

        view.layoutAnimation = controller
        view.adapter?.notifyDataSetChanged()
        view.scheduleLayoutAnimation()
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
                    putSerializable(ARG_SERIES_FILTER_ID, seriesFilterId)
                    putSerializable(ARG_CREATOR_FILTER, creatorFilterId)
                    putSerializable(ARG_DATE_FILTER_START, dateFilterStart)
                    putSerializable(ARG_DATE_FILTER_END, dateFilterEnd)
                }
            }
    }
}