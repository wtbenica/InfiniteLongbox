package com.wtb.comiccollector.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.fragments_view_models.SeriesListViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

private const val TAG = APP + "SeriesListFragment"

@ExperimentalCoroutinesApi
class SeriesListFragment : ListFragment<FullSeries, SeriesListFragment.SeriesHolder>() {

    private var listState: Bundle? = null

    override val viewModel: SeriesListViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        callback?.setTitle()
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(context)

    override fun getAdapter(): SeriesAdapter = SeriesAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        listRecyclerView.addItemDecoration(
            ItemOffsetDecoration(resources.getDimension(R.dimen.margin_narrow).toInt()))
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateBottomPadding()

        val adapter = getAdapter()

        listRecyclerView.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.itemList.collectLatest {
                    adapter.apply {
                        submitData(it)
                    }
                }
            }
        }

        filterViewModel.filter.observe(
            viewLifecycleOwner,
            { filter ->
                viewModel.setFilter(filter)
            }
        )
    }


    //    private fun runLayoutAnimation(view: RecyclerView) {
//        val context = view.context
//        val controller: LayoutAnimationController =
//            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
//
//        view.layoutAnimation = controller
//        view.adapter?.notifyDataSetChanged()
//        view.scheduleLayoutAnimation()
//    }
//
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

        private lateinit var item: FullSeries

        private val seriesTextView: TextView =
            itemView.findViewById(R.id.list_item_series_name_text)
        private val seriesImageView: ImageView = itemView.findViewById(R.id.series_imageview)
        private val seriesDateRangeTextView: TextView =
            itemView.findViewById(R.id.list_item_pub_dates)
        private val formatTextView: TextView =
            itemView.findViewById(R.id.list_item_series_format)
        private val coverProgressBar: ProgressBar =
            itemView.findViewById(R.id.cover_progress_bar)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(item: FullSeries) {
            this.item = item

            seriesTextView.text = this.item.series.seriesName

            val firstIssueId = this.item.series.firstIssue
            if (firstIssueId != null) {
                viewModel.getIssue(firstIssueId,
                                   viewModel.filter.value?.mMyCollection?.let { !it } ?: true)
            } else if (this.item.series.issueCount < 10) {
                viewModel.updateIssuesBySeries(this@SeriesHolder.item)
            }

            val firstIssue: FullIssue? = this.item.firstIssue

            seriesImageView.setImageURI(firstIssue?.coverUri)

            seriesDateRangeTextView.text = this.item.series.dateRange
            formatTextView.text = this.item.series.publishingFormat?.lowercase()

            coverProgressBar.visibility =
                if (firstIssue == null || firstIssue.coverUri != null || firstIssue.cover != null) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }

        override fun onClick(v: View?) {
            (callback as SeriesListCallback?)?.onSeriesSelected(item)
        }
    }

    interface SeriesListCallback : ListFragment.ListFragmentCallback {
        fun onSeriesSelected(series: FullSeries)
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