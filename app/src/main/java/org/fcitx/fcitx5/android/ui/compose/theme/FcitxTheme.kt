/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.fcitx.fcitx5.android.ui.compose.UiMode

import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs.ColorMode
import org.fcitx.fcitx5.android.ui.compose.component.rememberPref

/** Current colour mode as an Int, exposed so descendants can tell forced-dark from system. */
val LocalColorMode = staticCompositionLocalOf { ColorMode.System.value }

/** Whether frosted-glass surfaces are enabled by the user. */
val LocalEnableBlur = staticCompositionLocalOf { false }

val LocalEnableFloatingBottomBar = staticCompositionLocalOf { true }

val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { true }

/**
 * Snapshot of every appearance preference the theme depends on.
 *
 * `ManagedPreference` is not Compose state: reading `prefs.colorMode.getValue()` during composition
 * does not subscribe to changes, which is why every theme edit used to need a restart. Reading them
 * through [rememberPref] (a real change listener) makes edits apply immediately.
 */
data class AppearanceSnapshot(
    val uiMode: UiMode,
    val colorMode: ColorMode,
    val miuixMonet: Boolean,
    val keyColor: Int,
    val paletteStyle: String,
    val colorSpec: String,
    val enableBlur: Boolean,
    val floatingBottomBar: Boolean,
    val floatingBottomBarBlur: Boolean,
)

/** Reads the appearance preferences reactively, so a change recomposes the caller. */
@Composable
fun rememberAppearanceSnapshot(prefs: AppearancePrefs): AppearanceSnapshot {
    val uiMode = rememberPref(prefs.uiMode)
    val colorMode = rememberPref(prefs.colorMode)
    val miuixMonet = rememberPref(prefs.miuixMonet)
    val keyColor = rememberPref(prefs.keyColor)
    val paletteStyle = rememberPref(prefs.paletteStyle)
    val colorSpec = rememberPref(prefs.colorSpec)
    val enableBlur = rememberPref(prefs.enableBlur)
    val floating = rememberPref(prefs.floatingBottomBar)
    val floatingBlur = rememberPref(prefs.floatingBottomBarBlur)
    return AppearanceSnapshot(
        uiMode = UiMode.fromValue(uiMode.value),
        colorMode = ColorMode.fromValue(colorMode.value),
        miuixMonet = miuixMonet.value,
        keyColor = keyColor.value,
        paletteStyle = paletteStyle.value,
        colorSpec = colorSpec.value,
        enableBlur = enableBlur.value,
        floatingBottomBar = floating.value,
        floatingBottomBarBlur = floatingBlur.value,
    )
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean = when (LocalColorMode.current) {
    ColorMode.Light.value, ColorMode.MonetLight.value -> false
    ColorMode.Dark.value, ColorMode.MonetDark.value, ColorMode.Amoled.value -> true
    else -> androidx.compose.foundation.isSystemInDarkTheme()
}

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
    prefs: AppearancePrefs,
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
