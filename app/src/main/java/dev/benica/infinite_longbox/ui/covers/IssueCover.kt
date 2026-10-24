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

package dev.benica.infinite_longbox.ui.covers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.benica.infinite_longbox.R
import java.io.File

/** The cover file, or null when the issue has none. */
data class IssueCoverState(
    val file: File? = null,
    val placeholderKey: String = "",
    /** When the file last changed: a replaced cover keeps its name, and must still count as new. */
    val stamp: Long = file?.lastModified() ?: 0L,
)

/** The cover at the width given, as tall as the image is. */
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
        Box(clickable) {
            PlaceholderCover(
                key = state.placeholderKey,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                contentDescription = description,
            )
            AddCoverBadge(state.placeholderKey, Modifier.align(Alignment.BottomStart).padding(8.dp))
        }
    } else {
        val context = LocalContext.current
        val request = remember(file, file.lastModified()) { CoverImages.detailRequest(context, file) }
        // The last cover shown stays up while the next loads, rather than the title art flashing
        var previous by remember { mutableStateOf<Painter?>(null) }
        AsyncImage(
            model = request,
            contentDescription = stringResource(R.string.description_issue_cover_set),
            modifier = clickable,
            contentScale = ContentScale.FillWidth,
            placeholder = previous ?: painterResource(placeholderArtworkFor(state.placeholderKey)),
            onSuccess = { previous = it.painter },
        )
    }
}

/** Says the empty cover can be tapped to add one. */
@Composable
private fun AddCoverBadge(placeholderKey: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(placeholderColorFor(placeholderKey), CircleShape)
                .border(3.dp, colorResource(R.color.card_border), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add_photo),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = colorResource(R.color.card_border),
            )
        }
        Text(
            text = stringResource(R.string.cover_add_hint),
            modifier = Modifier
                .padding(top = 6.dp)
                .background(colorResource(R.color.card_tab), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
        )
    }
}
