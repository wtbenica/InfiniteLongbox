/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.benica.infinite_longbox.fragments

import android.content.Context
import android.graphics.drawable.Drawable
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
import dev.benica.infinite_longbox.APP
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.fragments_view_models.SeriesListViewModel
import dev.benica.infinite_longbox.views.FitTopImageView
import kotlinx.coroutines.*

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

        private lateinit var item: FullSeries

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

            val draw: Int? = context?.getDrawableFromAttr(R.attr.listItemBackground)
            val draw2: Drawable? = draw?.let { ResourcesCompat.getDrawable(resources, it, null) }

            firstIssue?.coverUri.let {
                if (it != null) {
                    seriesImageView.setImageURI(it)
                } else {
                    seriesImageView.setImageDrawable(draw2)
                }
            }

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

    interface SeriesListCallback : ListFragmentCallback {
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