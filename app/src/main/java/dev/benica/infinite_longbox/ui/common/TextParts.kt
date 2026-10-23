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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.theme.PermanentMarker
/** A stand-in for a line of text still loading: a glow sweeps across it, like a loading indicator. */
@Composable
internal fun Bar(modifier: Modifier) {
    val base = colorResource(R.color.card_border).copy(alpha = 0.35f)
    val glow = colorResource(R.color.text_secondary).copy(alpha = 0.35f)
    val sweep by rememberInfiniteTransition(label = "barGlow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "barGlowSweep",
    )
    Box(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .drawWithCache {
                // A soft band wider than the bar, moving from off the left edge to off the right
                val band = size.width * 0.6f
                val x = -band + (size.width + band) * sweep
                val brush = Brush.horizontalGradient(
                    colors = listOf(base, glow, base),
                    startX = x,
                    endX = x + band,
                )
                onDrawBehind {
                    drawRect(base)
                    drawRect(brush)
                }
            }
    )
}

@Composable
internal fun Subtitle(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = PermanentMarker,
        fontSize = 16.sp,
    )
}
