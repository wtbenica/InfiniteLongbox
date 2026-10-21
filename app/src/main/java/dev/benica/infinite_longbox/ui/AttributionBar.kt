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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R

/** Where the GCD's own site is, for a page that has no record of its own to link to. */
const val GCD_HOME_URL = "https://www.comics.org/"

/**
 * Says where the data on a page comes from, as the GCD asks: credited to the Grand Comics
 * Database™, linked to the page for [recordUrl] where there is one, with the license (CC BY-SA
 * 4.0) linked as well. The About screen has the full notice.
 */
@Composable
fun AttributionBar(recordUrl: String, modifier: Modifier = Modifier) {
    val linkStyle = TextLinkStyles(
        SpanStyle(
            color = colorResource(R.color.accent_strong),
            textDecoration = TextDecoration.Underline,
        )
    )
    val licenseUrl = stringResource(R.string.url_cc_by_sa_4_0)
    val prefix = stringResource(R.string.attribution_data_from)
    val gcd = stringResource(R.string.grand_comic_database_tm)
    val license = stringResource(R.string.attribution_license_name)

    val text = buildAnnotatedString {
        append(prefix)
        append(" ")
        withLink(LinkAnnotation.Url(recordUrl, linkStyle)) { append(gcd) }
        append(" · ")
        withLink(LinkAnnotation.Url(licenseUrl, linkStyle)) { append(license) }
    }

    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.margin_wide),
                vertical = dimensionResource(R.dimen.margin_default),
            ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = CondensedBlock,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AttributionBarPreview() {
    InfiniteLongboxTheme { AttributionBar(recordUrl = "https://www.comics.org/issue/1/") }
}
