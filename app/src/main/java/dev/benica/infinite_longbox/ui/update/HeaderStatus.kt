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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import dev.benica.infinite_longbox.R

/**
 * What sits under the toolbar: the database update's line, while an update runs, and below it a
 * thin bar that moves while anything is being loaded.
 */
@Composable
fun HeaderStatus(update: UpdateProgress, busy: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = update.visible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Surface(
                modifier = Modifier.padding(dimensionResource(R.dimen.margin_default)),
                shape = RoundedCornerShape(dimensionResource(R.dimen.radius_default)),
                color = colorResource(R.color.surface_raised),
            ) {
                UpdateLine(update.titleText, update.stage)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(colorResource(R.color.surface_sunken))
        ) {
            if (busy) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = colorResource(R.color.accent),
                    trackColor = colorResource(R.color.surface_sunken),
                )
            }
        }
    }
}
