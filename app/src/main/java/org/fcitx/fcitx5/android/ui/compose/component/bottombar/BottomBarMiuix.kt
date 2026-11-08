/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.component.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.ui.compose.tabTag
import org.fcitx.fcitx5.android.ui.compose.theme.LocalEnableFloatingBottomBar
import org.fcitx.fcitx5.android.ui.compose.theme.LocalEnableFloatingBottomBarBlur
import top.yukonga.miuix.kmp.basic.LocalNavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * The miuix bottom bar: inset and translucent (with a hairline border) when the floating bar is on,
 * docked otherwise. miuix's own `FloatingNavigationBar` is icon-only, so the floating variant is the
 * regular [NavigationBar] on a rounded surface with the selected tab's label shown.
 *
 * The liquid-glass variant of the template needs its backdrop wiring to be re-verified on-device
 * (it kept Compose permanently busy in the emulator test), so it is deliberately not used yet.
 */
@Composable
fun BottomBarMiuix(modifier: Modifier = Modifier, glass: GlassBackdrop? = null) {
    val mainState = LocalMainPagerState.current
    val floating = LocalEnableFloatingBottomBar.current
    val glassEnabled = LocalEnableFloatingBottomBarBlur.current
    val shape = RoundedCornerShape(26.dp)
    val surface = if (glassEnabled) colorScheme.surface.copy(alpha = 0.78f) else colorScheme.surface

    if (floating) {
        if (glassEnabled && glass != null) {
            // Blurred snapshot behind the bar; works from API 24 up (see LiquidGlassBar).
            LiquidGlassBar(
                backdrop = glass.bitmap,
                backdropScale = glass.scale,
                modifier = modifier,
            ) {
                CompositionLocalProvider(
                    LocalNavigationBarDisplayMode
                        provides NavigationBarDisplayMode.IconWithSelectedLabel
                ) {
                    NavigationBar(color = Color.Transparent) {
                        TabItems(mainState.selectedPage) { mainState.animateToPage(it) }
                    }
                }
            }
        } else {
            Box(
                modifier = modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clip(shape)
                    .background(surface)
                    .then(
                        if (glassEnabled) {
                            Modifier.border(1.dp, colorScheme.outline.copy(alpha = 0.35f), shape)
                        } else {
                            Modifier
                        }
                    ),
            ) {
                // The bar reads this local when its own display mode is left at the default.
                CompositionLocalProvider(
                    LocalNavigationBarDisplayMode
                        provides NavigationBarDisplayMode.IconWithSelectedLabel
                ) {
                    NavigationBar(color = Color.Transparent) {
                        TabItems(mainState.selectedPage) { mainState.animateToPage(it) }
                    }
                }
            }
        }
    } else {
        NavigationBar(color = surface, modifier = modifier) {
            TabItems(mainState.selectedPage) { mainState.animateToPage(it) }
        }
    }
}

/** The four tabs, shared by the floating and the docked bar. */
@Composable
internal fun RowScope.TabItems(selected: Int, onSelect: (Int) -> Unit) {
    MainDestination.entries.forEachIndexed { index, destination ->
        NavigationBarItem(
            selected = selected == index,
            onClick = { onSelect(index) },
            icon = destination.icon,
            label = stringResource(destination.label),
            modifier = Modifier.testTag(tabTag(index)),
        )
    }
}
