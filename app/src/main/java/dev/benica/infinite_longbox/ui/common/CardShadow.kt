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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import dev.benica.infinite_longbox.R

/** The one shadow cards and covers cast. */
@Composable
fun Modifier.cardShadow(shape: Shape): Modifier = dropShadow(
    shape = shape,
    shadow = Shadow(radius = 6.dp, color = colorResource(R.color.card_shadow)),
)

/** How every card in a list sits: the full width, with room around it for its shadow. */
@Composable
fun Modifier.listCardFrame(shape: Shape): Modifier = this
    .fillMaxWidth()
    .padding(dimensionResource(R.dimen.margin_default))
    .cardShadow(shape)
