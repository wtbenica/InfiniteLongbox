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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.All
import dev.benica.infinite_longbox.database.models.Character
import dev.benica.infinite_longbox.database.models.FilterItem
import dev.benica.infinite_longbox.database.models.FilterModel
import dev.benica.infinite_longbox.database.models.FilterType
import dev.benica.infinite_longbox.database.models.FullSeries
import dev.benica.infinite_longbox.database.models.NameDetail
import dev.benica.infinite_longbox.database.models.Publisher
import dev.benica.infinite_longbox.database.models.Series
import dev.benica.infinite_longbox.database.models.TextFilter
import dev.benica.infinite_longbox.ui.theme.Archivo
import dev.benica.infinite_longbox.ui.theme.CondensedBlock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.reflect.KClass

private val FILTER_TYPES: List<Pair<Int, KClass<out FilterType>>> = listOf(
    R.string.filter_type_all to All.Companion::class,
    R.string.filter_type_series to Series.Companion::class,
    R.string.filter_type_creator to NameDetail.Companion::class,
    R.string.filter_type_character to Character.Companion::class,
    R.string.filter_type_publisher to Publisher.Companion::class,
)

/** Puts a popup above the box it is declared in, as wide as that box */
private object AboveAnchor : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ) = IntOffset(anchorBounds.left, (anchorBounds.top - popupContentSize.height).coerceAtLeast(0))
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
internal fun SearchSection(actions: FilterSheetActions) {
    val focusManager = LocalFocusManager.current
    var text by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf<KClass<out FilterType>>(All.Companion::class) }
    var options by remember { mutableStateOf(emptyList<FilterModel>()) }

    // Looks up the options again for every keystroke, and when the type changes
    LaunchedEffect(text, type, focused) {
        options = if (focused) actions.options(text) else emptyList()
    }

    fun add(item: FilterItem) {
        text = ""
        focusManager.clearFocus()
        actions.add(item)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.margin_default))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.margin_default)))
            .background(colorResource(R.color.surface_sunken))
    ) {
        // Only the types row comes and goes, so the other rows stay usable while searching
        if (focused) {
            var width by remember { mutableStateOf(0) }
            Box(Modifier.fillMaxWidth().onSizeChanged { width = it.width }) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = dimensionResource(R.dimen.margin_default)),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_narrow)),
                ) {
                    FILTER_TYPES.forEach { (label, kind) ->
                        SheetChip(
                            text = stringResource(label).uppercase(),
                            selected = type == kind,
                            onClick = {
                                type = kind
                                actions.setFilterType(kind)
                            },
                            role = Role.RadioButton,
                            stroke = 1.dp,
                            textSize = 14.sp,
                        )
                    }
                }
                if (options.isNotEmpty()) {
                    Popup(popupPositionProvider = AboveAnchor, properties = PopupProperties(focusable = false)) {
                        OptionList(options, with(LocalDensity.current) { width.toDp() }, ::add)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            LabelIcon(R.drawable.ic_search_24)
            SearchField(
                text = text,
                onChange = { text = it },
                onFocus = { focused = it },
                onDone = {
                    if (text.isNotBlank()) add(TextFilter(text)) else focusManager.clearFocus()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SearchField(
    text: String,
    onChange: (String) -> Unit,
    onFocus: (Boolean) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = text,
        onValueChange = onChange,
        modifier = modifier
            .height(ROW_HEIGHT)
            .onFocusChanged { onFocus(it.isFocused) },
        singleLine = true,
        textStyle = TextStyle(
            color = colorResource(R.color.text_primary),
            fontFamily = CondensedBlock,
            fontSize = 18.sp,
        ),
        cursorBrush = SolidColor(colorResource(R.color.accent)),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { field ->
            Box(
                Modifier.padding(start = dimensionResource(R.dimen.margin_default)),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty()) {
                    Text(
                        stringResource(R.string.search),
                        color = colorResource(R.color.text_secondary),
                        fontFamily = CondensedBlock,
                        fontSize = 18.sp,
                    )
                }
                field()
            }
        },
    )
}

/** What the typed text could mean: a series, a creator, a character, a publisher */
@Composable
private fun OptionList(options: List<FilterModel>, width: Dp, onPick: (FilterModel) -> Unit) {
    Surface(
        modifier = Modifier
            .width(width)
            .heightIn(max = 300.dp),
        shape = RoundedCornerShape(12.dp),
        color = colorResource(R.color.surface_raised),
        border = BorderStroke(1.dp, colorResource(R.color.outline)),
        shadowElevation = 4.dp,
    ) {
        LazyColumn {
            items(options) { option -> OptionItem(option) { onPick(option) } }
        }
    }
}

@Composable
private fun OptionItem(option: FilterModel, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = dimensionResource(R.dimen.margin_default), vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // What kind of thing this is: Series, Creator, Character, Publisher
            Text(
                option.tagName.uppercase(),
                modifier = Modifier
                    .background(colorResource(R.color.accent_soft), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                color = colorResource(R.color.accent_strong),
                fontFamily = Archivo,
                fontSize = 11.sp,
                maxLines = 1,
            )
            // e.g. "limited series" or the publishing format
            val format = (option as? FullSeries)?.series?.publishingFormat
            if (!format.isNullOrBlank()) {
                Text(
                    format.uppercase(),
                    modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 2.dp),
                    color = colorResource(R.color.text_secondary),
                    fontFamily = Archivo,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
        Text(
            option.toString(),
            modifier = Modifier.padding(top = 4.dp),
            color = colorResource(R.color.text_primary),
            fontFamily = Archivo,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

