/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.ui.compose.pages.AppearancePage
import org.fcitx.fcitx5.android.ui.compose.pages.FeaturesPage
import org.fcitx.fcitx5.android.ui.compose.pages.HomePage
import org.fcitx.fcitx5.android.ui.compose.pages.SettingsPage
import org.fcitx.fcitx5.android.ui.compose.screens.AboutScreen
import org.fcitx.fcitx5.android.ui.compose.screens.AdvancedScreen
import org.fcitx.fcitx5.android.ui.compose.screens.CandidatesScreen
import org.fcitx.fcitx5.android.ui.compose.screens.ClipboardScreen
import org.fcitx.fcitx5.android.ui.compose.screens.DeveloperScreen
import org.fcitx.fcitx5.android.ui.compose.screens.RimeScreen
import org.fcitx.fcitx5.android.ui.compose.screens.SymbolScreen
import org.fcitx.fcitx5.android.ui.compose.theme.FcitxTheme
import org.fcitx.fcitx5.android.ui.compose.theme.LocalEnableFloatingBottomBar
import org.fcitx.fcitx5.android.ui.compose.theme.LocalEnableFloatingBottomBarBlur
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private const val PAGE_COUNT = 4

/** Test tag for a bottom-bar tab, used by the instrumented UI tests. */
fun tabTag(index: Int): String = "bottom_tab_$index"

/** One bottom-bar entry. */
private data class Tab(val icon: ImageVector, val labelRes: Int)

private val TABS = listOf(
    Tab(Icons.Rounded.Home, R.string.ksu_tab_home),
    Tab(Icons.Rounded.Palette, R.string.ksu_tab_appearance),
    Tab(Icons.Rounded.Extension, R.string.ksu_tab_features),
    Tab(Icons.Rounded.Settings, R.string.ksu_tab_settings),
)

/**
 * Root of the settings app.
 *
 * Layout mirrors KernelSU-Style-UI-Kit: a four-page pager with a floating, optionally frosted
 * bottom bar. Routes that already have a Compose screen are pushed inside Compose; the rest are
 * handed to [onOpenLegacyRoute], which the host activity renders in an overlay.
 */
@Composable
fun FcitxApp(
    prefs: AppearancePrefs,
    viewModel: MainViewModel,
    onOpenLegacyRoute: (SettingsRoute) -> Unit,
    onOpenActivity: (Class<*>) -> Unit,
) {
    FcitxTheme(prefs = prefs) {
        val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
        val scope = rememberCoroutineScope()
        val floating = LocalEnableFloatingBottomBar.current
        var composeDetail by remember { mutableStateOf<SettingsRoute?>(null) }

        val openRoute: (SettingsRoute) -> Unit = { route ->
            if (isComposeRoute(route)) composeDetail = route else onOpenLegacyRoute(route)
        }

        BackHandler(enabled = composeDetail != null) { composeDetail = null }
        BackHandler(enabled = composeDetail == null && pagerState.currentPage != 0) {
            scope.launch { pagerState.animateScrollToPage(0) }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    BottomBar(
                        selected = pagerState.currentPage,
                        onSelect = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                    )
                },
                contentWindowInsets = WindowInsets.systemBars
                    .add(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal),
            ) { innerPadding ->
                // The floating bar overhangs the content: reserve its height plus a comfortable gap
                // so the last card is never hidden behind it (visible in the emulator screenshots).
                val bottomInnerPadding = innerPadding.calculateBottomPadding() +
                    if (floating) 76.dp else 24.dp
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    when (page) {
                        0 -> HomePage(bottomInnerPadding, openRoute, onOpenActivity)
                        1 -> AppearancePage(bottomInnerPadding, openRoute, onOpenActivity)
                        2 -> FeaturesPage(bottomInnerPadding, openRoute, onOpenActivity)
                        else -> SettingsPage(bottomInnerPadding, openRoute)
                    }
                }
            }

            AnimatedVisibility(
                visible = composeDetail != null,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
            ) {
                val route = composeDetail
                if (route != null) {
                    ComposeDetailHost(
                        route = route,
                        viewModel = viewModel,
                        onBack = { composeDetail = null },
                        onOpenLegacyRoute = onOpenLegacyRoute,
                    )
                }
            }
        }
    }
}

/** Renders the Compose detail screen for [route]. */
@Composable
private fun ComposeDetailHost(
    route: SettingsRoute,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenLegacyRoute: (SettingsRoute) -> Unit,
) {
    val padding = 24.dp
    when (route) {
        is SettingsRoute.RimeSchemas -> RimeScreen(
            bottomInnerPadding = padding,
            onBack = onBack,
            onRedeploy = {
                // Triggering a deploy needs the running fcitx instance; the ViewModel owns it.
                viewModel.requestRimeRedeploy()
            },
        )

        is SettingsRoute.CandidatesWindow -> CandidatesScreen(padding, onBack)
        is SettingsRoute.Symbol -> SymbolScreen(padding, onBack)
        is SettingsRoute.Clipboard -> ClipboardScreen(padding, onBack)
        is SettingsRoute.Advanced -> AdvancedScreen(padding, onBack, viewModel)
        is SettingsRoute.Developer -> DeveloperScreen(padding, onBack)
        is SettingsRoute.About -> AboutScreen(
            bottomInnerPadding = padding,
            onBack = onBack,
            onOpenLicenses = { onOpenLegacyRoute(SettingsRoute.License) },
        )

        else -> onBack()
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val floating = LocalEnableFloatingBottomBar.current
    val barBlur = LocalEnableFloatingBottomBarBlur.current
    val color = if (barBlur) Color.Transparent else colorScheme.surface
    if (floating) {
        FloatingNavigationBar(color = color) {
            TABS.forEachIndexed { index, tab ->
                FloatingNavigationBarItem(
                    selected = selected == index,
                    onClick = { onSelect(index) },
                    icon = tab.icon,
                    label = stringResource(tab.labelRes),
                    modifier = Modifier.testTag(tabTag(index)),
                )
            }
        }
    } else {
        NavigationBar(color = color) {
            TABS.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = selected == index,
                    onClick = { onSelect(index) },
                    icon = tab.icon,
                    label = stringResource(tab.labelRes),
                    modifier = Modifier.testTag(tabTag(index)),
                )
            }
        }
    }
}
