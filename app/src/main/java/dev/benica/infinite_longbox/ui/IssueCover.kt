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

package dev.benica.infinite_longbox.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.benica.infinite_longbox.R
import java.io.File

/** The cover file, or null when the issue has none. */
data class IssueCoverState(val file: File? = null)

/**
 * The cover at the width it's given, as tall as the image is, so the screen is laid out around
 * it. The placeholder has a similar shape, so nothing jumps when the real cover loads.
 * Tap: view it, or add one if there isn't any. Long-press (when there is one): replace or remove.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IssueCover(
    state: IssueCoverState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val file = state.file
    val description = stringResource(R.string.description_issue_cover)
    val clickable = modifier
        .fillMaxWidth()
        .combinedClickable(
            role = Role.Image,
            onClick = onClick,
            onLongClick = if (file != null) onLongClick else null,
        )

    if (file == null) {
        Image(
            painter = painterResource(R.drawable.placeholder_gray),
            contentDescription = description,
            modifier = clickable,
            contentScale = ContentScale.FillWidth,
        )
    } else {
        // The file name stays the same when a cover is replaced, so key on its modified time
        val request = ImageRequest.Builder(LocalContext.current)
            .data(file)
            .memoryCacheKey("${file.path}@${file.lastModified()}")
            .crossfade(true)
            .build()
        AsyncImage(
            model = request,
            contentDescription = stringResource(R.string.description_issue_cover_set),
            modifier = clickable,
            contentScale = ContentScale.FillWidth,
            placeholder = painterResource(R.drawable.placeholder_gray),
        )
    }
}
