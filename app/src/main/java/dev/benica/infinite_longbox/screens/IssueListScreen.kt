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

package dev.benica.infinite_longbox.screens

import android.widget.Toast
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import dev.benica.infinite_longbox.nav.ScreenHost
import androidx.activity.compose.BackHandler
import androidx.lifecycle.lifecycleScope
import androidx.paging.compose.LazyPagingItems
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.filter.SearchFilter
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.database.models.Issue
import dev.benica.infinite_longbox.viewmodels.IssueListViewModel
import dev.benica.infinite_longbox.repository.Repository
import dev.benica.infinite_longbox.ui.covers.CoverImages
import dev.benica.infinite_longbox.ui.covers.placeholderKey
import dev.benica.infinite_longbox.ui.toolbar.BarAction
import dev.benica.infinite_longbox.ui.toolbar.SelectionBar
import dev.benica.infinite_longbox.ui.lists.IssueCardState
import dev.benica.infinite_longbox.ui.lists.IssueListItem
import dev.benica.infinite_longbox.ui.lists.IssueListItemSkeleton
import dev.benica.infinite_longbox.ui.series.SeriesInfoBox
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.format.DateTimeFormatter
import dev.benica.infinite_longbox.ui.dialogs.StoragePrompt

/**
 * The issues, as cards. For one series the series' details sit above them; a long press picks an
 * issue, and from then on a tap picks or unpicks one and dragging picks a range.
 */
@ExperimentalCoroutinesApi
class IssueListScreen(host: ScreenHost) : ComposeListScreen<FullIssue>(host) {

    override val viewModel: IssueListViewModel by lazy { host.entry.viewModel<IssueListViewModel>() }
    override val minColSizeDp = 300

    /** The series shown above the issues, when the list is of one series. */
    private var detailSeries by mutableStateOf<FullSeries?>(null)

    override val hasHeader: Boolean
        get() = detailSeries != null

    @Composable
    override fun Header() {
        detailSeries?.let { shown ->
            SeriesInfoBox(shown, onSeries = { callback.updateFilter(SearchFilter(series = it, myCollection = false)) })
        }
    }

    init {
        viewModel.seriesLiveData.observe(viewLifecycleOwner) { fullSeries ->
            fullSeries?.let {
                detailSeries = it
                callback.setTitle(it.series.seriesName)
            }
        }
    }

    override fun onHidden() {
        endSelection()
    }

    override fun key(item: FullIssue): Any = item.issue.issueId

    @Composable
    override fun Skeleton(index: Int) = IssueListItemSkeleton(placeholderKey = "issue|$index")

    @Composable
    override fun Row(item: FullIssue, index: Int) {
        val id = item.issue.issueId
        LaunchedEffect(id) { viewModel.updateIssueCover(id) }

        val picked = id in selection
        val state = remember(item, picked) {
            val coverUri = item.coverUri
            val collectionIds = item.collectionItems.map { it.userCollection }
            val issue = item.issue
            IssueCardState(
                issueId = issue.issueId,
                placeholderKey = item.placeholderKey(),
                number = issue.issueNumRaw.orEmpty(),
                variantName = issue.variantName.takeIf { it.isNotEmpty() && issue.variantOf != null },
                cover = coverUri?.path?.let { File(it) }?.takeIf { it.exists() },
                // Not found yet (still looking) vs looked for and missing
                loadingCover = coverUri == null && item.cover == null,
                inCollection = BaseCollection.MY_COLL.id in collectionIds,
                inWishList = BaseCollection.WISH_LIST.id in collectionIds,
                selected = picked,
            )
        }
        IssueListItem(
            state = state,
            onClick = {
                if (selection.isNotEmpty()) {
                    if (!selection.add(id)) selection.remove(id)
                } else {
                    (callback as IssueListCallback?)?.onIssueSelected(item.issue)
                }
            },
            onToggleCollection = { toggle(item, BaseCollection.MY_COLL.id, state.inCollection) },
            onToggleWishList = { toggle(item, BaseCollection.WISH_LIST.id, state.inWishList) },
        )
    }

    private fun toggle(issue: FullIssue, collId: Int, isIn: Boolean) {
        val cb = callback as? IssueListCallback ?: return
        if (isIn) cb.removeFromCollection(issue, collId) else cb.addToCollection(issue, collId)
    }

    override fun scrollLabel(item: FullIssue): String? = when {
        // An issue knows its month; issues aren't sorted by name
        sortedByDate() -> (item.issue.releaseDate ?: item.issue.coverDate)?.format(MONTH_YEAR)
        else -> null
    }

    override fun prefetch(item: FullIssue) {
        item.coverUri?.path?.let { CoverImages.prefetchIssue(context.applicationContext, File(it)) }
    }

    // BULK SELECTION: a long press picks an issue, and from then on a tap picks or unpicks one

    private val selection = mutableStateSetOf<Int>()

    /** The filter the selection was made under: a different list is a different selection. */
    private var selectionFilter: SearchFilter? = null

    @Composable
    override fun gridModifier(items: LazyPagingItems<FullIssue>, grid: LazyGridState, headerCount: Int): Modifier {
        val scope = rememberCoroutineScope()
        BackHandler(enabled = selection.isNotEmpty()) { endSelection() }

        // The top bar follows the selection: it changes with the first pick and goes back with the last
        LaunchedEffect(Unit) {
            snapshotFlow { selection.size }.collect { picked ->
                if (picked == 0) {
                    endSelection()
                } else {
                    if (selectionFilter == null) selectionFilter = viewModel.filter.value
                                callback.setSelection(
                        SelectionBar(
                            picked,
                            listOf(
                                BarAction(getString(R.string.selection_select_all)) { selectAll() },
                                BarAction(getString(R.string.selection_move_storage)) { moveSelectedToStorage() },
                            ),
                            onClose = ::endSelection,
                        )
                    )
                }
            }
        }

        // Picked issues belong to the list they were picked in: another filter ends the selection
        LaunchedEffect(Unit) {
            snapshotFlow { viewModel.filter.value }.collect { filter ->
                val under = selectionFilter
                if (under != null && filter != null && !under.sameItemsAs(filter)) {
                    endSelection()
                }
            }
        }

        return Modifier.pointerInput(items) {
            var anchor = -1
            var current = -1
            var base: Set<Int> = emptySet()
            var finger = Offset.Zero
            var scroller: Job? = null

            fun rowAt(at: Offset): Int? = grid.layoutInfo.visibleItemsInfo.firstOrNull {
                at.x >= it.offset.x && at.x < it.offset.x + it.size.width &&
                    at.y >= it.offset.y && at.y < it.offset.y + it.size.height
            }?.index?.minus(headerCount)?.takeIf { it >= 0 }

            // Everything from the first row pressed to the one under the finger is picked, and the rest as it was
            fun pick(at: Offset) {
                val row = rowAt(at) ?: return
                if (row == current) return
                selection.clear()
                selection.addAll(base)
                for (i in minOf(anchor, row)..maxOf(anchor, row)) items.peek(i)?.let { selection.add(it.issue.issueId) }
                current = row
            }

            detectDragGesturesAfterLongPress(
                onDragStart = { at ->
                    val row = rowAt(at) ?: return@detectDragGesturesAfterLongPress
                    val id = items.peek(row)?.issue?.issueId ?: return@detectDragGesturesAfterLongPress
                    selection.add(id)
                    anchor = row
                    current = row
                    base = selection.toSet()
                    finger = at
                    // Near the top or bottom edge the list scrolls on by itself, however still the finger is
                    scroller = scope.launch {
                        val edge = size.height * EDGE_FRACTION
                        while (true) {
                            val dy = when {
                                finger.y < edge -> -(edge - finger.y) / edge * MAX_SCROLL
                                finger.y > size.height - edge -> (finger.y - (size.height - edge)) / edge * MAX_SCROLL
                                else -> 0f
                            }
                            if (dy != 0f) {
                                grid.scrollBy(dy)
                                pick(finger)
                            }
                            delay(16)
                        }
                    }
                },
                onDrag = { change, _ ->
                    finger = change.position
                    pick(change.position)
                },
                onDragEnd = { scroller?.cancel() },
                onDragCancel = { scroller?.cancel() },
            )
        }
    }

    private fun endSelection() {
        selection.clear()
        selectionFilter = null
        callback.setSelection(null)
    }

    /** Everything the list shows, not only the part that has loaded so far. */
    private fun selectAll() {
        val filter = viewModel.filter.value ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            selection.addAll(Repository.get().getIssueIdsByFilter(filter))
        }
    }

    /** Files the picked copies in a storage place; the picked issues that aren't in My Collection are left. */
    private fun moveSelectedToStorage() {
        val ids = selection.toList()
        if (ids.isEmpty()) return
        StoragePrompt.show(
            activity, R.string.selection_storage_title,
            getString(R.string.selection_storage_message, ids.size), null, null,
        ) { place ->
            viewLifecycleOwner.lifecycleScope.launch {
                val moved = Repository.get().setStorage(ids, place)
                val left = ids.size - moved
                val text = getString(R.string.selection_moved, moved, place.orEmpty()) +
                    if (left > 0) getString(R.string.selection_not_owned, left) else ""
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                endSelection()
            }
        }
    }

    interface IssueListCallback : ScreenCallback {
        fun onIssueSelected(issue: Issue)
        fun onNewIssue(issueId: Int)
        fun addToCollection(issue: FullIssue, collId: Int)
        fun removeFromCollection(issue: FullIssue, collId: Int)
    }

    private companion object {
        /** Dragging within this fraction of the list's height from its top or bottom scrolls it. */
        const val EDGE_FRACTION = 0.15f
        const val MAX_SCROLL = 40f
        val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    }
}
