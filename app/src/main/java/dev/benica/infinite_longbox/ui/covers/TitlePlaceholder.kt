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

package dev.benica.infinite_longbox.ui.covers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.models.FullIssue
import dev.benica.infinite_longbox.ui.theme.InfiniteLongboxTheme

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val shareTech = FontFamily(Font(GoogleFont("Share Tech"), fontProvider))

/** The blank cover in each of its colors */
private val artwork = listOf(
    R.drawable.placeholder_cyan,
    R.drawable.placeholder_yellow,
    R.drawable.placeholder_magenta,
)

/** The main color of each blank cover image (cyan, yellow, magenta), in the same order as [artwork] */
private val artworkColors = listOf(Color(0xFF68AEAE), Color(0xFFAEAE68), Color(0xFFAE68AE))

/** The color of the blank cover [placeholderArtworkFor] picks for [key]. */
fun placeholderColorFor(key: String): Color =
    artworkColors[Math.floorMod(key.trim().lowercase().hashCode(), artworkColors.size)]

/** The blank cover for [key]: the same key always gets the same one, others spread across all three. */
fun placeholderArtworkFor(key: String): Int =
    artwork[Math.floorMod(key.trim().lowercase().hashCode(), artwork.size)]

/**
 * What an issue's placeholder color is chosen from: series, number, cover date and variant, so
 * the issues of a series rotate through the colors instead of all matching their series.
 */
fun FullIssue.placeholderKey(): String =
    "${series.seriesName}|${issue.issueNumRaw}|${issue.coverDateLong ?: issue.coverDate}|${issue.variantName}"

// Where the blank masthead sits in the placeholder images (800 x 1234), as fractions of its size
private const val IMAGE_HEIGHT_PER_WIDTH = 1234f / 800f
private const val MASTHEAD_LEFT = 0.0325f
private const val MASTHEAD_RIGHT = 0.969f
private const val MASTHEAD_TOP = 0.0194f
private const val MASTHEAD_BOTTOM = 0.172f

/**
 * Stands in for a missing series cover: the blank cover with the series title in its masthead.
 *
 * The two halves can be drawn apart ([showArt], [showTitle]) so a card can put its wash between
 * them: the art is washed and the title stays crisp over it.
 */
@Composable
fun TitlePlaceholder(
    name: String,
    modifier: Modifier = Modifier,
    showArt: Boolean = true,
    showTitle: Boolean = true,
) {
    BoxWithConstraints(modifier.clipToBounds()) {
        val width = maxWidth
        val imageHeight = width * IMAGE_HEIGHT_PER_WIDTH

        // The series card washes its own cover area, so no second wash here
        if (showArt) {
            PlaceholderCover(
                key = name,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                scrim = false,
            )
        }
        if (!showTitle) return@BoxWithConstraints

        val mastheadHeight = imageHeight * (MASTHEAD_BOTTOM - MASTHEAD_TOP)

        Box(
            modifier = Modifier
                .offset(x = width * MASTHEAD_LEFT, y = imageHeight * MASTHEAD_TOP)
                .size(
                    width = width * (MASTHEAD_RIGHT - MASTHEAD_LEFT),
                    height = mastheadHeight,
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = colorResource(R.color.masthead_chip),
                shape = RoundedCornerShape(mastheadHeight / 6)
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                ) {
                    BasicText(
                        text = name,
                        style = TextStyle(
                            fontFamily = shareTech,
                            color = colorResource(R.color.masthead_ink),
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 40.sp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TitlePlaceholderPreview() {
    InfiniteLongboxTheme {
        TitlePlaceholder("Doom Patrol", Modifier.fillMaxSize())
    }
}
