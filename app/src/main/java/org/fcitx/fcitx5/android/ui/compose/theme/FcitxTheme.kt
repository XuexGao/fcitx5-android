/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.fcitx.fcitx5.android.ui.compose.UiMode

/**
 * Applies the appearance settings to the whole settings app.
 *
 * The preferences are read through [rememberAppearanceSnapshot], so any edit re-renders the tree
 * immediately instead of waiting for a restart. Two component styles are supported, selected by
 * `AppearancePrefs.uiMode` and published as [UiMode]: miuix (the template's original) and Material 3.
 * Both build their palette from the same values, so switching style keeps the user's colours.
 */
@Composable
fun FcitxTheme(
    prefs: org.fcitx.fcitx5.android.data.prefs.AppearancePrefs,
    content: @Composable () -> Unit,
) {
    val appearance = rememberAppearanceSnapshot(prefs)
    val styledContent: @Composable () -> Unit = {
        CompositionLocalProvider(
            org.fcitx.fcitx5.android.ui.compose.LocalUiMode provides appearance.uiMode,
            LocalColorMode provides appearance.colorMode.value,
            LocalEnableBlur provides appearance.enableBlur,
            LocalEnableFloatingBottomBar provides appearance.floatingBottomBar,
            LocalEnableFloatingBottomBarBlur provides appearance.floatingBottomBarBlur,
            content = content,
        )
    }
    when (appearance.uiMode) {
        UiMode.Miuix -> FcitxMiuixTheme(appearance = appearance, content = styledContent)
        UiMode.Material -> FcitxMaterialTheme(appearance = appearance, content = styledContent)
    }
}
