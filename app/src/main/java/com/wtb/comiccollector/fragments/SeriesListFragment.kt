package com.wtb.comiccollector.fragments

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.comiccollector.APP
import com.wtb.comiccollector.MainActivity
import com.wtb.comiccollector.R
import com.wtb.comiccollector.database.models.FullIssue
import com.wtb.comiccollector.database.models.FullSeries
import com.wtb.comiccollector.fragments_view_models.SeriesListViewModel
import com.wtb.comiccollector.views.FitTopImageView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

private const val TAG = APP + "SeriesListFragment"

@ExperimentalCoroutinesApi
class SeriesListFragment : ListFragment<FullSeries, SeriesListFragment.SeriesHolder>() {

    override val viewModel: SeriesListViewModel by viewModels()
    override val minColSizeDp: Int
        get() = 600

    override fun onResume() {
        super.onResume()
        callback?.setTitle()
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager =
        GridLayoutManager(context, numCols)

    override fun getAdapter(): SeriesAdapter = SeriesAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        val itemOffsetDecoration = ItemOffsetDecoration(
            itemOffset = resources.getDimension(R.dimen.item_offset_vert_list_item_series).toInt(),
            itemOffsetHorizontal = resources.getDimension(R.dimen.item_offset_horz_list_item_series)
                .toInt()
        )

        val itemOffsetDecoration2 = ItemOffsetDecoration(
            itemOffset = resources.getDimension(R.dimen.item_offset_vert_list_item_series).toInt(),
            itemOffsetHorizontal = resources.getDimension(R.dimen.item_offset_horz_list_item_series)
                .toInt(),
            numCols = numCols
        )

        listRecyclerView.addItemDecoration(itemOffsetDecoration)

        return view
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

        private var item: FullSeries? = null
        private var coverJob: Job? = null
        private val seriesTextView: TextView =
            itemView.findViewById(R.id.list_item_series_name_text)
        private val seriesImageView: FitTopImageView = itemView.findViewById(R.id.series_imageview)
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
            if (item != this.item) {
                val defaultBgId: Int? = context?.getDrawableFromAttr(R.attr.listItemBackground)
                val defaultBg: Drawable? =
                    defaultBgId?.let { ResourcesCompat.getDrawable(resources, it, null) }
                updateCoverView(null, defaultBg)

                this.item = item
                this.item?.let {
                    coverJob?.cancel("New item to bind")
                    seriesTextView.text = it.series.seriesName

                    val firstIssueId = it.series.firstIssue
                    if (firstIssueId != null) {
                        viewModel.getIssue(firstIssueId,
                            viewModel.filter.value?.mMyCollection?.let { !it } ?: true)
                    }

                    viewModel.updateIssuesBySeries(it)

                    val firstIssue: FullIssue? = it.firstIssue


                    firstIssue?.let {
                        coverJob = CoroutineScope(Dispatchers.Default).launch {
                            viewModel.getIssueCoverFlow(it.issue.issueId).collectLatest {
                                it?.let { cover ->
                                    (context as MainActivity).runOnUiThread {
                                        updateCoverView(cover.coverUri, defaultBg)
                                    }
                                }
                            }
                        }
                    }

                    seriesDateRangeTextView.text = it.series.dateRange
                    formatTextView.text = it.series.publishingFormat?.lowercase()

//            coverProgressBar.visibility =
//                if (firstIssue == null || firstIssue.coverUri != null || firstIssue.cover != null) {
//                    View.GONE
//                } else {
//                    View.VISIBLE
//                }
                }
            }
        }

        private fun updateCoverView(uri: Uri?, defaultBg: Drawable?) {
            if (uri != null) {
                seriesImageView.setImageURI(uri)
                coverProgressBar.visibility = View.GONE
            } else {
                seriesImageView.setImageDrawable(defaultBg)
                coverProgressBar.visibility = View.VISIBLE
            }
        }

        override fun onClick(v: View?) {
            (callback as SeriesListCallback?)?.onSeriesSelected(item!!)
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

fun Context.getDrawableFromAttr(
    @AttrRes attrDrawable: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrDrawable, typedValue, resolveRefs)
    return typedValue.resourceId
}