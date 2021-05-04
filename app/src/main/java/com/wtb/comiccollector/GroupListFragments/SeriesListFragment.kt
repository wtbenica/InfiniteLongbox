package com.wtb.comiccollector.GroupListFragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.ARG_FILTER
import com.wtb.comiccollector.Filter
import com.wtb.comiccollector.GroupListViewModels.SeriesListViewModel
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.Series

private const val TAG = APP + "SeriesListFragment"

class SeriesListFragment(var callback: Callbacks? = null) : Fragment() {

    private val viewModel: SeriesListViewModel by lazy {
        ViewModelProvider(this).get(SeriesListViewModel::class.java)
    }

    private var seriesList: PagedList<Series>? = null

    private var filter = Filter()
    private lateinit var itemListRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        filter = arguments?.getSerializable(ARG_FILTER) as Filter
        viewModel.setFilter(filter)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        itemListRecyclerView = view.findViewById(R.id.results_frame) as RecyclerView
        itemListRecyclerView.layoutManager = LinearLayoutManager(context)
        val adapter = SeriesAdapter()
        itemListRecyclerView.adapter = adapter
        viewModel.seriesListLiveData.observe(viewLifecycleOwner, Observer(adapter::submitList))

        return view
    }

    private fun updateUI() {

    }

    private fun runLayoutAnimation(view: RecyclerView) {
        val context = view.context
        val controller: LayoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)

        view.layoutAnimation = controller
        view.adapter?.notifyDataSetChanged()
        view.scheduleLayoutAnimation()
    }

    inner class SeriesAdapter :
        PagedListAdapter<Series, SeriesHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesHolder =
            SeriesHolder(parent)

        override fun onBindViewHolder(holder: SeriesHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }
    }

    inner class SeriesHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from
            (parent.context).inflate(R.layout.list_item_series, parent, false)
    ), View.OnClickListener {

        lateinit var item: Series

        init {
            @Suppress("LeakingThis")
            itemView.setOnClickListener(this)
        }

        //        private val coverImageView: ImageView = itemView.findViewById(R.id.list_item_cover)
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
            callback?.onSeriesSelected(item)
        }
    }

    interface Callbacks {
        fun onSeriesSelected(series: Series)
    }

    companion object {
        @JvmStatic
        fun newInstance(
            callback: Callbacks,
            filter: Filter
        ) =
            SeriesListFragment(callback).apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_FILTER, filter)
                }
            }

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Series>() {
            override fun areItemsTheSame(oldItem: Series, newItem: Series): Boolean =
                oldItem.seriesId == newItem.seriesId


            override fun areContentsTheSame(oldItem: Series, newItem: Series): Boolean =
                oldItem == newItem
        }
    }
}