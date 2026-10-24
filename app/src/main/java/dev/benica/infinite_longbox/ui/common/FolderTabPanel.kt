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

package dev.benica.infinite_longbox.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize

/** A panel with a tab on its top-left, like a folder. */
@Composable
fun FolderTabPanel(
    tab: @Composable () -> Unit,
    body: @Composable () -> Unit,
    fill: Color,
    stroke: Color,
    strokeWidth: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    filletRadius: Dp = cornerRadius,
) {
    var tabSize by remember { mutableStateOf(IntSize.Zero) }

    Layout(
        content = {
            tab()
            body()
        },
        modifier = modifier.drawBehind {
            if (tabSize == IntSize.Zero) return@drawBehind
            val s = strokeWidth.toPx()
            val inset = s / 2f
            val r = cornerRadius.toPx()
            val tabW = tabSize.width.toFloat()
            val tabH = tabSize.height.toFloat()
            val w = size.width
            val h = size.height
            // A long title can leave less room right of the tab than the fillet needs
            val f = minOf(filletRadius.toPx(), w - (tabW - inset)).coerceAtLeast(0f)
            // Stroke centered on a line inset by half its width, so it lies inside the bounds
            val panelTop = tabH + inset

            val outline = Path().apply {
                moveTo(0f, h)
                lineTo(0f, inset + r)
                arcTo(Rect(0f, inset, 2 * r, inset + 2 * r), 180f, 90f, false) // tab top-left
                lineTo(tabW - inset - r, inset)
                arcTo(
                    Rect(tabW - inset - 2 * r, inset, tabW - inset, inset + 2 * r),
                    270f, 90f, false
                ) // tab top-right
                lineTo(tabW - inset, panelTop - f)
                if (f > 0f) {
                    arcTo(
                        Rect(tabW - inset, panelTop - 2 * f, tabW - inset + 2 * f, panelTop),
                        180f, -90f, false
                    ) // concave fillet into the panel's top edge
                }
                // Square at the far end: it runs into the card's own border
                lineTo(w, panelTop)
                lineTo(w, h)
                close()
            }
            drawPath(outline, fill)
            drawPath(
                outline, stroke,
                style = Stroke(width = s, join = StrokeJoin.Round)
            )
        }
    ) { measurables, constraints ->
        val tabPlaceable = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val bodyPlaceable = measurables[1].measure(
            constraints.copy(minWidth = constraints.maxWidth, minHeight = 0)
        )
        tabSize = IntSize(tabPlaceable.width, tabPlaceable.height)

        layout(constraints.maxWidth, tabPlaceable.height + bodyPlaceable.height) {
            tabPlaceable.placeRelative(0, 0)
            bodyPlaceable.placeRelative(0, tabPlaceable.height)
        }
    }
}
