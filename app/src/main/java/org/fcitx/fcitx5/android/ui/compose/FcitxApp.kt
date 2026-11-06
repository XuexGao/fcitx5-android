/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import org.fcitx.fcitx5.android.ui.compose.screens.AddonDirProfileScreen
import org.fcitx.fcitx5.android.ui.compose.screens.AddonScreen
import org.fcitx.fcitx5.android.ui.compose.screens.AddonConfigScreen
import org.fcitx.fcitx5.android.ui.compose.screens.GlobalConfigScreen
import org.fcitx.fcitx5.android.ui.compose.screens.InputMethodConfigScreen
import org.fcitx.fcitx5.android.ui.compose.screens.IconThemeScreen
import org.fcitx.fcitx5.android.ui.compose.screens.InputMethodListScreen
import org.fcitx.fcitx5.android.ui.compose.screens.KeyboardGroupScreen
import org.fcitx.fcitx5.android.ui.compose.screens.KeyboardSettingsScreen
import org.fcitx.fcitx5.android.ui.compose.screens.LicenseScreen
import org.fcitx.fcitx5.android.ui.compose.screens.MultiSelectScreen
import org.fcitx.fcitx5.android.ui.compose.screens.AdvancedScreen
import org.fcitx.fcitx5.android.ui.compose.screens.CandidatesScreen
import org.fcitx.fcitx5.android.ui.compose.screens.ClipboardScreen
import org.fcitx.fcitx5.android.ui.compose.screens.DeveloperScreen
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
import org.fcitx.fcitx5.android.ui.compose.theme.LocalEnableFloatingBottomBar
import org.fcitx.fcitx5.android.ui.compose.theme.LocalEnableFloatingBottomBarBlur
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import top.yukonga.miuix.kmp.basic.LocalNavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
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
 * bottom bar. Detail screens are pushed onto [composeStack]; routes requested from outside Compose
 * arrive through [ComposeRouteHost].
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
        val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
        val scope = rememberCoroutineScope()
        val floating = LocalEnableFloatingBottomBar.current
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

        BackHandler(enabled = composeStack.isNotEmpty()) { closeTop() }
        BackHandler(enabled = composeStack.isEmpty() && pagerState.currentPage != 0) {
            scope.launch { pagerState.animateScrollToPage(0) }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets.systemBars
                    .add(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal),
            ) { innerPadding ->
                // Reserve the bar's height plus a comfortable gap so the last card is never hidden.
                val bottomInnerPadding = innerPadding.calculateBottomPadding() +
                    if (floating) 96.dp else 72.dp
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

            // Drawn after the content so the floating bar sits above the pages, with the content
            // scrolling underneath it.
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                BottomBar(
                    selected = pagerState.currentPage,
                    onSelect = { index ->
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                )
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

/**
 * The four-tab bar.
 *
 * The floating variant is the "liquid glass" look from the template: an inset, rounded, translucent
 * surface that content scrolls under, with the selected tab's label next to its icon. miuix's own
 * `FloatingNavigationBar` is icon-only (its item renders just an Image and uses the label as the
 * content description), so the floating variant is built from the regular [NavigationBar] with
 * [NavigationBarDisplayMode.IconWithSelectedLabel] on a rounded surface.
 */
@Composable
private fun BottomBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val floating = LocalEnableFloatingBottomBar.current
    val glass = LocalEnableFloatingBottomBarBlur.current
    val shape = RoundedCornerShape(26.dp)
    val surface = if (glass) colorScheme.surface.copy(alpha = 0.78f) else colorScheme.surface

    if (floating) {
        Box(
            modifier = modifier
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(shape)
                .background(surface)
                .then(
                    if (glass) Modifier.border(1.dp, colorScheme.outline.copy(alpha = 0.35f), shape)
                    else Modifier
                ),
        ) {
            // The bar reads this local when its own display mode is left at the default.
            CompositionLocalProvider(
                LocalNavigationBarDisplayMode provides NavigationBarDisplayMode.IconWithSelectedLabel
            ) {
                NavigationBar(color = Color.Transparent) {
                    TabItems(selected, onSelect)
                }
            }
        }
    } else {
        NavigationBar(color = surface, modifier = modifier) {
            TabItems(selected, onSelect)
        }
    }
}

/** The four tabs, shared by the floating and the docked bar. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItems(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
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
