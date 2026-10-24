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

package dev.benica.infinite_longbox.ui.lists

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.filter.LetterIndex
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.theme.PermanentMarker
import kotlin.math.roundToInt

/** A column of letters on the list's right edge, for lists sorted by name. */
@Composable
fun BoxScope.LetterStrip(
    index: LetterIndex,
    current: Char?,
    bottomPadding: Dp,
    onJump: (letter: Char, position: Int, centerY: Float) -> Unit,
    onDragging: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val letters = LetterIndex.LETTERS
    val density = LocalDensity.current
    val stripWidth = with(density) { STRIP_WIDTH.toPx() }
    val maxLetterHeight = with(density) { MAX_LETTER_HEIGHT.toPx() }
    val edgeMargin = with(density) { EDGE_MARGIN.toPx() }
    var height by remember { mutableIntStateOf(0) }
    val measurer = rememberTextMeasurer()

    fun letterHeight() = (height.toFloat() / letters.size).coerceAtMost(maxLetterHeight)
    fun top() = (height - letterHeight() * letters.size) / 2f

    val background = colorResource(R.color.surface_raised).copy(alpha = 0.9f)
    val currentBackground = colorResource(R.color.accent)
    val textColor = colorResource(R.color.accent_strong)
    val currentTextColor = colorResource(R.color.on_accent)

    Canvas(
        modifier
            .align(Alignment.CenterEnd)
            .width(TOUCH_WIDTH)
            .fillMaxHeight()
            .padding(bottom = bottomPadding)
            .onSizeChanged { height = it.height }
            .pointerInput(index) {
                fun jump(y: Float) {
                    val lh = letterHeight()
                    val touched = ((y - top()) / lh).toInt().coerceIn(0, letters.size - 1)
                    // The letter touched, or the nearest below it that has names, or failing that above
                    val chosen = (touched until letters.size).firstOrNull { index.has(letters[it]) }
                        ?: (touched downTo 0).firstOrNull { index.has(letters[it]) }
                        ?: return
                    val letter = letters[chosen]
                    onJump(letter, index.positionOf(letter) ?: return, top() + (chosen + 0.5f) * lh)
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onDragging(true)
                    jump(down.position.y)
                    drag(down.id) { change ->
                        change.consume()
                        jump(change.position.y)
                    }
                    onDragging(false)
                }
            }
    ) {
        val right = size.width - edgeMargin
        val lh = letterHeight()
        val top = top()
        drawRoundRect(
            background,
            topLeft = Offset(right - stripWidth, top),
            size = Size(stripWidth, lh * letters.size),
            cornerRadius = CornerRadius(stripWidth / 2),
        )
        val centerX = right - stripWidth / 2
        letters.forEachIndexed { i, letter ->
            val centerY = top + (i + 0.5f) * lh
            val isCurrent = letter == current
            if (isCurrent) drawCircle(currentBackground, radius = stripWidth / 2 - 2.dp.toPx(), center = Offset(centerX, centerY))
            val color = (if (isCurrent) currentTextColor else textColor)
                .copy(alpha = if (isCurrent || index.has(letter)) 1f else 0.33f)
            val layout = measurer.measure(
                letter.toString(),
                TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            )
            drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, centerY - layout.size.height / 2f))
        }
    }
}

/** A scroll handle for long lists, the same size however long: the drag maps straight to a position. */
@Composable
fun BoxScope.FastScrollThumb(
    fraction: Float,
    count: Int,
    bottomPadding: Dp,
    onDrag: (position: Int, centerY: Float) -> Unit,
    onDragging: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onThumbMoved: (centerY: Float) -> Unit = {},
) {
    val density = LocalDensity.current
    val thumbHeight = with(density) { THUMB_HEIGHT.toPx() }
    val thumbWidth = with(density) { THUMB_WIDTH.toPx() }
    val edgeMargin = with(density) { THUMB_MARGIN.toPx() }
    val slop = with(density) { GRAB_SLOP.toPx() }
    var height by remember { mutableIntStateOf(0) }
    var dragged by remember { mutableStateOf<Float?>(null) }

    val shown = dragged ?: fraction
    val thumbTop = shown * (height - thumbHeight).coerceAtLeast(0f)
    onThumbMoved(thumbTop + thumbHeight / 2f)

    val rest = colorResource(R.color.accent)
    val active = colorResource(R.color.accent_strong)

    Canvas(
        modifier
            .align(Alignment.CenterEnd)
            .width(TOUCH_WIDTH)
            .fillMaxHeight()
            .padding(bottom = bottomPadding)
            .onSizeChanged { height = it.height }
            .pointerInput(count) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val top = fraction * (height - thumbHeight).coerceAtLeast(0f)
                    // Only the handle itself is grabbed; a touch elsewhere is left to the list
                    if (down.position.y !in (top - slop)..(top + thumbHeight + slop)) return@awaitEachGesture
                    down.consume()
                    onDragging(true)
                    fun scrollTo(y: Float) {
                        val travel = (height - thumbHeight).coerceAtLeast(1f)
                        val f = ((y - thumbHeight / 2f) / travel).coerceIn(0f, 1f)
                        dragged = f
                        onDrag((f * (count - 1)).roundToInt(), f * travel + thumbHeight / 2f)
                    }
                    scrollTo(down.position.y)
                    drag(down.id) { change ->
                        change.consume()
                        scrollTo(change.position.y)
                    }
                    dragged = null
                    onDragging(false)
                }
            }
    ) {
        val right = size.width - edgeMargin
        drawRoundRect(
            if (dragged != null) active else rest,
            topLeft = Offset(right - thumbWidth, thumbTop),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = CornerRadius(thumbWidth / 2),
        )
    }
}

/** The letter or date a handle or strip is at, beside it, [centerY] down the list area. */
@Composable
fun BoxScope.ScrollBubble(label: String, centerY: Float, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val margin = with(density) { BUBBLE_MARGIN.roundToPx() }
    val height = with(density) { BUBBLE_HEIGHT.toPx() }
    Box(
        modifier
            .align(Alignment.TopEnd)
            .offset { IntOffset(-margin, (centerY - height / 2f).roundToInt()) }
            .height(BUBBLE_HEIGHT)
            .widthIn(min = BUBBLE_HEIGHT)
            .background(colorResource(R.color.accent), RoundedCornerShape(BUBBLE_HEIGHT / 2)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp),
            fontFamily = PermanentMarker,
            fontSize = if (label.length > 1) 20.sp else 32.sp,
            color = colorResource(R.color.on_accent),
        )
    }
}

private val STRIP_WIDTH = 30.dp
private val TOUCH_WIDTH = 44.dp
private val EDGE_MARGIN = 2.dp
private val MAX_LETTER_HEIGHT = 34.dp
private val THUMB_HEIGHT = 56.dp
private val THUMB_WIDTH = 10.dp
private val THUMB_MARGIN = 4.dp
private val GRAB_SLOP = 12.dp
private val BUBBLE_HEIGHT = 64.dp
private val BUBBLE_MARGIN = 40.dp
