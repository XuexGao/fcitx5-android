/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.component.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.ui.compose.component.liquid.LiquidGlassNavigationBar
import org.fcitx.fcitx5.android.ui.compose.tabTag
import org.fcitx.fcitx5.android.ui.compose.theme.LocalEnableFloatingBottomBar
import org.fcitx.fcitx5.android.ui.compose.theme.LocalEnableFloatingBottomBarBlur
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.basic.LocalNavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * The miuix bottom bar.
 *
 * The floating variant is the template's own liquid-glass navigation bar
 * ([LiquidGlassNavigationBar], ported verbatim from compose-miuix-ui's example and adapted from
 * Kyant0/AndroidLiquidGlass): a frosted pill with a lens refraction, a moving specular highlight
 * and a press-to-scale indicator. It needs RenderEffect, so below API 31 the bar falls back to the
 * flat rounded translucent pill — the same look, minus the refraction.
 *
 * The docked variant is a plain [top.yukonga.miuix.kmp.basic.NavigationBar].
 */
@Composable
fun BottomBarMiuix(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    blurActive: Boolean = false,
) {
    val mainState = LocalMainPagerState.current
    val floating = LocalEnableFloatingBottomBar.current
    val glassEnabled = LocalEnableFloatingBottomBarBlur.current
    val items = MainDestination.entries.map { destination ->
        NavigationItem(label = stringResource(destination.label), icon = destination.icon)
    }

    if (floating && glassEnabled && backdrop != null) {
        LiquidGlassNavigationBar(
            items = items,
            selectedIndex = mainState.selectedPage,
            onItemClick = { mainState.animateToPage(it) },
            backdrop = backdrop,
            isBlurActive = blurActive,
            modifier = modifier,
        )
    } else {
        // Docked bar, or a device without RenderEffect: keep the previous rounded pill.
        val shape = RoundedCornerShape(26.dp)
        val surface = if (glassEnabled) colorScheme.surface.copy(alpha = 0.78f) else colorScheme.surface
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
            PlainNavigationBar(mainState.selectedPage) { mainState.animateToPage(it) }
        }
    }
}

/** The plain miuix navigation bar, used when the glass effect is unavailable. */
@Composable
private fun PlainNavigationBar(selected: Int, onSelect: (Int) -> Unit) {
    CompositionLocalProvider(
        LocalNavigationBarDisplayMode provides NavigationBarDisplayMode.IconWithSelectedLabel
    ) {
        NavigationBar(color = Color.Transparent) {
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
    }
}
