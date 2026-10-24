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

package dev.benica.infinite_longbox.ui.theme

import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.benica.infinite_longbox.R

/** The condensed face used for small labels (the theme's fontBlock) */
val CondensedBlock = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))

/** The same face, bold: titles (a Typeface-made family can't be made bold by a weight) */
val CondensedBold = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))

/** The face of the update line and the filter options */
val Archivo = FontFamily(Font(R.font.archivo))

/** The series-title typeface */
val PermanentMarker = FontFamily(Font(R.font.permanent_marker))

/**
 * Maps the app's color roles (values/colors.xml, overridden in values-night) onto a Material 3
 * scheme, so Compose follows the same palette and dark mode as the View screens. Nothing here
 * picks a color: change a role in colors.xml and both worlds change.
 */
@Composable
fun InfiniteLongboxTheme(content: @Composable () -> Unit) {
    val surface = colorResource(R.color.surface)
    val raised = colorResource(R.color.surface_raised)
    val sunken = colorResource(R.color.surface_sunken)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val accent = colorResource(R.color.accent)
    val onAccent = colorResource(R.color.on_accent)
    val outline = colorResource(R.color.outline)

    val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = colors.copy(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = colorResource(R.color.accent_soft),
            onPrimaryContainer = colorResource(R.color.accent_strong),
            secondary = accent,
            onSecondary = onAccent,
            // Chips and the like that are "selected" use these: the accent's soft shade, not the default purple
            secondaryContainer = colorResource(R.color.accent_soft),
            onSecondaryContainer = colorResource(R.color.accent_strong),
            background = surface,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = sunken,
            onSurfaceVariant = textSecondary,
            surfaceContainer = raised,
            surfaceContainerHigh = raised,
            surfaceContainerLow = sunken,
            outline = outline,
            outlineVariant = outline,
        ),
        content = content
    )
}
