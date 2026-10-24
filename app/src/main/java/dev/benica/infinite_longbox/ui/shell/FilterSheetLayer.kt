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
package dev.benica.infinite_longbox.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.filter.FilterSheetHost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** How much of the sheet shows when it is closed, in dp: its header, and the system bar under it. */
@Composable
fun sheetPeek() = dimensionResource(R.dimen.peek_height) +
    with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }

/** The filter sheet over the bottom of the screen: closed, open or hidden; a drag settles at closed or open. */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun FilterSheetLayer(
    host: FilterSheetHost,
    hidden: Boolean,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val peekPx = with(density) { sheetPeek().toPx() }

    var height by remember { mutableIntStateOf(0) }
    var offset by remember { mutableFloatStateOf(Float.NaN) }
    var animation by remember { mutableStateOf<Job?>(null) }
    val closedAt = (height - peekPx).coerceAtLeast(0f)

    fun settle(target: Float, velocity: Float = 0f) {
        animation?.cancel()
        animation = scope.launch {
            animate(offset, target, velocity, spring(stiffness = Spring.StiffnessMediumLow)) { value, _ -> offset = value }
        }
    }

    // Rests where it should: open, closed, or out of sight. The first time it is simply put there.
    LaunchedEffect(height, peekPx, hidden, open) {
        if (height == 0) return@LaunchedEffect
        val target = when {
            hidden -> height.toFloat()
            open -> 0f
            else -> closedAt
        }
        if (offset.isNaN()) offset = target else settle(target)
    }

    SideEffect { host.onOpen = { onOpenChange(true) } }
    BackHandler(enabled = open && !hidden) { onOpenChange(false) }

    val slide = if (offset.isNaN() || closedAt <= 0f) 0f else (1f - offset / closedAt).coerceIn(0f, 1f)

    Box(modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .onSizeChanged { height = it.height }
                .offset { IntOffset(0, if (offset.isNaN()) HIDDEN_AT else offset.roundToInt()) }
                .draggable(
                    orientation = Orientation.Vertical,
                    enabled = !hidden,
                    state = rememberDraggableState { delta ->
                        if (!offset.isNaN()) offset = (offset + delta).coerceIn(0f, closedAt)
                    },
                    onDragStarted = { animation?.cancel() },
                    onDragStopped = { velocity ->
                        val toClosed = velocity > FLING || (velocity > -FLING && offset > closedAt / 2)
                        onOpenChange(!toClosed)
                        settle(if (toClosed) closedAt else 0f, velocity)
                    },
                )
                // Nothing under the sheet should feel a touch that lands on it
                .pointerInput(Unit) { detectTapGestures { } },
            shape = RoundedCornerShape(
                topStart = dimensionResource(R.dimen.margin_wide),
                topEnd = dimensionResource(R.dimen.margin_wide),
            ),
            color = colorResource(R.color.surface_raised),
            shadowElevation = dimensionResource(R.dimen.margin_default),
        ) {
            Box(Modifier.navigationBarsPadding()) { host.Sheet(slide = slide, expanded = slide >= 1f) }
        }
    }
}

private const val FLING = 1200f

/** Where the sheet sits before it has been measured: out of sight. */
private const val HIDDEN_AT = 100_000
