package com.wtb.comiccollector.item_lists.fragments

import android.net.Uri
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
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.*
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.item_lists.view_models.SeriesListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = APP + "SeriesListFragment"

@ExperimentalCoroutinesApi
class SeriesListFragment(var callback: SeriesListCallback? = null) : Fragment() {

    private val viewModel: SeriesListViewModel by lazy {
        ViewModelProvider(this).get(SeriesListViewModel::class.java)
    }

    private var filter = SearchFilter()
        set(value) {
            Log.d(TAG, "** Setting filter $value")
            field = value
            viewModel.setFilter(filter)
        }
    private lateinit var itemListRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Creating")
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        filter = arguments?.getSerializable(ARG_FILTER) as SearchFilter
        Log.d(TAG, "Loaded $filter")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        itemListRecyclerView = view.findViewById(R.id.results_frame) as RecyclerView
        itemListRecyclerView.layoutManager = LinearLayoutManager(context)

        val itemDecoration = ItemOffsetDecoration(dpToPx(requireContext(), 8F).toInt())
        itemListRecyclerView.addItemDecoration(itemDecoration)

        (requireActivity() as MainActivity).supportActionBar?.title = requireContext()
            .applicationInfo.loadLabel(requireContext().packageManager).toString()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SeriesAdapter()
        itemListRecyclerView.adapter = adapter

        lifecycleScope.launch {
            viewModel.seriesList.collectLatest {
                adapter.submitData(it) }
        }
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
        PagingDataAdapter<FullSeries, SeriesHolder>(DIFF_CALLBACK) {

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

        lateinit var item: FullSeries

        init {
            itemView.setOnClickListener(this)
        }

        private val seriesTextView: TextView = itemView.findViewById(R.id.list_item_name)
        private val seriesImageView: ImageView = itemView.findViewById(R.id.series_imageview)
        private val seriesDateRangeTextView: TextView = itemView.findViewById(R.id.list_item_dates)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(item: FullSeries) {
            this.item = item
            seriesTextView.text = this.item.series.seriesName

            val uri: Uri? = this.item.firstIssue?.coverUri

            uri.let { seriesImageView.setImageURI(it) }

            seriesDateRangeTextView.text = this.item.series.dateRange
        }

        override fun onClick(v: View?) {
            callback?.onSeriesSelected(item.series)
        }
    }

    interface SeriesListCallback {
        fun onSeriesSelected(series: Series)
    }

    companion object {
        @JvmStatic
        fun newInstance(
            callback: SeriesListCallback,
            filter: SearchFilter
        ): SeriesListFragment {
            return SeriesListFragment(callback).apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_FILTER, filter)
                }
            }
        }

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FullSeries>() {
            override fun areItemsTheSame(oldItem: FullSeries, newItem: FullSeries): Boolean =
                oldItem.series.seriesId == newItem.series.seriesId


            override fun areContentsTheSame(oldItem: FullSeries, newItem: FullSeries): Boolean =
                oldItem == newItem
        }
    }
}