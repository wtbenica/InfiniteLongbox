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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Layout
import kotlin.math.roundToInt

/** How far up the top bar is (zero or less), read where it is laid out rather than composed. */
val LocalBarOffset = staticCompositionLocalOf<() -> Float> { { 0f } }

/** The top bar scrolling away as a list scrolls up, and back as it scrolls down; [offset] is how far up it is. */
@Stable
class BarCollapse {
    var offset by mutableFloatStateOf(0f)

    /** Minus the bar's height, as last measured. */
    var limit = 0f

    /** False on a page, where the bar stays. */
    var enabled = true

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (enabled) offset = (offset + available.y).coerceIn(limit, 0f)
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (enabled && available.y > 0f) offset = (offset + available.y).coerceIn(limit, 0f)
            return Offset.Zero
        }
    }
}

/** [content] that slides out of the top as [collapse] says, giving its room to what is below. */
@Composable
fun CollapsingBar(collapse: BarCollapse, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier.clipToBounds()) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minHeight = 0))
        collapse.limit = -placeable.height.toFloat()
        val shift = collapse.offset.coerceIn(collapse.limit, 0f).roundToInt()
        layout(placeable.width, placeable.height + shift) { placeable.place(0, shift) }
    }
}
