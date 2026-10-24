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

package dev.benica.infinite_longbox.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import dev.benica.infinite_longbox.nav.ScreenHost
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.viewmodels.SeriesListViewModel
import dev.benica.infinite_longbox.ui.covers.CoverImages
import dev.benica.infinite_longbox.ui.lists.SeriesCardState
import dev.benica.infinite_longbox.ui.lists.SeriesListItem
import dev.benica.infinite_longbox.ui.lists.SeriesListItemSkeleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File

/** The series, as cards: each with its first issue's cover, or failing that another issue's. */
@ExperimentalCoroutinesApi
class SeriesListScreen(host: ScreenHost) : ComposeListScreen<FullSeries>(host) {

    override val viewModel: SeriesListViewModel by lazy { host.entry.viewModel<SeriesListViewModel>() }
    override val minColSizeDp: Int
        get() = 600

    override fun onShown() {
        callback.setTitle()
    }

    override fun key(item: FullSeries): Any = item.series.seriesId

    @Composable
    override fun Skeleton(index: Int) = SeriesListItemSkeleton(placeholderKey = "series|$index")

    @Composable
    override fun Row(item: FullSeries, index: Int) {
        // What the card needs from the server, asked when it comes into view (and again each time it does)
        LaunchedEffect(item) { requestWhatTheCardNeeds(item) }

        val state = remember(item) {
            val firstIssue: FullIssue? = item.firstIssue
            val coverFile = coverFileOf(item)
            SeriesCardState(
                name = item.series.seriesName,
                publisher = item.publisher.publisher,
                dateRange = item.series.dateRange,
                format = item.series.publishingFormat?.lowercase().orEmpty(),
                cover = coverFile?.takeIf { it.exists() },
                loadingCover = !(firstIssue == null || coverFile?.exists() == true || firstIssue.cover != null),
            )
        }
        SeriesListItem(state, onClick = { (callback as SeriesListCallback).onSeriesSelected(item) })
    }

    private fun requestWhatTheCardNeeds(item: FullSeries) {
        val firstIssueId = item.series.firstIssue
        if (firstIssueId != null) {
            viewModel.updateIssue(
                firstIssueId,
                viewModel.filter.value?.mMyCollection?.let { !it } ?: true
            )
        } else if (item.series.issueCount < 10) {
            viewModel.updateIssuesBySeries(item)
        }

        val firstIssue: FullIssue? = item.firstIssue
        val coverFile = coverFileOf(item)

        // A record of a cover whose file is gone (the fallback issue isn't one the card asks for
        // above): ask for that issue, so the file comes back
        if (coverFile != null && !coverFile.exists()) {
            coverFile.name.removePrefix("COVER_").removeSuffix(".jpg").toIntOrNull()
                ?.let { viewModel.updateIssue(it) }
        }

        // No cover for the first issue: the card falls back to the series' earliest cover
        // (item.earliestCoverUri), and goes and gets one if the phone has none
        if (coverFile?.exists() != true &&
            shouldFetchSeriesCover(false, firstIssue != null, firstIssue?.cover != null)
        ) {
            viewModel.fetchSeriesCover(item.series.seriesId)
        }
    }

    override fun scrollLabel(item: FullSeries): String? = when {
        // A series only knows the year it began
        sortedByDate() -> item.series.startDate?.year?.toString()
        sortedByName() -> initialOf(item.compareValue)
        else -> null
    }

    override fun prefetch(item: FullSeries) {
        coverFileOf(item)?.let { CoverImages.prefetchSeries(context.applicationContext, it) }
    }

    /** The file of the cover a card shows: the first issue's, or failing that the series' earliest. */
    private fun coverFileOf(series: FullSeries): File? =
        (series.firstIssue?.coverUri ?: series.earliestCoverUri)?.path?.let { File(it) }

    interface SeriesListCallback : ScreenCallback {
        fun onSeriesSelected(series: FullSeries)
    }

}
