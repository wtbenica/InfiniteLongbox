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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.SortType
import dev.benica.infinite_longbox.database.models.FullAppearance
import dev.benica.infinite_longbox.database.models.FullCharacter
import dev.benica.infinite_longbox.database.models.FullCredit
import dev.benica.infinite_longbox.database.models.NameDetailAndCreator
import dev.benica.infinite_longbox.database.models.Story

private const val STORY_TYPE_COVER = 6

/** One story's box: its title, optional details (synopsis, characters) and credits. */
data class StoryUi(
    val storyId: Int,
    val title: String,
    val synopsis: String?,
    val characters: List<FullAppearance>,
    val credits: List<FullCredit>,
) {
    val hasDetails get() = !synopsis.isNullOrBlank() || characters.isNotEmpty()
}

/**
 * The stories to show for an issue, with the variant's. A variant with its own cover story
 * replaces the issue's cover story. Characters come out alphabetized.
 */
fun buildStoryUi(
    issueStories: List<Story>,
    variantStories: List<Story>,
    issueCredits: List<FullCredit>,
    variantCredits: List<FullCredit>,
    issueAppearances: List<FullAppearance>,
    variantAppearances: List<FullAppearance>,
): List<StoryUi> {
    val stories = if (variantStories.any { it.storyType == STORY_TYPE_COVER }) {
        issueStories.filter { it.storyType != STORY_TYPE_COVER } + variantStories
    } else {
        issueStories + variantStories
    }.sortedBy { it.storyType }

    val appearances = issueAppearances + variantAppearances
    val credits = issueCredits + variantCredits

    return stories.map { story ->
        StoryUi(
            storyId = story.storyId,
            title = storyTitle(story),
            synopsis = story.synopsis?.takeIf { it.isNotBlank() },
            characters = appearances
                .filter { it.appearance.story == story.storyId }
                .sortedBy { SortType.sortKeyOf(it.character.character.name) },
            credits = credits.filter { it.story.storyId == story.storyId },
        )
    }
}

private fun storyTitle(story: Story): String =
    if (story.storyType == STORY_TYPE_COVER) {
        if (!story.title.isNullOrEmpty()) "Cover - ${story.title}" else "Cover"
    } else {
        story.title.orEmpty().ifEmpty { "Untitled Story" }
    }

@Composable
fun CreditsBox(
    stories: List<StoryUi>,
    expanded: Set<Int>,
    onToggle: (Int) -> Unit,
    onCharacterClick: (FullCharacter) -> Unit,
    onCreatorClick: (NameDetailAndCreator) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        stories.forEach { story ->
            StoryBox(
                story = story,
                isExpanded = story.storyId in expanded,
                onToggle = { onToggle(story.storyId) },
                onCharacterClick = onCharacterClick,
                onCreatorClick = onCreatorClick,
            )
        }
    }
}

@Composable
private fun StoryBox(
    story: StoryUi,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onCharacterClick: (FullCharacter) -> Unit,
    onCreatorClick: (NameDetailAndCreator) -> Unit,
) {
    val narrow = dimensionResource(R.dimen.margin_narrow)
    val default = dimensionResource(R.dimen.margin_default)
    val shape = RoundedCornerShape(default)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = narrow, top = narrow, end = narrow, bottom = default + narrow)
            .animateContentSize()
    ) {
        // Title bar, with the expander when there's anything to expand
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.peek_height))
                .background(colorResource(R.color.accent_soft), shape)
                .padding(start = narrow),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = story.title.uppercase(),
                modifier = Modifier
                    .weight(1f)
                    .padding(narrow),
                color = colorResource(R.color.accent_strong),
                fontFamily = CondensedBlock,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            if (story.hasDetails) {
                val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "expand")
                Box(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.peek_height))
                        .clickable(role = Role.Button, onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.icon_arrow_down),
                        contentDescription = stringResource(R.string.story_dropdown_button),
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation),
                        colorFilter = ColorFilter.tint(colorResource(R.color.accent_strong)),
                    )
                }
            }
        }

        AnimatedVisibility(visible = isExpanded && story.hasDetails) {
            Column(Modifier.padding(narrow)) {
                story.synopsis?.let {
                    SectionTitle(stringResource(R.string.synopsis))
                    Text(
                        text = it,
                        modifier = Modifier.padding(default),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = CondensedBlock,
                        fontSize = 16.sp,
                    )
                }
                if (story.characters.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.characters))
                    Column(Modifier.padding(default), verticalArrangement = Arrangement.spacedBy(narrow)) {
                        story.characters.forEach { AppearanceRow(it, onCharacterClick) }
                    }
                }
            }
        }

        story.credits.forEach { CreditRow(it, onCreatorClick) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val narrow = dimensionResource(R.dimen.margin_narrow)
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                colorResource(R.color.surface_sunken),
                RoundedCornerShape(dimensionResource(R.dimen.margin_default))
            )
            .padding(narrow),
        color = colorResource(R.color.text_secondary),
        fontFamily = CondensedBlock,
        fontSize = 14.sp,
    )
}

@Composable
private fun CreditRow(credit: FullCredit, onCreatorClick: (NameDetailAndCreator) -> Unit) {
    val narrow = dimensionResource(R.dimen.margin_narrow)
    val creator = credit.nameDetail

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = credit.role.roleName.replaceFirstChar { it.uppercase() },
            modifier = Modifier
                .weight(1f)
                .padding(narrow)
                .background(colorResource(R.color.surface_sunken))
                .padding(vertical = narrow, horizontal = dimensionResource(R.dimen.margin_wide)),
            color = colorResource(R.color.text_secondary),
            fontFamily = CondensedBlock,
            fontSize = 14.sp,
        )
        Text(
            text = creatorName(creator),
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button) { onCreatorClick(creator) }
                .padding(horizontal = dimensionResource(R.dimen.margin_default), vertical = narrow),
            color = colorResource(R.color.accent_strong),
            fontFamily = CondensedBlock,
            fontSize = 14.sp,
        )
    }
}

private fun creatorName(creator: NameDetailAndCreator): String {
    val nameDetail = creator.nameDetail.name
    val name = creator.creator.name
    return if (nameDetail != name) "$name (as $nameDetail)" else name
}

@Composable
private fun AppearanceRow(appearance: FullAppearance, onCharacterClick: (FullCharacter) -> Unit) {
    val narrow = dimensionResource(R.dimen.margin_narrow)
    val character = appearance.character.character
    val name = character.alterEgo?.let { "${character.name} ($it)" } ?: character.name
    val link = colorResource(R.color.accent_strong)
    val plain = MaterialTheme.colorScheme.onSurface

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                modifier = Modifier.clickable(role = Role.Button) {
                    onCharacterClick(appearance.character)
                },
                color = link,
                fontFamily = CondensedBlock,
                fontSize = 14.sp,
            )
            appearance.appearance.notes?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(start = narrow),
                    color = plain,
                    fontFamily = CondensedBlock,
                    fontSize = 14.sp,
                )
            }
            appearance.appearance.details?.let {
                Box(
                    Modifier
                        .padding(horizontal = narrow)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(colorResource(R.color.accent_soft))
                )
                Text(text = it, color = plain, fontFamily = CondensedBlock, fontSize = 14.sp)
            }
        }
        appearance.appearance.membership?.let {
            Text(text = it, color = plain, fontFamily = CondensedBlock, fontSize = 14.sp)
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CreditsBoxPreview() {
    InfiniteLongboxTheme {
        CreditsBox(
            stories = listOf(
                StoryUi(1, "Cover", null, emptyList(), emptyList()),
                StoryUi(2, "Untitled Story", "A short synopsis.", emptyList(), emptyList()),
            ),
            expanded = setOf(2),
            onToggle = {}, onCharacterClick = {}, onCreatorClick = {},
        )
    }
}
