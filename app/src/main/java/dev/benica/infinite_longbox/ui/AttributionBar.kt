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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R

/** The data's license badge and a link to the issue on the Grand Comics Database. */
@Composable
fun AttributionBar(gcdIssueId: Int, modifier: Modifier = Modifier) {
    val uri = LocalUriHandler.current
    val licenseUrl = stringResource(R.string.url_cc_by_sa_4_0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.margin_default)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.cc_by_sa),
            contentDescription = stringResource(R.string.description_license_badge),
            modifier = Modifier
                .width(96.dp)
                .clickable(role = Role.Button) { uri.openUri(licenseUrl) }
                .padding(vertical = dimensionResource(R.dimen.margin_default)),
        )
        ExternalLink(stringResource(R.string.grand_comic_database_tm)) {
            uri.openUri("https://www.comics.org/issue/$gcdIssueId")
        }
    }
}

/** Link text followed by the "opens elsewhere" icon. */
@Composable
private fun ExternalLink(text: String, onClick: () -> Unit) {
    val color = colorResource(R.color.accent_strong)

    Row(
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.margin_narrow),
                vertical = dimensionResource(R.dimen.margin_default),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = CondensedBlock,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
        )
        Image(
            painter = painterResource(R.drawable.ic_open_in_new),
            contentDescription = stringResource(R.string.description_open_link),
            modifier = Modifier
                .padding(start = dimensionResource(R.dimen.margin_narrow))
                .size(18.dp),
            colorFilter = ColorFilter.tint(color),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AttributionBarPreview() {
    InfiniteLongboxTheme { AttributionBar(gcdIssueId = 1) }
}
