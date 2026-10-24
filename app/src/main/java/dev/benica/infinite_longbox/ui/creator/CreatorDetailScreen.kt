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

package dev.benica.infinite_longbox.ui.creator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.daos.SignedIssue
import dev.benica.infinite_longbox.database.models.Creator
import dev.benica.infinite_longbox.viewmodels.CreatorDetailState
import dev.benica.infinite_longbox.repository.CharacterInfoText
import dev.benica.infinite_longbox.repository.CreatorInfo
import dev.benica.infinite_longbox.repository.CreatorInfoText
import dev.benica.infinite_longbox.repository.IssueRef
import dev.benica.infinite_longbox.repository.LifeEvent
import dev.benica.infinite_longbox.repository.OtherName
import dev.benica.infinite_longbox.repository.RoleCount
import dev.benica.infinite_longbox.repository.SeriesCount
import dev.benica.infinite_longbox.ui.common.AttributionBar
import dev.benica.infinite_longbox.ui.common.Body
import dev.benica.infinite_longbox.ui.common.LinkRow
import dev.benica.infinite_longbox.ui.common.LoadFailed
import dev.benica.infinite_longbox.ui.common.Section
import dev.benica.infinite_longbox.ui.common.Subtitle
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import dev.benica.infinite_longbox.ui.theme.PermanentMarker

/** "Batman #25 (Tim Sale Cover)": which copy it is. */
private fun signedLabel(issue: SignedIssue): String = buildString {
    append(issue.series)
    if (issue.number.isNotBlank()) append(" #").append(issue.number)
    if (issue.variantName.isNotBlank()) append(" (").append(issue.variantName).append(")")
}

/** What the page can ask for; the screen decides what each does. */
@Immutable
class CreatorDetailActions(
    val onFilter: () -> Unit,
    val onRetry: () -> Unit,
    val onIssue: (Int) -> Unit,
)

/** Everything known about one creator: who they are, what they have been credited as, what they made. */
@Composable
fun CreatorDetailScreen(
    state: CreatorDetailState,
    actions: CreatorDetailActions,
    modifier: Modifier = Modifier,
    /** The copies in My Collection this creator signed. */
    signed: List<SignedIssue> = emptyList(),
) {
    val margin = dimensionResource(R.dimen.margin_wide)
    val info = (state as? CreatorDetailState.Loaded)?.info
    val local: Creator? = when (state) {
        is CreatorDetailState.Loading -> state.creator
        is CreatorDetailState.Failed -> state.creator
        is CreatorDetailState.Loaded -> null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(margin),
        verticalArrangement = Arrangement.spacedBy(margin),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_narrow))) {
            Text(
                text = info?.name ?: local?.name.orEmpty(),
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = PermanentMarker,
                fontSize = 30.sp,
            )
            info?.disambiguation?.takeIf { it.isNotBlank() }?.let { Subtitle(it) }
        }

        Button(onClick = actions.onFilter, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.creator_filter_by))
        }

        if (signed.isNotEmpty()) {
            Section(stringResource(R.string.creator_signed_copies)) {
                signed.forEach {
                    LinkRow(
                        label = "",
                        text = signedLabel(it),
                    ) { actions.onIssue(it.issueId) }
                }
            }
        }

        when (state) {
            is CreatorDetailState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            is CreatorDetailState.Failed ->
                LoadFailed(stringResource(R.string.creator_load_failed), actions.onRetry)
            is CreatorDetailState.Loaded -> Details(state.info, actions)
        }
    }
}

@Composable
private fun Details(info: CreatorInfo, actions: CreatorDetailActions) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_wide))) {
        val born = CreatorInfoText.lifeEvent(info.born)
        val died = CreatorInfoText.lifeEvent(info.died)
        if (born.isNotEmpty() || died.isNotEmpty()) {
            Section(stringResource(R.string.creator_life)) {
                if (born.isNotEmpty()) LinkRow(stringResource(R.string.creator_born), born, null)
                if (died.isNotEmpty()) LinkRow(stringResource(R.string.creator_died), died, null)
            }
        }

        if (info.otherNames.isNotEmpty()) {
            Section(stringResource(R.string.creator_other_names)) {
                Body(info.otherNames.joinToString(" · ") { CreatorInfoText.otherName(it) })
            }
        }

        if (info.credits > 0) {
            Section(stringResource(R.string.creator_credits)) {
                Body(CharacterInfoText.credits(info.credits, info.issues))
                if (info.roles.isNotEmpty()) Body(CreatorInfoText.roles(info.roles))
                val active = CreatorInfoText.active(info.firstCredit, info.latestCredit)
                if (active.isNotEmpty()) LinkRow(stringResource(R.string.creator_active), active, null)
                info.firstCredit?.let {
                    LinkRow(stringResource(R.string.creator_first_credit), CharacterInfoText.issue(it)) {
                        actions.onIssue(it.issueId)
                    }
                }
                info.latestCredit?.takeIf { it.issueId != info.firstCredit?.issueId }?.let {
                    LinkRow(stringResource(R.string.creator_latest_credit), CharacterInfoText.issue(it)) {
                        actions.onIssue(it.issueId)
                    }
                }
            }
        }

        if (info.topSeries.isNotEmpty()) {
            Section(stringResource(R.string.creator_most_credits)) {
                info.topSeries.forEach { Body(CreatorInfoText.series(it)) }
            }
        }

        if (info.bio.isNotBlank()) {
            Section(stringResource(R.string.creator_about)) { Body(info.bio) }
        }

        if (info.credits == 0 && info.bio.isBlank() && info.otherNames.isEmpty()) {
            Body(stringResource(R.string.creator_no_details))
        }

        // Creator ids are the GCD's own
        AttributionBar("https://www.comics.org/creator/${info.creatorId}/")
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun CreatorDetailScreenPreview() {
    InfiniteLongboxTheme {
        CreatorDetailScreen(
            state = CreatorDetailState.Loaded(
                CreatorInfo(
                    creatorId = 2, name = "Jack Kirby",
                    born = LifeEvent("Aug 28, 1917", "New York City, United States"),
                    died = LifeEvent("Feb 6, 1994", "Thousand Oaks, California, United States"),
                    bio = "American comic book artist, widely regarded as one of the medium's major innovators.",
                    otherNames = listOf(OtherName("Jacob Kurtzberg", "birth name"), OtherName("Jolly Jack", "pen name")),
                    credits = 18400, issues = 9200,
                    roles = listOf(RoleCount("Penciler", 9000), RoleCount("Writer", 4000)),
                    firstCredit = IssueRef(1, 2, "Red Raven Comics", "1", "1940-08-00", 1940),
                    latestCredit = IssueRef(3, 4, "Silver Star", "6", "1983-01-00", 1983),
                    topSeries = listOf(SeriesCount(1, "Fantastic Four", 102), SeriesCount(2, "Thor", 80)),
                )
            ),
            actions = CreatorDetailActions({}, {}, {}),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CreatorDetailLoadingPreview() {
    InfiniteLongboxTheme {
        CreatorDetailScreen(
            state = CreatorDetailState.Loading(Creator(creatorId = 2, name = "Jack Kirby", sortName = "Kirby, Jack")),
            actions = CreatorDetailActions({}, {}, {}),
        )
    }
}
