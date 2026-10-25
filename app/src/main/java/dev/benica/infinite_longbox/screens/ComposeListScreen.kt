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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.benica.infinite_longbox.filter.LetterIndex
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.filter.SortType
import dev.benica.infinite_longbox.database.models.ListItem
import dev.benica.infinite_longbox.nav.ScreenHost
import dev.benica.infinite_longbox.ui.shell.LocalBarOffset
import dev.benica.infinite_longbox.ui.shell.LocalSheetPeek
import dev.benica.infinite_longbox.ui.lists.FastScrollThumb
import dev.benica.infinite_longbox.ui.lists.LetterStrip
import dev.benica.infinite_longbox.ui.lists.ScrollBubble
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** A list screen in Compose: a paged grid of rows, with a letter strip or scroll handle on the right edge. */
@ExperimentalCoroutinesApi
abstract class ComposeListScreen<T : ListItem>(host: ScreenHost) : FilteredListScreen<T>(host) {

    /** Identifies a row across loads, so the list stays where it was while rows change. */
    protected abstract fun key(item: T): Any

    @Composable
    protected abstract fun Skeleton(index: Int)

    @Composable
    protected abstract fun Row(item: T, index: Int)

    /** What the scroll handle says at [item]: its initial, or its date if the list is sorted by date. */
    protected abstract fun scrollLabel(item: T): String?

    /** Whether the list has a header: a full-width item above the rows, which scrolls away with them. */
    protected open val hasHeader: Boolean
        get() = false

    @Composable
    protected open fun Header() {
    }

    /** What a subclass adds to the grid's gestures; [headerCount] items come before the first row. */
    @Composable
    protected open fun gridModifier(items: LazyPagingItems<T>, grid: LazyGridState, headerCount: Int): Modifier =
        Modifier

    /** Loads what the row at [item] will show, before it is scrolled to. */
    protected open fun prefetch(item: T) {}

    protected fun sortedByName(): Boolean =
        viewModel.filter.value?.mSortType?.sortColumn?.contains("name", ignoreCase = true) == true

    protected fun sortedByDate(): Boolean =
        viewModel.filter.value?.mSortType?.sortColumn?.contains("date", ignoreCase = true) == true

    /** The initial of a name as the list sorts it ("#" for numbers). */
    protected fun initialOf(name: String): String? {
        val first = SortType.sortKeyOf(name).firstOrNull() ?: return null
        return if (first.isLetter()) first.uppercase() else "#"
    }

    @Composable
    override fun Content() {
        // Opaque, as the list was: whatever is behind it (a screen mid-fade) must not show through the gaps
        Box(Modifier.fillMaxSize().background(colorResource(R.color.surface))) { ListScreen() }
    }

    @Composable
    private fun ListScreen() {
        val filter by viewModel.filter.asFlow().collectAsState(initial = null)
        val items = viewModel.itemList.collectAsLazyPagingItems()
        val headerCount = if (hasHeader) 1 else 0
        val grid = rememberLazyGridState()
        val scope = rememberCoroutineScope()

        // The padding under the list: what shows of the bottom sheet
        val bottom = LocalSheetPeek.current

        // Coming back puts the list where it was once rows exist; another filter starts at the top
        var placed by remember { mutableStateOf(false) }
        LaunchedEffect(filter, items.itemCount > 0) {
            val shown = filter ?: return@LaunchedEffect
            if (items.itemCount == 0) return@LaunchedEffect
            if (!placed) {
                placed = true
                if (viewModel.scrolledFor == null || viewModel.scrolledFor == shown) {
                    grid.scrollToItem(viewModel.scrollIndex, viewModel.scrollOffset)
                } else {
                    viewModel.scrollIndex = 0
                    viewModel.scrollOffset = 0
                }
            } else if (viewModel.scrolledFor != shown) {
                grid.scrollToItem(0)
            }
            viewModel.scrolledFor = shown
        }
        LaunchedEffect(grid, items) {
            snapshotFlow { Triple(placed, grid.firstVisibleItemIndex, grid.firstVisibleItemScrollOffset) }
                .collect { (ready, index, offset) ->
                    if (ready && items.itemCount > 0) {
                        viewModel.scrollIndex = index
                        viewModel.scrollOffset = offset
                    }
                }
        }

        // The covers of the rows about to scroll in
        LaunchedEffect(grid, items) {
            snapshotFlow { grid.firstVisibleItemIndex - headerCount }.collect { first ->
                for (i in (first - 4).coerceAtLeast(0)..(first + 14).coerceAtMost(items.itemCount - 1)) {
                    items.peek(i)?.let(::prefetch)
                }
            }
        }

        // Where each letter starts: asked again as rows arrive, which moves them
        var letterIndex by remember { mutableStateOf<LetterIndex?>(null) }
        LaunchedEffect(filter) {
            val shown = filter ?: return@LaunchedEffect
            letterIndex = null
            if (!sortedByName()) return@LaunchedEffect
            snapshotFlow { items.itemCount }.collect {
                val index = viewModel.letterIndex(shown) ?: return@collect
                if (viewModel.filter.value == shown && index != letterIndex) letterIndex = index
            }
        }

        // The strip and the handle show while the list scrolls or is held, and fade after a moment
        var stripHeld by remember { mutableStateOf(false) }
        var thumbHeld by remember { mutableStateOf(false) }
        var showing by remember { mutableStateOf(false) }
        val scrolling = grid.isScrollInProgress
        LaunchedEffect(scrolling, stripHeld, thumbHeld) {
            if (scrolling || stripHeld || thumbHeld) {
                showing = true
            } else {
                delay(HIDE_DELAY_MS)
                showing = false
            }
        }

        val label by remember(items) {
            derivedStateOf {
                val at = (grid.firstVisibleItemIndex - headerCount).coerceAtLeast(0)
                if (at < items.itemCount) items.peek(at)?.let(::scrollLabel) else null
            }
        }
        // Where the bubble is, and what it says if not the live label (the strip's letter)
        var bubble by remember { mutableStateOf<Pair<String?, Float>?>(null) }
        var lastLabel by remember { mutableStateOf("") }
        LaunchedEffect(label) { label?.let { lastLabel = it } }
        val barOffset = LocalBarOffset.current
        var thumbCenter by remember { mutableStateOf(0f) }
        val fraction by remember(items) {
            derivedStateOf {
                val visible = grid.layoutInfo.visibleItemsInfo.size
                val total = items.itemCount
                if (total <= visible || total == 0) 0f
                else ((grid.firstVisibleItemIndex - headerCount).coerceAtLeast(0).toFloat() / (total - visible)).coerceIn(0f, 1f)
            }
        }

        Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(numCols),
                state = grid,
                modifier = Modifier
                    .fillMaxSize()
                    .then(gridModifier(items, grid, headerCount)),
                contentPadding = PaddingValues(bottom = bottom),
            ) {
                if (hasHeader) item(span = { GridItemSpan(maxLineSpan) }) { Header() }
                items(count = items.itemCount, key = items.itemKey { key(it) }) { index ->
                    val item = items[index]
                    if (item == null) Skeleton(index) else Row(item, index)
                }
            }

            // The strip, handle and bubble stay where they are as the top bar slides away and the list grows under it
            Box(
                Modifier.fillMaxSize().layout { measurable, constraints ->
                    val shift = (-barOffset()).roundToInt().coerceIn(0, constraints.maxHeight)
                    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = constraints.maxHeight - shift))
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, shift) }
                }
            ) {
                val index = letterIndex
                if (items.itemCount > 0 && showing) {
                    if (index != null) {
                        val current by remember(items) {
                            derivedStateOf { label?.firstOrNull() }
                        }
                        LetterStrip(
                            index = index,
                            current = current,
                            bottomPadding = bottom + 72.dp,
                            onJump = { letter, position, centerY ->
                                scope.launch { grid.scrollToItem(position + headerCount) }
                                bubble = letter.toString() to centerY
                            },
                            onDragging = { held ->
                                stripHeld = held
                                if (!held) bubble = null
                            },
                        )
                    } else if (!sortedByName()) {
                        FastScrollThumb(
                            fraction = fraction,
                            count = items.itemCount,
                            bottomPadding = bottom + 72.dp,
                            onDrag = { position, centerY ->
                                scope.launch { grid.scrollToItem(position + headerCount) }
                                bubble = null to centerY
                            },
                            onDragging = { held ->
                                thumbHeld = held
                                if (!held) bubble = null
                            },
                            onThumbMoved = { thumbCenter = it },
                        )
                    }
                }
                // A letter or date by the finger on the strip or handle, or by the handle as the list scrolls
                val passive = if (!thumbHeld && !stripHeld && scrolling && !sortedByName() && showing) label?.let { it to thumbCenter } else null
                val shown = bubble?.let { (text, y) -> (text ?: label ?: lastLabel) to y } ?: passive
                shown?.takeIf { it.first.isNotEmpty() && showing }?.let { (text, y) -> ScrollBubble(text, y) }
            }
        }
    }

    private companion object {
        const val HIDE_DELAY_MS = 1500L
    }
}
