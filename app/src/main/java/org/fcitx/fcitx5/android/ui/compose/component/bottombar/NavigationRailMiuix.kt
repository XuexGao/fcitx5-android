/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 *
 * Ported from KernelSU-Style-UI-Kit (Apache-2.0); the expanded/collapsed state is remembered in
 * this app's appearance preferences instead of the template's settings repository.
 */
package org.fcitx.fcitx5.android.ui.compose.component.bottombar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.ui.compose.component.rememberPref
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun NavigationRailMiuix(modifier: Modifier = Modifier) {
    val mainState = LocalMainPagerState.current
    val expanded = rememberPref(AppearancePrefs.getInstance().navigationRailExpanded)
    val items = MainDestination.entries.map { it to stringResource(it.label) }

    val state = rememberNavigationRailState(
        initialValue = if (expanded.value) NavigationRailValue.Expanded
        else NavigationRailValue.Collapsed,
    )
    LaunchedEffect(state.currentValue) {
        expanded.value = state.isExpanded
    }

    NavigationRail(
        modifier = modifier,
        state = state,
        color = MiuixTheme.colorScheme.surface,
        expandContentDescription = stringResource(R.string.nav_rail_expand),
        collapseContentDescription = stringResource(R.string.nav_rail_collapse),
    ) {
        items.forEachIndexed { index, (destination, label) ->
            NavigationRailItem(
                selected = mainState.selectedPage == index,
                onClick = { mainState.animateToPage(index) },
                icon = destination.icon,
                label = label,
            )
        }
    }
}
