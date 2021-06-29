package com.wtb.comiccollector.fragments

import android.content.Context
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.database.models.Series
import com.wtb.comiccollector.fragments_view_models.FilterViewModel
import com.wtb.comiccollector.fragments_view_models.SeriesListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = APP + "SeriesListFragment"

@ExperimentalCoroutinesApi
class SeriesListFragment() : Fragment() {

    private val viewModel: SeriesListViewModel by viewModels()
    private val filterViewModel: FilterViewModel by viewModels({ requireActivity() })

    private lateinit var itemListRecyclerView: RecyclerView
    private var callback: SeriesListCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "ON ATTACH")
        callback = context as SeriesListCallback?

    }

    override fun onResume() {
        super.onResume()

        callback?.setTitle()
        callback?.setToolbarScrollFlags(
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        lifecycleScope.launch {
            filterViewModel.filter.collectLatest { filter ->
                viewModel.setFilter(filter)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        itemListRecyclerView = view.findViewById(R.id.results_frame) as RecyclerView
        itemListRecyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SeriesAdapter()
        itemListRecyclerView.adapter = adapter

        lifecycleScope.launch {
            viewModel.seriesList.collectLatest {
                adapter.submitData(it)
            }
        }
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
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_series, parent, false)
    ), View.OnClickListener {

        lateinit var item: FullSeries

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

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    interface SeriesListCallback : IssueListFragment.ListFragmentCallback {
        fun onSeriesSelected(series: Series)
    }

    companion object {
        @JvmStatic
        fun newInstance() = SeriesListFragment()

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FullSeries>() {
            override fun areItemsTheSame(oldItem: FullSeries, newItem: FullSeries): Boolean =
                oldItem.series.seriesId == newItem.series.seriesId


            override fun areContentsTheSame(oldItem: FullSeries, newItem: FullSeries): Boolean =
                oldItem == newItem
        }
    }
}