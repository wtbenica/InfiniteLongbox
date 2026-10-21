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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.FullCharacter
import dev.benica.infinite_longbox.database.models.Issue
import dev.benica.infinite_longbox.database.models.NameDetailAndCreator
import kotlinx.coroutines.ExperimentalCoroutinesApi

/** Everything the issue detail screen shows, gathered in one place. */
@OptIn(ExperimentalCoroutinesApi::class)
data class IssueDetailUiState(
    val cover: IssueCoverState = IssueCoverState(),
    val info: IssueInfoState = IssueInfoState(),
    val actions: IssueActionState = IssueActionState(),
    val variants: VariantSelectorState = VariantSelectorState(),
    val stories: List<StoryUi> = emptyList(),
    val jumpBar: JumpBarState = JumpBarState(),
    val gcdIssueId: Int = 0,
)

/** What the screen can ask for; the fragment decides what each one does. */
@OptIn(ExperimentalCoroutinesApi::class)
@Immutable
class IssueDetailActions(
    val onCoverClick: () -> Unit,
    val onCoverLongClick: () -> Unit,
    val onToggleCollection: () -> Unit,
    val onToggleWishList: () -> Unit,
    val onSearchEbay: () -> Unit,
    val onVariantSelected: (Issue) -> Unit,
    val onCharacterClick: (FullCharacter) -> Unit,
    val onCreatorClick: (NameDetailAndCreator) -> Unit,
    val onJump: (Int) -> Unit,
)

/**
 * The whole issue detail page: the cover beside its info and actions, the variant selector and
 * the credits scroll together; the jump bar and the attribution links stay at the bottom.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun IssueDetailScreen(
    state: IssueDetailUiState,
    actions: IssueDetailActions,
    modifier: Modifier = Modifier,
) {
    var expandedStories by remember { mutableStateOf(emptySet<Int>()) }
    val narrow = dimensionResource(R.dimen.margin_narrow)

    Column(
        modifier
            .fillMaxSize()
            .background(colorResource(R.color.surface))
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            CoverBeside(
                cover = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(
                                    dimensionResource(R.dimen.margin_default)
                                )
                                .cardShadow(RoundedCornerShape(8.dp)),
                            border = BorderStroke(4.dp, colorResource(R.color.card_border)),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            IssueCover(
                                state = state.cover,
                                onClick = actions.onCoverClick,
                                onLongClick = actions.onCoverLongClick,
                            )
                        }
                    }
                },
                beside = {
                    Column(Modifier.fillMaxSize()) {
                        IssueInfoBox(
                            state.info,
                            Modifier
                                .weight(1f)
                                .padding(top = narrow, end = narrow, bottom = narrow)
                        )
                        IssueActionBar(
                            state = state.actions,
                            onToggleCollection = actions.onToggleCollection,
                            onToggleWishList = actions.onToggleWishList,
                            onSearchEbay = actions.onSearchEbay,
                        )
                    }
                },
            )

            VariantSelector(
                state.variants,
                actions.onVariantSelected,
                Modifier.padding(top = dimensionResource(R.dimen.margin_default)),
            )

            CreditsBox(
                stories = state.stories,
                expanded = expandedStories,
                onToggle = { id ->
                    expandedStories =
                        if (id in expandedStories) expandedStories - id else expandedStories + id
                },
                onCharacterClick = actions.onCharacterClick,
                onCreatorClick = actions.onCreatorClick,
                modifier = Modifier.padding(narrow),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.surface_raised))
        ) {
            IssueJumpBar(state.jumpBar, actions.onJump)
            AttributionBar(
                if (state.gcdIssueId > 0) "https://www.comics.org/issue/${state.gcdIssueId}/" else GCD_HOME_URL
            )
        }
    }
}

/**
 * Two equal halves: the cover on the left, as tall as it is, and [beside] on the right at exactly
 * that height. The right side never makes the cover taller; it scrolls inside its own area.
 */
@Composable
private fun CoverBeside(
    cover: @Composable () -> Unit,
    beside: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(content = { cover(); beside() }, modifier = modifier) { measurables, constraints ->
        val half = constraints.maxWidth / 2
        val coverPlaceable = measurables[0].measure(Constraints.fixedWidth(half))
        val besidePlaceable = measurables[1].measure(Constraints.fixed(half, coverPlaceable.height))

        layout(constraints.maxWidth, coverPlaceable.height) {
            coverPlaceable.placeRelative(0, 0)
            besidePlaceable.placeRelative(half, 0)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Preview(showBackground = true)
@Composable
private fun IssueDetailScreenPreview() {
    InfiniteLongboxTheme {
        IssueDetailScreen(
            state = IssueDetailUiState(
                cover = IssueCoverState(placeholderKey = "Doom Patrol|8"),
                stories = listOf(StoryUi(1, "Cover", null, emptyList(), emptyList())),
                jumpBar = JumpBarState(position = 3, count = 20),
                gcdIssueId = 1,
            ),
            actions = IssueDetailActions({}, {}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}
