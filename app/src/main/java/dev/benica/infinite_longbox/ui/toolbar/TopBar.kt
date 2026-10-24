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
package dev.benica.infinite_longbox.ui.toolbar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.theme.CondensedBlock
import dev.benica.infinite_longbox.ui.theme.CondensedBold

/** One thing the bar can do: a line in the overflow menu, or a button while issues are picked. */
class BarAction(val label: String, val onClick: () -> Unit)

/** Issues are picked: the bar says how many, offers [actions] for them, and [onClose] lets them go. */
class SelectionBar(val count: Int, val actions: List<BarAction>, val onClose: () -> Unit)

/**
 * The bar at the top of every screen: the title (and a smaller line under it) and the overflow
 * menu. While issues are picked it turns into [selection]'s count and actions.
 */
@Composable
fun TopBar(
    title: String,
    subtitle: String?,
    menu: List<BarAction>,
    selection: SelectionBar?,
    modifier: Modifier = Modifier,
) {
    val tint = colorResource(R.color.accent_strong)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selection != null) {
            Icon(
                painterResource(R.drawable.icon_close),
                contentDescription = stringResource(R.string.scan_done),
                modifier = Modifier
                    .size(40.dp)
                    .clickable(role = Role.Button, onClick = selection.onClose)
                    .padding(8.dp),
                tint = tint,
            )
            Text(
                stringResource(R.string.selected_count, selection.count),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                color = tint,
                fontFamily = CondensedBold,
                fontSize = 20.sp,
                maxLines = 1,
            )
            selection.actions.forEach { action ->
                TextButton(onClick = action.onClick) {
                    Text(action.label, color = tint, fontFamily = CondensedBlock, maxLines = 1)
                }
            }
        } else {
            Column(Modifier.weight(1f)) {
                Text(
                    title.uppercase(),
                    color = tint,
                    fontFamily = CondensedBold,
                    fontSize = 24.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        color = tint,
                        fontFamily = CondensedBlock,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            OverflowMenu(menu, tint)
        }
    }
}

@Composable
private fun OverflowMenu(entries: List<BarAction>, tint: androidx.compose.ui.graphics.Color) {
    var open by remember { mutableStateOf(false) }
    Box {
        Icon(
            painterResource(R.drawable.icon_more_vert),
            contentDescription = stringResource(R.string.more_options),
            modifier = Modifier
                .size(48.dp)
                .clickable(role = Role.Button) { open = true }
                .padding(12.dp),
            tint = tint,
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.label) },
                    onClick = {
                        open = false
                        entry.onClick()
                    },
                )
            }
        }
    }
}
