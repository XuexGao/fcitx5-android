/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 *
 * Ported from KernelSU-Style-UI-Kit (Apache-2.0), with this app's four pages.
 */
package org.fcitx.fcitx5.android.ui.compose.component.bottombar

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import org.fcitx.fcitx5.android.R

/** The four top-level pages, in bar order. */
enum class MainDestination(
    @get:StringRes val label: Int,
    val icon: ImageVector,
) {
    Home(R.string.ksu_tab_home, Icons.Rounded.Home),
    Appearance(R.string.ksu_tab_appearance, Icons.Rounded.Palette),
    Features(R.string.ksu_tab_features, Icons.Rounded.Extension),
    Settings(R.string.ksu_tab_settings, Icons.Rounded.Settings);

    /** The same icon in its outlined form, used for the unselected Material bar items. */
    val outlinedIcon: ImageVector
        get() = when (this) {
            Home -> Icons.Outlined.Home
            Appearance -> Icons.Outlined.Palette
            Features -> Icons.Outlined.Extension
            Settings -> Icons.Outlined.Settings
        }
}
