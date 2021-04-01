package com.wtb.comiccollector.NewGroupListFragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.NewGroupListViewModels.NewSeriesListViewModel
import com.wtb.comiccollector.R
import com.wtb.comiccollector.Series
import java.time.LocalDate

private const val TAG = APP + "NewSeriesListFragment"
const val ARG_DATE_FILTER_END: String = "End Date"
const val ARG_DATE_FILTER_START: String = "Start Date"
const val ARG_PUBLISHER_IDS: String = "Publisher Ids"
const val ARG_CREATOR_IDS: String = "Creator Ids"

class NewSeriesListFragment(var callback: Callbacks) : Fragment() {

    val viewModel: NewSeriesListViewModel by lazy {
        ViewModelProvider(this).get(NewSeriesListViewModel::class.java)
    }

    protected lateinit var seriesList: List<Series>

    private var creatorIdsFilter: MutableSet<Int>? = null
    private var publisherIdsFilter: MutableSet<Int>? = null
    private var dateFilterStart: LocalDate? = null
    private var dateFilterEnd: LocalDate? = null
    private lateinit var itemListRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        creatorIdsFilter =
            Filter.deserialize(arguments?.getSerializable(ARG_CREATOR_IDS) as String?)
        publisherIdsFilter =
            Filter.deserialize(arguments?.getSerializable(ARG_PUBLISHER_IDS) as String?)
        dateFilterStart = arguments?.getSerializable(ARG_DATE_FILTER_START) as LocalDate?
        dateFilterEnd = arguments?.getSerializable(ARG_DATE_FILTER_END) as LocalDate?
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        seriesList = emptyList()

        itemListRecyclerView = view.findViewById(R.id.results_frame) as RecyclerView
        itemListRecyclerView.layoutManager = LinearLayoutManager(context)
        itemListRecyclerView.adapter = SeriesAdapter(seriesList)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.filter(creatorIdsFilter, publisherIdsFilter, dateFilterStart, dateFilterEnd)

        viewModel.seriesListLiveData.observe(
            viewLifecycleOwner,
            { objectList ->
                objectList?.let {
                    this.seriesList = it
                    updateUI()
                }
            }
        )
    }

    private fun updateUI() {
        itemListRecyclerView.adapter = SeriesAdapter(seriesList)
        runLayoutAnimation(itemListRecyclerView)
    }

    private fun runLayoutAnimation(view: RecyclerView) {
        val context = view.context
        val controller: LayoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)

        view.layoutAnimation = controller
        view.adapter?.notifyDataSetChanged()
        view.scheduleLayoutAnimation()
    }

    inner class SeriesAdapter(var seriesList: List<Series>) : RecyclerView.Adapter<SeriesHolder>() {

        private var lastPosition = -1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesHolder {
            val view = layoutInflater.inflate(R.layout.list_item_series, parent, false)
            return getHolder(view)
        }

        override fun onBindViewHolder(holder: SeriesHolder, position: Int) {
            val item = seriesList[position]
            holder.bind(item)
        }

        override fun getItemCount(): Int = seriesList.size

        fun getHolder(view: View) = SeriesHolder(view)
    }

    inner class SeriesHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        lateinit var item: Series

        init {
            @Suppress("LeakingThis")
            itemView.setOnClickListener(this)
        }

        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
        private val seriesTextView: TextView = itemView.findViewById(R.id.list_item_name)

        private val seriesDateRangeTextView: TextView = itemView.findViewById(R.id.list_item_dates)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(item: Series) {
            this.item = item
            seriesTextView.text = this.item.seriesName
            seriesDateRangeTextView.text = this.item.dateRange
        }

        override fun onClick(v: View?) {
            Log.d(TAG, "Series Clicked")
            callback.onSeriesSelected(item)
        }
    }

    interface Callbacks {
        fun onSeriesSelected(series: Series)
    }

    companion object {
        @JvmStatic
        fun newInstance(
            callback: Callbacks,
            filter: Filter,
            dateFilterStart: LocalDate? = null,
            dateFilterEnd: LocalDate? = null
        ) =
            NewSeriesListFragment(callback).apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CREATOR_IDS, filter.creatorIds())
                    putSerializable(ARG_PUBLISHER_IDS, filter.publisherIds())
                    putSerializable(ARG_DATE_FILTER_START, dateFilterStart)
                    putSerializable(ARG_DATE_FILTER_END, dateFilterEnd)
                }
            }
    }
}