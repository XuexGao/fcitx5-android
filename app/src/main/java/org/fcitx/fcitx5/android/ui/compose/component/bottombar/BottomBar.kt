/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 *
 * Ported from KernelSU-Style-UI-Kit (Apache-2.0).
 */
package org.fcitx.fcitx5.android.ui.compose.component.bottombar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.fcitx.fcitx5.android.ui.compose.LocalUiMode
import org.fcitx.fcitx5.android.ui.compose.UiMode

@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    backdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop? = null,
    blurActive: Boolean = false,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> BottomBarMiuix(modifier, backdrop, blurActive)
        UiMode.Material -> BottomBarMaterial()
    }
}

@Composable
fun SideRail(modifier: Modifier = Modifier) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> NavigationRailMiuix(modifier)
        UiMode.Material -> NavigationRailMaterial(modifier)
    }
}
