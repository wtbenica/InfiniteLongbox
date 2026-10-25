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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.theme.CondensedBlock
import kotlinx.coroutines.launch
import java.io.File

/** Room for a title and a variant name of two lines each, and the position. */
private val CAPTION_HEIGHT = 124.dp

/** One cover for the viewer: what it is, and its file (null when the issue has none). */
data class ViewerCover(
    val issueId: Int,
    val title: String,
    val variantName: String,
    val file: File?,
    val placeholderKey: String,
)

/** The viewer's dialog runs under the system bars too; [CoverViewer] keeps its content clear of them. */
val ViewerDialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)

/** The covers as large as the screen allows, one at a time. */
@Composable
fun CoverViewer(
    covers: List<ViewerCover>,
    start: Int,
    onDismiss: (shown: Int) -> Unit,
    modifier: Modifier = Modifier,
    onPageChanged: (Int) -> Unit = {},
) {
    val margin = dimensionResource(R.dimen.margin_default)
    val pager = rememberPagerState(initialPage = start.coerceIn(0, covers.lastIndex)) { covers.size }
    val scope = rememberCoroutineScope()
    val shown = covers[pager.currentPage]
    LaunchedEffect(pager.currentPage) { onPageChanged(pager.currentPage) }
    val close = { onDismiss(pager.currentPage) }

    Column(
        modifier
            .fillMaxSize()
            .background(colorResource(R.color.surface_raised))
            .systemBarsPadding()
            .padding(margin),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { covers[it].issueId }) { page ->
                val cover = covers[page]
                val tap = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close)
                if (cover.file != null) {
                    AsyncImage(
                        model = cover.file,
                        contentDescription = stringResource(R.string.description_issue_cover_set),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().then(tap),
                    )
                } else {
                    PlaceholderCover(
                        key = cover.placeholderKey,
                        modifier = Modifier.fillMaxSize().then(tap),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center,
                    )
                }
            }
            if (pager.currentPage > 0) {
                Arrow("‹", Alignment.CenterStart, stringResource(R.string.cover_viewer_previous)) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                }
            }
            if (pager.currentPage < covers.lastIndex) {
                Arrow("›", Alignment.CenterEnd, stringResource(R.string.cover_viewer_next)) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }
            }
        }
        // A fixed height, whatever the text
        Column(
            Modifier
                .padding(margin)
                .fillMaxWidth()
                .height(CAPTION_HEIGHT)
                .background(colorResource(R.color.accent_soft), RoundedCornerShape(dimensionResource(R.dimen.margin_wide)))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close)
                .padding(horizontal = dimensionResource(R.dimen.margin_wide), vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            CoverCaption(shown.title, maxLines = 2)
            if (shown.variantName.isNotBlank()) CoverCaption(shown.variantName, maxLines = 2)
            if (covers.size > 1) {
                CoverCaption(stringResource(R.string.cover_viewer_position, pager.currentPage + 1, covers.size), maxLines = 1)
            }
        }
    }
}

@Composable
private fun BoxScope.Arrow(symbol: String, alignment: Alignment, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .align(alignment)
            .semantics { contentDescription = description }
            .padding(4.dp)
            .size(40.dp)
            .background(colorResource(R.color.surface_raised).copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CoverCaption(text: String, maxLines: Int) {
    Text(
        text = text,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontFamily = CondensedBlock,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
