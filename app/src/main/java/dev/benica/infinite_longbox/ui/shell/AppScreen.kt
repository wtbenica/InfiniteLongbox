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
package dev.benica.infinite_longbox.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import dev.benica.infinite_longbox.MainActivity
import dev.benica.infinite_longbox.Notices
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.nav.Entry
import dev.benica.infinite_longbox.nav.Route
import dev.benica.infinite_longbox.nav.ScreenHost
import dev.benica.infinite_longbox.nav.ScreenLifecycle
import dev.benica.infinite_longbox.ui.shell.LocalSheetPeek
import dev.benica.infinite_longbox.ui.scan.ScanButton
import dev.benica.infinite_longbox.ui.toolbar.TopBar
import dev.benica.infinite_longbox.ui.update.HeaderStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi

/** The whole app: top bar and update line, the top screen, the filter sheet and scan button. */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun AppScreen(app: MainActivity) {
    val navigator = app.navigator
    val top = navigator.top
    // An issue's, character's or creator's page covers the lists, and takes the sheet and the scan button away
    val onPage = top?.isDetail == true
    val fullScreen = top?.isFullScreen == true

    val collapse = remember { BarCollapse() }
    collapse.enabled = !onPage
    LaunchedEffect(top?.key) { collapse.offset = 0f }

    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val peek = if (onPage) 0.dp else sheetPeek()

    BackHandler(enabled = navigator.stack.size > 1) { app.goBack() }

    CompositionLocalProvider(LocalSheetPeek provides peek) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colorResource(R.color.surface_raised))
                .nestedScroll(collapse.connection)
        ) {
            Column(Modifier.fillMaxSize()) {
                if (!fullScreen) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        CollapsingBar(collapse) {
                            TopBar(app.barTitle, app.barSubtitle, app.menuEntries(), app.barSelection)
                        }
                        HeaderStatus(app.progressUpdate, app.loading)
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(colorResource(R.color.surface))
                        // A list leaves room for the sheet under it; a page, for the system bar
                        .then(if (onPage && !fullScreen) Modifier.navigationBarsPadding() else Modifier)
                ) {
                    AnimatedContent(
                        targetState = top,
                        contentKey = { it?.key },
                        transitionSpec = { fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS)) },
                        label = "screen",
                    ) { entry -> if (entry != null) EntryContent(app, entry) }
                }
            }

            FilterSheetLayer(
                host = app.filterSheet,
                hidden = onPage,
                open = sheetOpen,
                onOpenChange = { sheetOpen = it },
            )

            val snackbars = remember { SnackbarHostState() }
            LaunchedEffect(Unit) {
                Notices.notices.collect {
                    snackbars.showSnackbar(it.text, duration = if (it.long) SnackbarDuration.Long else SnackbarDuration.Short)
                }
            }
            SnackbarHost(
                hostState = snackbars,
                modifier = Modifier
                    .align(if (fullScreen) Alignment.TopCenter else Alignment.BottomCenter)
                    .then(
                        when {
                            fullScreen -> Modifier.statusBarsPadding()
                            // Above the jump bar and the attribution under an issue
                            top?.route is Route.Issue -> Modifier.padding(bottom = navigationBarHeight() + ISSUE_FOOTER)
                            else -> Modifier.padding(bottom = peek.coerceAtLeast(navigationBarHeight()))
                        }
                    )
                    .padding(dimensionResource(R.dimen.margin_default)),
            ) { data ->
                Snackbar(
                    data,
                    containerColor = colorResource(R.color.text_primary),
                    contentColor = colorResource(R.color.surface),
                    actionColor = colorResource(R.color.accent_soft),
                )
            }

            ScanButton(
                visible = !onPage && !sheetOpen,
                onClick = app::startBarcodeScan,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = dimensionResource(R.dimen.margin_default),
                        bottom = peek + dimensionResource(R.dimen.margin_default),
                    ),
            )
        }
    }
}

/** One screen of the stack, alive while it is on show. */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun EntryContent(app: MainActivity, entry: Entry) {
    val lifecycle = remember(entry) { ScreenLifecycle() }
    val screen = remember(entry) { app.createScreen(ScreenHost(app, entry, lifecycle)) }
    DisposableEffect(entry) {
        lifecycle.resume()
        screen.onShown()
        onDispose {
            screen.onHidden()
            lifecycle.destroy()
        }
    }
    screen.Content()
}

private const val FADE_MS = 200
private val ISSUE_FOOTER = 88.dp

@Composable
private fun navigationBarHeight() = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
