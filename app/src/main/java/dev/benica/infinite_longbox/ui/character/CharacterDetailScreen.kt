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

package dev.benica.infinite_longbox.ui.character

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
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.fragments_view_models.CharacterDetailState
import dev.benica.infinite_longbox.repository.CharacterInfo
import dev.benica.infinite_longbox.repository.CharacterInfoText
import dev.benica.infinite_longbox.repository.IssueRef
import dev.benica.infinite_longbox.repository.RelatedCharacter
import dev.benica.infinite_longbox.repository.TeamRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import dev.benica.infinite_longbox.ui.common.AttributionBar
import dev.benica.infinite_longbox.ui.common.Subtitle
import dev.benica.infinite_longbox.ui.theme.PermanentMarker
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme
import dev.benica.infinite_longbox.ui.common.GCD_HOME_URL
import dev.benica.infinite_longbox.ui.common.Body
import dev.benica.infinite_longbox.ui.common.LinkRow
import dev.benica.infinite_longbox.ui.common.LoadFailed
import dev.benica.infinite_longbox.ui.common.Section
import dev.benica.infinite_longbox.ui.common.Subtitle

/** What the page can ask for; the fragment decides what each does. */
@Immutable
class CharacterDetailActions(
    val onFilter: () -> Unit,
    val onRetry: () -> Unit,
    val onCharacter: (Int) -> Unit,
    val onIssue: (Int) -> Unit,
)

/** Everything known about one character: who they are, who they go by, who they are with. */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun CharacterDetailScreen(
    state: CharacterDetailState,
    actions: CharacterDetailActions,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(R.dimen.margin_wide)
    val info = (state as? CharacterDetailState.Loaded)?.info
    val local: Character? = when (state) {
        is CharacterDetailState.Loading -> state.character
        is CharacterDetailState.Failed -> state.character
        is CharacterDetailState.Loaded -> null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(margin),
        verticalArrangement = Arrangement.spacedBy(margin),
    ) {
        Header(
            name = info?.name ?: local?.name.orEmpty(),
            alterEgo = info?.alterEgo ?: local?.alterEgo.orEmpty(),
            publisher = info?.publisher.orEmpty(),
            firstYear = info?.firstYear,
        )

        Button(onClick = actions.onFilter, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.character_filter_by))
        }

        when (state) {
            is CharacterDetailState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            is CharacterDetailState.Failed ->
                LoadFailed(stringResource(R.string.character_load_failed), actions.onRetry)

            is CharacterDetailState.Loaded -> Details(state.info, actions)
        }
    }
}

@Composable
private fun Header(name: String, alterEgo: String, publisher: String, firstYear: Int?) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_narrow))) {
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = PermanentMarker,
            fontSize = 30.sp,
        )
        if (alterEgo.isNotBlank()) {
            Text(
                text = alterEgo,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = PermanentMarker,
                fontSize = 20.sp,
            )
        }
        val facts = listOfNotNull(
            publisher.takeIf { it.isNotBlank() },
            firstYear?.let { stringResource(R.string.character_first_year, it) },
        ).joinToString(" | ")
        if (facts.isNotEmpty()) Subtitle(facts)
    }
}

@Composable
private fun Details(info: CharacterInfo, actions: CharacterDetailActions) {
    val gap = dimensionResource(R.dimen.margin_wide)
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        if (info.alternateNames.isNotEmpty()) {
            Section(stringResource(R.string.character_also_known_as)) {
                Body(info.alternateNames.joinToString(" · "))
            }
        }

        if (info.related.isNotEmpty()) {
            Section(stringResource(R.string.character_related)) {
                info.related.forEach { Related(it, actions.onCharacter) }
            }
        }

        if (info.teams.isNotEmpty()) {
            Section(stringResource(R.string.character_teams)) {
                info.teams.forEach { Team(it) }
            }
        }

        if (info.appearances > 0) {
            Section(stringResource(R.string.character_appearances)) {
                Body(CharacterInfoText.appearances(info.appearances, info.issues))
                IssueRow(
                    stringResource(R.string.character_first_appearance),
                    info.firstAppearance,
                    actions.onIssue
                )
                IssueRow(stringResource(R.string.character_origin), info.origin, actions.onIssue)
                IssueRow(stringResource(R.string.character_death), info.death, actions.onIssue)
            }
        }

        if (info.description.isNotBlank()) {
            Section(stringResource(R.string.character_about)) { Body(info.description) }
        }

        if (!info.linked && info.appearances == 0) {
            Body(stringResource(R.string.character_no_details))
        }

        AttributionBar(info.gcdId?.let { "https://www.comics.org/character/$it/" } ?: GCD_HOME_URL)
    }
}

@Composable
private fun Related(related: RelatedCharacter, onCharacter: (Int) -> Unit) {
    val id = related.characterId
    LinkRow(
        label = related.relation,
        text = CharacterInfoText.name(related.name, related.alterEgo),
        onClick = id?.let { { onCharacter(it) } },
    )
}

@Composable
private fun Team(team: TeamRef) {
    val membership = team.membership.takeIf { it != "formal" }.orEmpty()
    LinkRow(
        label = CharacterInfoText.years(team.yearJoined, team.yearLeft),
        text = listOf(team.name, membership).filter { it.isNotEmpty() }.joinToString(" · "),
        onClick = null,
    )
}

@Composable
private fun IssueRow(label: String, issue: IssueRef?, onIssue: (Int) -> Unit) {
    if (issue == null) return
    LinkRow(label, CharacterInfoText.issue(issue)) { onIssue(issue.issueId) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun CharacterDetailScreenPreview() {
    InfiniteLongboxTheme {
        CharacterDetailScreen(
            state = CharacterDetailState.Loaded(
                CharacterInfo(
                    characterId = 1, name = "Oracle", alterEgo = "Barbara Gordon", publisher = "DC",
                    appearances = 2270, issues = 1320, linked = true, firstYear = 1989,
                    firstAppearance = IssueRef(1, 2, "Suicide Squad", "23", "1989-01-00", 1989),
                    alternateNames = listOf("Orakel", "Batgirl"),
                    related = listOf(RelatedCharacter("alias of", 5, "Barbara Gordon", "", "")),
                    teams = listOf(
                        TeamRef(1, "Suicide Squad", 1989, null, "uncertain"),
                        TeamRef(2, "Birds of Prey", 1996, null, "formal"),
                    ),
                    description = "Paralyzed by the Joker, she became the DC universe's information broker.",
                )
            ),
            actions = CharacterDetailActions({}, {}, {}, {}),
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CharacterDetailLoadingPreview() {
    InfiniteLongboxTheme {
        CharacterDetailScreen(
            state = CharacterDetailState.Loading(
                Character(
                    characterId = 1,
                    name = "Oracle",
                    alterEgo = "Barbara Gordon",
                    publisher = 1
                )
            ),
            actions = CharacterDetailActions({}, {}, {}, {}),
        )
    }
}
