/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs.ColorMode
import org.fcitx.fcitx5.android.ui.compose.LocalUiMode
import org.fcitx.fcitx5.android.ui.compose.UiMode

/** Current colour mode as an Int, exposed so descendants can tell forced-dark from system. */
val LocalColorMode = staticCompositionLocalOf { ColorMode.System.value }

/** Whether frosted-glass surfaces are enabled by the user. */
val LocalEnableBlur = staticCompositionLocalOf { false }

val LocalEnableFloatingBottomBar = staticCompositionLocalOf { true }

val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { true }

@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean = when (LocalColorMode.current) {
    ColorMode.Light.value, ColorMode.MonetLight.value -> false
    ColorMode.Dark.value, ColorMode.MonetDark.value, ColorMode.Amoled.value -> true
    else -> isSystemInDarkTheme()
}

/**
 * Applies the appearance settings to the whole settings app.
 *
 * Two component styles are supported, selected by [AppearancePrefs.uiMode] and published through
 * [LocalUiMode]: the miuix look (the template's original) and a Material 3 look. Both build their
 * palette from the same preferences, so switching style keeps the user's colours.
 */
@Composable
fun FcitxTheme(
    prefs: AppearancePrefs,
    content: @Composable () -> Unit,
) {
    val uiMode = UiMode.fromValue(prefs.uiMode.getValue())
    val styledContent: @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalUiMode provides uiMode,
            LocalColorMode provides prefs.colorModeValue.value,
            LocalEnableBlur provides prefs.enableBlur.getValue(),
            LocalEnableFloatingBottomBar provides prefs.floatingBottomBar.getValue(),
            LocalEnableFloatingBottomBarBlur provides prefs.floatingBottomBarBlur.getValue(),
            content = content,
        )
    }
    when (uiMode) {
        UiMode.Miuix -> FcitxMiuixTheme(prefs = prefs, content = styledContent)
        UiMode.Material -> FcitxMaterialTheme(prefs = prefs, content = styledContent)
    }
}
