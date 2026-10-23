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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
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
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.ui.covers.CoverImages
import dev.benica.infinite_longbox.ui.covers.placeholderKey
import dev.benica.infinite_longbox.ui.lists.IssueCardState
import dev.benica.infinite_longbox.ui.lists.IssueListItem
import dev.benica.infinite_longbox.ui.lists.IssueListItemSkeleton
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import dev.benica.infinite_longbox.views.SeriesDetailBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
        listRecyclerView.addOnItemTouchListener(dragSelect)

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
        selectionMode?.finish()
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
            prefetchCovers(position)
        }

        /** Loads the covers of the cards about to scroll in, so they don't appear after the card does. */
        private fun prefetchCovers(position: Int) {
            val context = context ?: return
            val from = (position - CoverImages.PREFETCH_BEHIND).coerceAtLeast(0)
            val to = (position + CoverImages.PREFETCH_AHEAD).coerceAtMost(itemCount - 1)
            for (i in from..to) {
                if (i == position) continue
                peek(i)?.coverUri?.path?.let { CoverImages.prefetchIssue(context, File(it)) }
            }
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
        val issueId: Int? get() = fullIssue?.issue?.issueId
        private var card by mutableStateOf<IssueCardState?>(null)
        private var slot by mutableIntStateOf(0)

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
                                if (selecting) toggleSelected(this@IssueViewHolder)
                                else fullIssue?.issue?.let {
                                    (callback as IssueListCallback?)?.onIssueSelected(it)
                                }
                            },
                            onLongClick = { startSelecting(this@IssueViewHolder) },
                            onToggleCollection = {
                                toggle(
                                    BaseCollection.MY_COLL.id,
                                    state.inCollection
                                )
                            },
                            onToggleWishList = {
                                toggle(
                                    BaseCollection.WISH_LIST.id,
                                    state.inWishList
                                )
                            },
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
                cover = coverUri?.path?.let { File(it) }?.takeIf { it.exists() },
                // Not found yet (still looking) vs looked for and missing
                loadingCover = coverUri == null && item.cover == null,
                inCollection = BaseCollection.MY_COLL.id in collectionIds,
                inWishList = BaseCollection.WISH_LIST.id in collectionIds,
                selected = issue.issueId in selection,
            )
        }
    }

    // BULK SELECTION: a long press picks an issue, and from then on a tap picks or unpicks one

    private val selection = linkedSetOf<Int>()
    private var selectionMode: ActionMode? = null
    private val selecting: Boolean
        get() = selectionMode != null

    private fun toggleSelected(holder: IssueViewHolder) {
        val id = holder.issueId ?: return
        if (!selection.add(id)) selection.remove(id)
        listRecyclerView.adapter?.notifyItemChanged(holder.bindingAdapterPosition)
        when {
            selection.isEmpty() -> selectionMode?.finish()
            selectionMode == null ->
                selectionMode = (activity as? AppCompatActivity)?.startSupportActionMode(selectionCallback)
            else -> selectionMode?.invalidate()
        }
    }

    /** A long press picks the issue; keeping the finger down and moving then picks more (see [DragSelect]). */
    private fun startSelecting(holder: IssueViewHolder) {
        val id = holder.issueId ?: return
        if (id !in selection) toggleSelected(holder)
        dragSelect.start(holder.bindingAdapterPosition)
    }

    private val dragSelect = DragSelect()

    /**
     * After a long press, dragging over other issues picks everything from the first one to the
     * one under the finger (and unpicks again if the finger goes back). Near the top or bottom
     * edge the list scrolls on its own. Taps and ordinary scrolls are left alone.
     */
    private inner class DragSelect : RecyclerView.OnItemTouchListener {
        private var touching = false
        private var dragging = false
        private var anchor = -1
        private var current = -1
        private var base: Set<Int> = emptySet()
        private var lastX = 0f
        private var lastY = 0f

        private val autoScroll = object : Runnable {
            override fun run() {
                if (!dragging) return
                val list = listRecyclerView
                val edge = list.height * EDGE_FRACTION
                val dy = when {
                    lastY < edge -> -(edge - lastY) / edge * MAX_SCROLL
                    lastY > list.height - edge -> (lastY - (list.height - edge)) / edge * MAX_SCROLL
                    else -> 0f
                }
                if (dy != 0f) {
                    list.scrollBy(0, dy.toInt())
                    pick(lastX, lastY)
                }
                list.postOnAnimation(this)
            }
        }

        fun start(position: Int) {
            if (!touching || position == RecyclerView.NO_POSITION) return
            dragging = true
            anchor = position
            current = position
            base = selection.toSet()
            listRecyclerView.postOnAnimation(autoScroll)
        }

        private fun stop() {
            touching = false
            dragging = false
        }

        private fun pick(x: Float, y: Float) {
            val list = listRecyclerView
            val child = list.findChildViewUnder(x, y) ?: return
            val position = list.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION || position == current) return

            val paged = list.adapter as? PagingDataAdapter<*, *> ?: return
            val oldLow = minOf(anchor, current)
            val oldHigh = maxOf(anchor, current)
            val low = minOf(anchor, position)
            val high = maxOf(anchor, position)

            selection.clear()
            selection.addAll(base)
            for (i in low..high) (paged.peek(i) as? FullIssue)?.let { selection.add(it.issue.issueId) }

            current = position
            val from = minOf(low, oldLow)
            list.adapter?.notifyItemRangeChanged(from, maxOf(high, oldHigh) - from + 1)
            selectionMode?.invalidate()
        }

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { touching = true; dragging = false }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stop()
            }
            lastX = e.x
            lastY = e.y
            // Once dragging, the list is the one handling the finger, not the card under it
            return dragging && e.actionMasked == MotionEvent.ACTION_MOVE
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
            lastX = e.x
            lastY = e.y
            when (e.actionMasked) {
                MotionEvent.ACTION_MOVE -> if (dragging) pick(e.x, e.y)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stop()
            }
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
    }

    private val selectionCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.issue_selection, menu)
            mode.title = getString(R.string.selected_count, selection.size)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.title = getString(R.string.selected_count, selection.size)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = when (item.itemId) {
            R.id.selection_select_all -> { selectAll(); true }
            R.id.selection_move_storage -> { moveSelectedToStorage(); true }
            else -> false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            selection.clear()
            selectionMode = null
            listRecyclerView.adapter?.let { it.notifyItemRangeChanged(0, it.itemCount) }
        }
    }

    /** Everything the list shows, not only the part that has loaded so far. */
    private fun selectAll() {
        val filter = viewModel.filter.value ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val all = withContext(Dispatchers.IO) {
                Repository.get().getIssuesByFilter(filter).first().map { it.issue.issueId }
            }
            selection.addAll(all)
            listRecyclerView.adapter?.let { it.notifyItemRangeChanged(0, it.itemCount) }
            selectionMode?.invalidate()
        }
    }

    /** Files the picked copies in a storage place; the picked issues that aren't in My Collection are left. */
    private fun moveSelectedToStorage() {
        val ids = selection.toList()
        if (ids.isEmpty()) return
        StoragePrompt.show(
            requireContext(), viewLifecycleOwner.lifecycleScope, R.string.selection_storage_title,
            getString(R.string.selection_storage_message, ids.size), null, null,
        ) { place ->
            viewLifecycleOwner.lifecycleScope.launch {
                val moved = Repository.get().setStorage(ids, place)
                val left = ids.size - moved
                val text = getString(R.string.selection_moved, moved, place.orEmpty()) +
                    if (left > 0) getString(R.string.selection_not_owned, left) else ""
                Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
                selectionMode?.finish()
            }
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

        /** Dragging within this fraction of the list's height from its top or bottom scrolls it. */
        private const val EDGE_FRACTION = 0.15f
        private const val MAX_SCROLL = 40f

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FullIssue>() {
            override fun areItemsTheSame(oldItem: FullIssue, newItem: FullIssue): Boolean =
                oldItem.issue.issueId == newItem.issue.issueId

            override fun areContentsTheSame(oldItem: FullIssue, newItem: FullIssue): Boolean =
                oldItem == newItem
        }

        @Suppress("unused")
        private const val TAG = APP + "IssueListFragment"
    }
}