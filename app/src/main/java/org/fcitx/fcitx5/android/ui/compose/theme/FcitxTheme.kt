/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs.ColorMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

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
 * Applies the miuix theme built from [AppearancePrefs].
 *
 * The controller is the same one KernelSU-Style-UI-Kit uses, so the palette generation (including
 * the Monet/dynamic variants and the palette styles) behaves identically. Changing any appearance
 * preference recomposes the whole tree, which is what makes the settings app re-skin instantly.
 */
@Composable
fun FcitxTheme(
    prefs: AppearancePrefs,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorMode = prefs.colorModeValue
    val systemDark = isSystemInDarkTheme()
    val darkTheme = colorMode.isDark || (colorMode.isSystem && systemDark)

    val miuixPaletteStyle = runCatching {
        ThemePaletteStyle.valueOf(prefs.paletteStyle.getValue())
    }.getOrDefault(ThemePaletteStyle.TonalSpot)

    val miuixColorSpec = if (prefs.colorSpec.getValue() == "Spec2025") {
        ThemeColorSpec.Spec2025
    } else {
        ThemeColorSpec.Spec2021
    }

    val keyColor = prefs.keyColor.getValue().takeIf { it != 0 }?.let { Color(it) }

    val controller = ThemeController(
        colorSchemeMode = when (colorMode) {
            ColorMode.System -> ColorSchemeMode.System
            ColorMode.Light -> ColorSchemeMode.Light
            ColorMode.Dark -> ColorSchemeMode.Dark
            ColorMode.MonetSystem -> ColorSchemeMode.MonetSystem
            ColorMode.MonetLight -> ColorSchemeMode.MonetLight
            ColorMode.MonetDark, ColorMode.Amoled -> ColorSchemeMode.MonetDark
        },
        keyColor = keyColor,
        isDark = darkTheme,
        paletteStyle = miuixPaletteStyle,
        colorSpec = miuixColorSpec,
    )

    MiuixTheme(controller = controller) {
        LaunchedEffect(darkTheme) {
            val window = (context as? Activity)?.window ?: return@LaunchedEffect
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
        CompositionLocalProvider(
            LocalColorMode provides colorMode.value,
            LocalEnableBlur provides prefs.enableBlur.getValue(),
            LocalEnableFloatingBottomBar provides prefs.floatingBottomBar.getValue(),
            LocalEnableFloatingBottomBarBlur provides prefs.floatingBottomBarBlur.getValue(),
            content = content,
        )
    }
}
