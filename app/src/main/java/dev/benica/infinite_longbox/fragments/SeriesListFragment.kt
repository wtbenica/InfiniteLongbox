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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
import dev.benica.infinite_longbox.ui.InfiniteLongboxTheme
import dev.benica.infinite_longbox.ui.SeriesCardState
import dev.benica.infinite_longbox.ui.SeriesListItem
import dev.benica.infinite_longbox.ui.SeriesListItemSkeleton
import java.io.File
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

        listRecyclerView.addItemDecoration(itemOffsetDecoration)

        return view
    }

    inner class SeriesAdapter :
        PagingDataAdapter<FullSeries, SeriesHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesHolder =
            SeriesHolder(parent)

        override fun onBindViewHolder(holder: SeriesHolder, position: Int) {
            holder.bind(getItem(position), position)
        }
    }

    inner class SeriesHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        ComposeView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Recycled holders: drop the composition when the view leaves the pool or window
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }
    ) {

        private var item: FullSeries? = null
        private var card by mutableStateOf<SeriesCardState?>(null)
        private var slot by mutableStateOf(0)

        init {
            (itemView as ComposeView).setContent {
                InfiniteLongboxTheme {
                    val state = card
                    if (state == null) {
                        SeriesListItemSkeleton(placeholderKey = "series|$slot")
                    } else {
                        SeriesListItem(state, onClick = {
                            item?.let { (callback as SeriesListCallback?)?.onSeriesSelected(it) }
                        })
                    }
                }
            }
        }

        fun bind(item: FullSeries?, position: Int) {
            // A recycled view must start clean, whatever animation it was last part of
            itemView.translationX = 0f
            itemView.translationY = 0f
            itemView.alpha = 1f
            itemView.scaleX = 1f
            itemView.scaleY = 1f

            this.item = item
            this.slot = position
            if (item == null) {
                card = null
                return
            }

            val firstIssueId = item.series.firstIssue
            if (firstIssueId != null) {
                viewModel.updateIssue(
                    firstIssueId,
                    viewModel.filter.value?.mMyCollection?.let { !it } ?: true
                )
            } else if (item.series.issueCount < 10) {
                viewModel.updateIssuesBySeries(item)
            }

            val firstIssue: FullIssue? = item.firstIssue
            val coverUri = firstIssue?.coverUri ?: item.earliestCoverUri

            card = SeriesCardState(
                name = item.series.seriesName,
                publisher = item.publisher.publisher,
                dateRange = item.series.dateRange,
                format = item.series.publishingFormat?.lowercase().orEmpty(),
                cover = coverUri?.path?.let { File(it) },
                loadingCover = !(firstIssue == null || coverUri != null || firstIssue.cover != null),
            )

            // No cover for the first issue: the card falls back to the series' earliest cover
            // (item.earliestCoverUri), and goes and gets one if the phone has none
            if (coverUri == null &&
                shouldFetchSeriesCover(false, firstIssue != null, firstIssue?.cover != null)
            ) {
                viewModel.fetchSeriesCover(item.series.seriesId)
            }
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
