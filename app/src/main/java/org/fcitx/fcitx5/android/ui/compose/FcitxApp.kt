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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.ui.compose.component.bottombar.BottomBar
import org.fcitx.fcitx5.android.ui.compose.component.bottombar.LocalMainPagerState
import org.fcitx.fcitx5.android.ui.compose.component.bottombar.MainDestination
import org.fcitx.fcitx5.android.ui.compose.component.bottombar.SideRail
import org.fcitx.fcitx5.android.ui.compose.component.bottombar.rememberMainPagerState
import org.fcitx.fcitx5.android.ui.compose.component.bottombar.useNavigationRail
import org.fcitx.fcitx5.android.ui.compose.pages.AppearancePage
import org.fcitx.fcitx5.android.ui.compose.pages.FeaturesPage
import org.fcitx.fcitx5.android.ui.compose.pages.HomePage
import org.fcitx.fcitx5.android.ui.compose.pages.SettingsPage
import org.fcitx.fcitx5.android.ui.compose.screens.AboutScreen
import org.fcitx.fcitx5.android.ui.compose.screens.AddonConfigScreen
import org.fcitx.fcitx5.android.ui.compose.screens.AddonDirProfileScreen
import org.fcitx.fcitx5.android.ui.compose.screens.AddonScreen
import org.fcitx.fcitx5.android.ui.compose.screens.AdvancedScreen
import org.fcitx.fcitx5.android.ui.compose.screens.CandidatesScreen
import org.fcitx.fcitx5.android.ui.compose.screens.ClipboardScreen
import org.fcitx.fcitx5.android.ui.compose.screens.ColorPaletteScreen
import org.fcitx.fcitx5.android.ui.compose.screens.DeveloperScreen
import org.fcitx.fcitx5.android.ui.compose.screens.GlobalConfigScreen
import org.fcitx.fcitx5.android.ui.compose.screens.IconThemeScreen
import org.fcitx.fcitx5.android.ui.compose.screens.InputMethodConfigScreen
import org.fcitx.fcitx5.android.ui.compose.screens.InputMethodListScreen
import org.fcitx.fcitx5.android.ui.compose.screens.KeyboardGroupScreen
import org.fcitx.fcitx5.android.ui.compose.screens.KeyboardSettingsScreen
import org.fcitx.fcitx5.android.ui.compose.screens.LicenseScreen
import org.fcitx.fcitx5.android.ui.compose.screens.MultiSelectScreen
import org.fcitx.fcitx5.android.ui.compose.screens.PinyinCustomPhraseScreen
import org.fcitx.fcitx5.android.ui.compose.screens.PinyinDictionaryScreen
import org.fcitx.fcitx5.android.ui.compose.screens.PluginScreen
import org.fcitx.fcitx5.android.ui.compose.screens.PunctuationEditorScreen
import org.fcitx.fcitx5.android.ui.compose.screens.QuickPhraseEditScreen
import org.fcitx.fcitx5.android.ui.compose.screens.QuickPhraseListScreen
import org.fcitx.fcitx5.android.ui.compose.screens.RimeScreen
import org.fcitx.fcitx5.android.ui.compose.screens.SymbolScreen
import org.fcitx.fcitx5.android.ui.compose.screens.TableInputMethodScreen
import org.fcitx.fcitx5.android.ui.compose.screens.ThemeScreen
import org.fcitx.fcitx5.android.ui.compose.theme.FcitxTheme
import org.fcitx.fcitx5.android.ui.compose.theme.LocalEnableBlur
import org.fcitx.fcitx5.android.ui.compose.theme.LocalEnableFloatingBottomBar
import org.fcitx.fcitx5.android.ui.compose.theme.LocalEnableFloatingBottomBarBlur
import org.fcitx.fcitx5.android.ui.compose.util.rememberBlurBackdrop
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import timber.log.Timber
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/** Test tag for a bottom-bar tab, used by the instrumented UI tests. */
fun tabTag(index: Int): String = "bottom_tab_$index"

/**
 * Root of the settings app.
 *
 * Layout mirrors KernelSU-Style-UI-Kit: a pager with a floating, optionally frosted bottom bar
 * (or a navigation rail on wide windows) and a Compose back stack for the detail screens. The
 * component style itself — miuix or Material 3 — comes from [UiMode] via [FcitxTheme].
 */
@Composable
fun FcitxApp(
    prefs: AppearancePrefs,
    viewModel: MainViewModel,
    onOpenActivity: (Class<*>) -> Unit,
    onOpenIntent: (android.content.Intent) -> Unit,
    routeHost: ComposeRouteHost,
) {
    FcitxTheme(prefs = prefs) {
        val scope = rememberCoroutineScope()
        val uiMode = LocalUiMode.current
        val enableBlur = LocalEnableBlur.current
        val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
        val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
        val useRail = useNavigationRail(enableFloatingBottomBar)

        val pagerState = rememberPagerState(pageCount = { MainDestination.entries.size })
        val mainPagerState = rememberMainPagerState(pagerState, scope, animatePageChanges = !useRail)

        // The reference glass bar samples two backdrops: one for the bar's own surface and one
        // for its moving indicator. Both are provided here, above the pager.
        val glassEnabled = enableFloatingBottomBar && enableFloatingBottomBarBlur
        val blurBackdrop = rememberBlurBackdrop(enableBlur)
        val barBackdrop = rememberLayerBackdrop {
            drawRect(androidx.compose.ui.graphics.Color.Transparent)
            drawContent()
        }

        // Every settings screen is Compose, so opening a route is just a push.
        val composeStack = remember { mutableStateListOf<SettingsRoute>() }
        val openRoute: (SettingsRoute) -> Unit = { route -> composeStack.add(route) }
        val closeTop: () -> Unit = {
            if (composeStack.isNotEmpty()) composeStack.removeAt(composeStack.lastIndex)
        }

        // Routes requested from outside Compose (deep links, keyboard toolbar buttons, …).
        LaunchedEffect(routeHost) {
            snapshotFlow { routeHost.pending.value }.collect { route ->
                if (route != null) {
                    composeStack.add(route)
                    routeHost.pending.value = null
                }
            }
        }

        // The bar highlights the tapped page at once; a swipe updates the pager's own page.
        LaunchedEffect(mainPagerState.pagerState.currentPage) { mainPagerState.syncPage() }

        DisposableEffect(glassEnabled) {
            onDispose { if (!glassEnabled) glassBackdrop = null }
        }

        BackHandler(enabled = composeStack.isNotEmpty()) { closeTop() }
        BackHandler(
            enabled = composeStack.isEmpty() && mainPagerState.selectedPage != 0
        ) {
            mainPagerState.animateToPage(0)
        }

        CompositionLocalProvider(LocalMainPagerState provides mainPagerState) {
            val pageContent: @Composable (Dp) -> Unit = { bottomInnerPadding ->
                Box(
                    modifier = if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop)
                    else Modifier,
                ) {
                    HorizontalPager(
                        state = mainPagerState.pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (glassEnabled) Modifier.layerBackdrop(barBackdrop) else Modifier
                            ),
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
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (useRail) {
                    val railContent = @Composable {
                        Row(modifier = Modifier.fillMaxSize()) {
                            SideRail()
                            Box(modifier = Modifier.weight(1f)) {
                                pageContent(WindowInsets.systemBars
                                    .add(WindowInsets.displayCutout)
                                    .only(WindowInsetsSides.Bottom)
                                    .asPaddingValues()
                                    .calculateBottomPadding()
                                )
                            }
                        }
                    }
                    when (uiMode) {
                        UiMode.Material -> androidx.compose.material3.Scaffold { railContent() }
                        UiMode.Miuix -> top.yukonga.miuix.kmp.basic.Scaffold { railContent() }
                    }
                } else {
                    val bar: @Composable () -> Unit = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            BottomBar(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                backdrop = barBackdrop,
                                blurBackdrop = if (enableBlur) blurBackdrop else null,
                                blurActive = enableBlur,
                            )
                        }
                    }
                    val shellContent: @Composable (Dp) -> Unit = { innerBottom ->
                        pageContent(innerBottom)
                    }
                    when (uiMode) {
                        UiMode.Material -> androidx.compose.material3.Scaffold(bottomBar = bar) { inner ->
                            shellContent(inner.calculateBottomPadding())
                        }
                        UiMode.Miuix -> top.yukonga.miuix.kmp.basic.Scaffold(
                            bottomBar = bar,
                            contentWindowInsets = WindowInsets.systemBars
                                .add(WindowInsets.displayCutout)
                                .only(WindowInsetsSides.Horizontal),
                        ) { inner ->
                            shellContent(inner.calculateBottomPadding())
                        }
                    }
                }

                AnimatedVisibility(
                    visible = composeStack.isNotEmpty(),
                    enter = slideInHorizontally { it },
                    exit = slideOutHorizontally { it },
                ) {
                    val route = composeStack.lastOrNull()
                    if (route != null) {
                        // A fresh composition per stack entry keeps `remember`ed state from leaking
                        // between two screens that happen to share a route type.
                        key(composeStack.size) {
                            ComposeDetailHost(
                                route = route,
                                viewModel = viewModel,
                                onBack = closeTop,
                                onOpenRoute = openRoute,
                                onOpenActivity = onOpenActivity,
                                onOpenIntent = onOpenIntent,
                            )
                        }
                    }
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
    onOpenRoute: (SettingsRoute) -> Unit,
    onOpenActivity: (Class<*>) -> Unit,
    onOpenIntent: (android.content.Intent) -> Unit,
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
        is SettingsRoute.GlobalConfig -> GlobalConfigScreen(
            bottomInnerPadding = padding,
            onBack = onBack,
            viewModel = viewModel,
            onOpenRoute = onOpenRoute,
        )

        is SettingsRoute.InputMethodConfig -> InputMethodConfigScreen(
            uniqueName = route.uniqueName,
            title = route.name,
            bottomInnerPadding = padding,
            onBack = onBack,
            viewModel = viewModel,
            onOpenRoute = onOpenRoute,
        )

        is SettingsRoute.AddonConfig -> AddonConfigScreen(
            addon = route.uniqueName,
            title = route.name,
            bottomInnerPadding = padding,
            onBack = onBack,
            viewModel = viewModel,
            onOpenRoute = onOpenRoute,
        )

        is SettingsRoute.Theme -> ThemeScreen(bottomInnerPadding = padding, onBack = onBack)
        is SettingsRoute.Plugin -> PluginScreen(bottomInnerPadding = padding, onBack = onBack)
        is SettingsRoute.InputMethodList -> InputMethodListScreen(
            bottomInnerPadding = padding,
            onBack = onBack,
            viewModel = viewModel,
            onOpenRoute = onOpenRoute,
        )

        is SettingsRoute.MultiSelect -> MultiSelectScreen(
            title = route.title,
            addon = route.addon,
            path = route.path,
            option = route.option,
            min = route.min,
            bottomInnerPadding = padding,
            onBack = onBack,
            viewModel = viewModel,
        )

        is SettingsRoute.AddonDirProfileManager -> AddonDirProfileScreen(
            title = route.title,
            addon = route.addon,
            path = route.path,
            bottomInnerPadding = padding,
            onBack = onBack,
            viewModel = viewModel,
        )

        is SettingsRoute.License -> LicenseScreen(bottomInnerPadding = padding, onBack = onBack)

        is SettingsRoute.ColorPalette -> ColorPaletteScreen(
            bottomInnerPadding = padding,
            onBack = onBack,
        )

        is SettingsRoute.IconTheme -> IconThemeScreen(
            bottomInnerPadding = padding,
            onBack = onBack,
            onOpenIntent = onOpenIntent,
        )

        is SettingsRoute.VirtualKeyboard -> KeyboardSettingsScreen(
            bottomInnerPadding = padding,
            onBack = onBack,
            onOpenRoute = onOpenRoute,
        )

        is SettingsRoute.KeyboardGroup -> KeyboardGroupScreen(
            group = route.group,
            bottomInnerPadding = padding,
            onBack = onBack,
            onOpenActivity = onOpenActivity,
        )

        is SettingsRoute.PinyinDict -> PinyinDictionaryScreen(
            bottomInnerPadding = padding,
            onBack = onBack,
            viewModel = viewModel,
        )

        is SettingsRoute.PinyinCustomPhrase -> PinyinCustomPhraseScreen(
            title = stringResource(R.string.ksu_pinyin_custom_phrase),
            bottomInnerPadding = padding,
            onBack = onBack,
            viewModel = viewModel,
        )

        is SettingsRoute.Punctuation -> PunctuationEditorScreen(
            title = route.title,
            lang = route.lang,
            bottomInnerPadding = padding,
            onBack = onBack,
            viewModel = viewModel,
        )

        is SettingsRoute.TableInputMethods -> TableInputMethodScreen(
            bottomInnerPadding = padding,
            onBack = onBack,
        )

        is SettingsRoute.QuickPhraseList -> QuickPhraseListScreen(
            bottomInnerPadding = padding,
            onBack = onBack,
            viewModel = viewModel,
            onOpenRoute = onOpenRoute,
        )

        is SettingsRoute.QuickPhraseEdit -> QuickPhraseEditScreen(
            quickPhrase = route.param.quickPhrase,
            bottomInnerPadding = padding,
            onBack = onBack,
            viewModel = viewModel,
        )

        is SettingsRoute.AddonList -> AddonScreen(
            bottomInnerPadding = padding,
            onBack = onBack,
            viewModel = viewModel,
            onOpenRoute = onOpenRoute,
        )

        is SettingsRoute.About -> AboutScreen(
            bottomInnerPadding = padding,
            onBack = onBack,
            onOpenLicenses = { onOpenRoute(SettingsRoute.License) },
        )

        else -> onBack()
    }
}
