/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 *
 * Ported from KernelSU-Style-UI-Kit (Apache-2.0).
 */
package org.fcitx.fcitx5.android.ui.compose.component.bottombar

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun NavigationRailMaterial(modifier: Modifier = Modifier) {
    val mainState = LocalMainPagerState.current
    NavigationRail(modifier = modifier) {
        MainDestination.entries.forEachIndexed { index, destination ->
            val selected = mainState.selectedPage == index
            NavigationRailItem(
                selected = selected,
                onClick = { if (!selected) mainState.animateToPage(index) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.icon else destination.outlinedIcon,
                        contentDescription = stringResource(destination.label),
                    )
                },
                label = { Text(stringResource(destination.label)) },
            )
        }
    }
}
