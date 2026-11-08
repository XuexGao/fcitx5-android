/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 *
 * Ported from KernelSU-Style-UI-Kit (Apache-2.0): the miuix side of the style switch.
 */
package org.fcitx.fcitx5.android.ui.compose.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.dynamiccolor.ColorSpec
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs.ColorMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/**
 * miuix flavour of the settings app.
 *
 * The controller is the one the template uses, so palette generation (including Monet and the
 * palette styles) behaves identically. The Monet choice is owned by [AppearancePrefs.miuixMonet]:
 * the stored colour mode is coerced to its Monet / non-Monet counterpart so the two switches stay
 * consistent, exactly as the template's `ThemeController.getAppSettings` does.
 */
/**
 * The miuix colour controller built from [prefs].
 *
 * Both styles use it: the miuix style for its own surfaces, and the Material style to keep the
 * handful of miuix widgets it still contains (icon tints, small titles) from falling back to
 * miuix's all-zero default palette.
 */
@Composable
fun miuixController(prefs: AppearancePrefs): ThemeController =
    miuixController(rememberAppearanceSnapshot(prefs))

/**
 * Builds the miuix controller from an already-observed [AppearanceSnapshot].
 *
 * Both styles use it: the miuix style for its own surfaces, and the Material style to keep the
 * handful of miuix widgets it still contains from falling back to miuix's all-zero default palette.
 */
@Composable
fun miuixController(appearance: AppearanceSnapshot): ThemeController {
    val systemDark = isSystemInDarkTheme()
    val monet = appearance.miuixMonet
    val storedMode = appearance.colorMode
    val colorMode = when {
        !monet && storedMode.isMonet -> ColorMode.fromValue(storedMode.toNonMonetMode())
        monet && !storedMode.isMonet -> ColorMode.fromValue(storedMode.toMonetMode())
        else -> storedMode
    }
    val darkTheme = colorMode.isDark || (colorMode.isSystem && systemDark)

    val paletteStyle = runCatching {
        ThemePaletteStyle.valueOf(parsePaletteStyle(appearance.paletteStyle).name)
    }.getOrDefault(ThemePaletteStyle.TonalSpot)
    val colorSpec = if (parseColorSpec(appearance.colorSpec) == ColorSpec.SpecVersion.SPEC_2025) {
        ThemeColorSpec.Spec2025
    } else {
        ThemeColorSpec.Spec2021
    }

    return ThemeController(
        colorSchemeMode = when (colorMode) {
            ColorMode.System -> ColorSchemeMode.System
            ColorMode.Light -> ColorSchemeMode.Light
            ColorMode.Dark -> ColorSchemeMode.Dark
            ColorMode.MonetSystem -> ColorSchemeMode.MonetSystem
            ColorMode.MonetLight -> ColorSchemeMode.MonetLight
            ColorMode.MonetDark, ColorMode.Amoled -> ColorSchemeMode.MonetDark
        },
        keyColor = appearance.keyColor.takeIf { it != 0 }?.let { Color(it) },
        isDark = darkTheme,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )
}

@Composable
fun FcitxMiuixTheme(
    appearance: AppearanceSnapshot,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val controller = miuixController(appearance)
    val darkTheme = controller.isDark == true

    MiuixTheme(
        controller = controller,
        content = {
            LaunchedEffect(darkTheme) {
                val window = (context as? Activity)?.window ?: return@LaunchedEffect
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            CompositionLocalProvider(
                LocalContentColor provides MiuixTheme.colorScheme.onBackground,
                content = content,
            )
        },
    )
}
