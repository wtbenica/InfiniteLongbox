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

package dev.benica.infinite_longbox.ui.filter

import dev.benica.infinite_longbox.ui.common.SheetChip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.filter.SearchFilter
import dev.benica.infinite_longbox.filter.SortType
import dev.benica.infinite_longbox.database.models.BaseCollection
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.database.models.FilterItem
import dev.benica.infinite_longbox.database.models.FilterModel
import dev.benica.infinite_longbox.database.models.FilterType
import dev.benica.infinite_longbox.database.models.FullCreator
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.database.models.Series
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

/** What the sheet can ask for: the host turns each into a change of the filter. */
@ExperimentalCoroutinesApi
interface FilterSheetActions {
    fun myCollection(on: Boolean)
    fun wishList(on: Boolean)
    fun missingCover(on: Boolean)
    fun missingStorage(on: Boolean)
    fun showVariants(on: Boolean)
    fun nextView()
    fun sortBy(sort: SortType)
    fun add(item: FilterItem)
    fun remove(item: FilterItem)
    fun setFilterType(type: KClass<out FilterType>)
    fun pickDate(isStart: Boolean)
    fun setDate(date: LocalDate, isStart: Boolean)
    fun openHeader()
    suspend fun options(text: String): List<FilterModel>
}

/** The filter sheet: a strip saying what the list is filtered by, opening to dates, switches, sort, filter items and a search box. */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun FilterSheet(
    filter: SearchFilter,
    summary: String,
    slide: Float,
    expanded: Boolean,
    actions: FilterSheetActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .alpha(slide)
                .padding(dimensionResource(R.dimen.margin_wide))
        ) {
            DateRow(filter, actions)
            OptionRow(filter, actions)
            SortRow(filter, actions)
            FilterItems(filter, actions)
        }

        // Closed, the sections are still there (they give the sheet its height) but can't be touched
        if (slide == 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            )
        }

        if (!expanded) {
            FilterSheetHeader(summary, onClick = actions::openHeader, modifier = Modifier.alpha(1 - slide))
        }
    }
}

// ROWS

@Composable
private fun SectionRow(
    icon: Int,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val corner = RoundedCornerShape(dimensionResource(R.dimen.margin_default))
    Row(
        modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.margin_default))
            .height(IntrinsicSize.Min)
            .clip(corner)
            .background(colorResource(R.color.surface_sunken)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabelIcon(icon)
        Box(Modifier.weight(1f)) { content() }
        trailing?.invoke()
    }
}

/** The square at the start of a row, saying what the row is for */
@Composable
internal fun LabelIcon(icon: Int) {
    Box(
        Modifier
            .size(ROW_HEIGHT)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.margin_default)))
            .background(colorResource(R.color.accent_soft)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = colorResource(R.color.accent_strong))
    }
}

/** Chips in a line that scrolls sideways */
@Composable
private fun ChipLine(content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(R.dimen.margin_narrow))
            .height(ROW_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_narrow)),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun DateRow(filter: SearchFilter, actions: FilterSheetActions) {
    SectionRow(R.drawable.icon_label_dates) {
        ChipLine {
            DateChip(filter.mStartDate, isStart = true, actions)
            DateChip(filter.mEndDate, isStart = false, actions)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun DateChip(date: LocalDate, isStart: Boolean, actions: FilterSheetActions) {
    val unset = date == if (isStart) SearchFilter.MIN_DATE else SearchFilter.MAX_DATE
    val text = when {
        unset && isStart -> "Start Date"
        unset -> "End Date"
        else -> date.format(DATE_FORMAT).uppercase()
    }
    SheetChip(
        text = text,
        selected = true,
        onClick = { actions.pickDate(isStart) },
        onClose = if (unset) null else {
            { actions.setDate(if (isStart) LocalDate.MIN else LocalDate.MAX, isStart) }
        },
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun OptionRow(filter: SearchFilter, actions: FilterSheetActions) {
    val inMyCollection = filter.mMyCollection && filter.mCollectionId == BaseCollection.MY_COLL.id
    val onWishList = filter.mMyCollection && filter.mCollectionId == BaseCollection.WISH_LIST.id
    SectionRow(R.drawable.toggle_24) {
        ChipLine {
            SheetChip("My Collection", inMyCollection, { actions.myCollection(!inMyCollection) }, role = Role.Checkbox)
            SheetChip("Wish List", onWishList, { actions.wishList(!onWishList) }, role = Role.Checkbox)

            // What's missing from the collection: no cover on this phone, not filed in a storage place
            if (filter.mMyCollection) {
                SheetChip(
                    stringResource(R.string.filter_no_cover), filter.mMissingCover,
                    { actions.missingCover(!filter.mMissingCover) }, role = Role.Checkbox
                )
                if (filter.mCollectionId == BaseCollection.MY_COLL.id) {
                    SheetChip(
                        stringResource(R.string.filter_not_stored), filter.mMissingStorage,
                        { actions.missingStorage(!filter.mMissingStorage) }, role = Role.Checkbox
                    )
                }
            }

            SheetChip(viewLabel(filter.mViewOption), selected = true, onClick = actions::nextView)

            if (filter.mViewOption == FullIssue::class && !filter.mMyCollection) {
                SheetChip("Variants", filter.mShowVariants, { actions.showVariants(!filter.mShowVariants) }, role = Role.Checkbox)
            }
        }
    }
}

private fun viewLabel(view: KClass<*>) = when (view) {
    FullSeries::class -> "Series"
    FullIssue::class -> "Issues"
    Character::class -> "Characters"
    FullCreator::class -> "Creators"
    else -> "That's unusual..."
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun SortRow(filter: SearchFilter, actions: FilterSheetActions) {
    val current = filter.mSortType
    SectionRow(R.drawable.sort_24) {
        ChipLine {
            filter.getSortOptions().forEach { option ->
                val chosen = current != null && current.sortColumn == option.sortColumn
                // The chosen sort says which way it goes ("Date · Oldest → Newest"); the others only what they sort
                val shown = if (chosen) current else option
                SheetChip(
                    text = if (chosen) "${shown.tag} · ${shown.directionLabel}" else shown.tag,
                    selected = chosen,
                    onClick = { actions.sortBy(if (chosen) shown.toggle() else option) },
                    role = Role.RadioButton,
                )
            }
        }
    }
}

/** What the list is filtered by, one chip each. */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun FilterItems(filter: SearchFilter, actions: FilterSheetActions) {
    val active = filter.getAll()
    val switchedOff = remember { mutableStateListOf<FilterItem>() }
    var searching by remember { mutableStateOf(false) }

    // Any change of the filter closes the search, which is how a picked item gets out of the way
    LaunchedEffect(filter) {
        switchedOff.removeAll { it in active }
        searching = false
    }

    val items = active.toList() + switchedOff.filter { it !in active }
    val showSearch = items.isEmpty() || searching

    if (items.isNotEmpty()) {
        SectionRow(
            R.drawable.icon_label_filter,
            trailing = {
                if (!showSearch) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .background(
                                colorResource(R.color.accent),
                                RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                            )
                            .border(
                                1.dp, colorResource(R.color.accent_strong),
                                RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                            )
                            .clickable(
                                onClickLabel = stringResource(R.string.content_description_add_filter_button),
                                role = Role.Button,
                            ) { searching = true }
                            .padding(dimensionResource(R.dimen.margin_default)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.icon_plus_sign),
                            contentDescription = stringResource(R.string.content_description_add_filter_button),
                            tint = colorResource(R.color.on_accent),
                        )
                    }
                }
            },
        ) {
            ChipLine {
                items.forEach { item ->
                    val on = item in active
                    SheetChip(
                        text = item.toString(),
                        selected = on,
                        onClick = {
                            if (on) {
                                switchedOff.add(item)
                                actions.remove(item)
                            } else {
                                switchedOff.remove(item)
                                actions.add(item)
                            }
                        },
                        onClose = {
                            switchedOff.remove(item)
                            actions.remove(item)
                        },
                        role = Role.Checkbox,
                    )
                }
            }
        }
    }

    if (showSearch) SearchSection(actions)
}

internal val ROW_HEIGHT = 48.dp
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
