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
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
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
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.database.models.Issue
import dev.benica.infinite_longbox.fragments_view_models.IssueListViewModel
import dev.benica.infinite_longbox.ui.InfiniteLongboxTheme
import dev.benica.infinite_longbox.ui.IssueCardState
import dev.benica.infinite_longbox.ui.IssueListItem
import dev.benica.infinite_longbox.ui.IssueListItemSkeleton
import dev.benica.infinite_longbox.ui.placeholderKey
import dev.benica.infinite_longbox.views.SeriesDetailBox
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class IssueListFragment : ListFragment<FullIssue, IssueListFragment.IssueViewHolder>() {

    override val viewModel: IssueListViewModel by viewModels()
    override val minColSizeDp = 300

    private var mSeries: FullSeries? = null
    private var seriesDetailBox: SeriesDetailBox? = null

    private fun updateSeriesDetailFragment(series: FullSeries, newBox: Boolean = false) {
        seriesDetailBox.let {
            if (newBox || it == null) {
                details.removeAllViews()
                seriesDetailBox = SeriesDetailBox(requireContext(), series)
                details.addView(seriesDetailBox)
            } else {
                it.setSeries(series)
            }
        }

        updateBottomPadding()
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager =
        GridLayoutManager(context, numCols)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemDecoration =
            ItemOffsetDecoration(
                itemOffset = resources.getDimension(R.dimen.item_offset_vert_list_item_issue)
                    .toInt(),
                itemOffsetHorizontal = resources.getDimension(R.dimen.item_offset_horz_list_item_issue)
                    .toInt(),
                numCols = numCols
            )

        listRecyclerView.addItemDecoration(itemDecoration)

        // The adapter and paging collection are set up by ListFragment. A second adapter here
        // would be collected too, and its scroll hints (always at the top) made refreshes after
        // a collection change reload from the top.

        viewModel.seriesLiveData.observe(
            viewLifecycleOwner
        ) { fullSeries ->
            fullSeries?.let {
                mSeries = fullSeries
                updateSeriesDetailFragment(it)
                callback?.setTitle(fullSeries.series.seriesName)
            }
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_issue_list, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()

        mSeries?.let {
            updateSeriesDetailFragment(it, true)
        }
    }

    override fun getAdapter(): IssueAdapter = IssueAdapter()

    override fun onDestroyView() {
        viewModel.saveIssueListState(listRecyclerView.layoutManager?.onSaveInstanceState())
        super.onDestroyView()
    }

//                //  exists an issue number 1, then violates unique series/issue restraint in db
//                val issue = filter.mSeries?.let { Issue(seriesId = it.seriesId) } ?: Issue()
//                issueListViewModel.addIssue(issue)
//                issueListCallback?.onNewIssue(issue.issueId)
//                true
//
    inner class IssueAdapter :
        PagingDataAdapter<FullIssue, IssueViewHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueViewHolder =
            IssueViewHolder(parent)

        override fun onBindViewHolder(holder: IssueViewHolder, position: Int) {
            holder.bind(getItem(position), position)
        }
    }

    inner class IssueViewHolder(val parent: ViewGroup) : RecyclerView.ViewHolder(
        ComposeView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Recycled holders: drop the composition when the view leaves the pool or window
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }
    ) {
        private var fullIssue: FullIssue? = null
        private var card by mutableStateOf<IssueCardState?>(null)
        private var slot by mutableStateOf(0)

        init {
            (itemView as ComposeView).setContent {
                InfiniteLongboxTheme {
                    val state = card
                    if (state == null) {
                        IssueListItemSkeleton(placeholderKey = "issue|$slot")
                    } else {
                        IssueListItem(
                            state = state,
                            onClick = {
                                fullIssue?.issue?.let {
                                    (callback as IssueListCallback?)?.onIssueSelected(it)
                                }
                            },
                            onToggleCollection = { toggle(BaseCollection.MY_COLL.id, state.inCollection) },
                            onToggleWishList = { toggle(BaseCollection.WISH_LIST.id, state.inWishList) },
                        )
                    }
                }
            }
        }

        private fun toggle(collId: Int, isIn: Boolean) {
            val issue = fullIssue ?: return
            val cb = callback as? IssueListCallback? ?: return
            if (isIn) cb.removeFromCollection(issue, collId) else cb.addToCollection(issue, collId)
        }

        fun bind(item: FullIssue?, position: Int) {
            // A recycled view must start clean, whatever animation it was last part of
            itemView.translationX = 0f
            itemView.translationY = 0f
            itemView.alpha = 1f
            itemView.scaleX = 1f
            itemView.scaleY = 1f

            fullIssue = item
            this.slot = position
            if (item == null) {
                card = null
                return
            }

            viewModel.updateIssueCover(item.issue.issueId)

            val coverUri = item.coverUri
            val collectionIds = item.collectionItems.map { it.userCollection }
            val issue = item.issue

            card = IssueCardState(
                issueId = issue.issueId,
                placeholderKey = item.placeholderKey(),
                number = issue.issueNumRaw.orEmpty(),
                variantName = issue.variantName.takeIf { it.isNotEmpty() && issue.variantOf != null },
                cover = coverUri?.path?.let { File(it) },
                // Not found yet (still looking) vs looked for and missing
                loadingCover = coverUri == null && item.cover == null,
                inCollection = BaseCollection.MY_COLL.id in collectionIds,
                inWishList = BaseCollection.WISH_LIST.id in collectionIds,
            )
        }
    }

    interface IssueListCallback : ListFragmentCallback {
        fun onIssueSelected(issue: Issue)
        fun onNewIssue(issueId: Int)
        fun addToCollection(issue: FullIssue, collId: Int)
        fun removeFromCollection(issue: FullIssue, collId: Int)
    }

    companion object {
        @JvmStatic
        fun newInstance() = IssueListFragment()

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FullIssue>() {
            override fun areItemsTheSame(oldItem: FullIssue, newItem: FullIssue): Boolean =
                oldItem.issue.issueId == newItem.issue.issueId

            override fun areContentsTheSame(oldItem: FullIssue, newItem: FullIssue): Boolean =
                oldItem == newItem
        }

        private const val TAG = APP + "IssueListFragment"
    }
}