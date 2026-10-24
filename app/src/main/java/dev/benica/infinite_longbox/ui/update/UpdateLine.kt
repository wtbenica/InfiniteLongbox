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

package dev.benica.infinite_longbox.ui.update

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.theme.Archivo

/** The part of the update under way: [key] says which, and [text] is what to show for it. */
data class UpdateStage(val key: String, val text: String)

/** How long one part fades into the next. */
const val STAGE_FADE_MS = 180

/** One line telling what the database update is doing: the title, and the part being fetched. */
@Composable
fun UpdateLine(title: String, stage: UpdateStage?, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(horizontal = dimensionResource(R.dimen.margin_default), vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontFamily = Archivo,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AnimatedContent(
            targetState = stage,
            contentKey = { it?.key },
            transitionSpec = { fadeIn(tween(STAGE_FADE_MS)) togetherWith fadeOut(tween(STAGE_FADE_MS)) },
            label = "updateStage",
        ) { shown ->
            Text(
                text = shown?.text.orEmpty(),
                fontFamily = Archivo,
                color = colorResource(R.color.accent_strong),
            )
        }
    }
}
