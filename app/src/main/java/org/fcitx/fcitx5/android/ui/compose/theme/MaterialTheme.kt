/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 *
 * Ported from KernelSU-Style-UI-Kit (Apache-2.0): the Material 3 side of the style switch.
 */
package org.fcitx.fcitx5.android.ui.compose.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.rememberDynamicColorScheme
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs

/**
 * Material 3 flavour of the settings app: the palette is generated with materialKolor from the
 * user's seed colour (or from the system's dynamic colours when no seed is set), exactly like the
 * template does.
 *
 * Dynamic system colours only exist from Android 12 on, so below that a fixed seed stands in for
 * them; the rest of the pipeline (palette style, colour spec, AMOLED) is identical.
 */
@Composable
fun FcitxMaterialTheme(
    prefs: AppearancePrefs,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val colorMode = prefs.colorModeValue
    val darkTheme = colorMode.isDark || (colorMode.isSystem && systemDark)
    val amoled = colorMode.isAmoled

    val paletteStyle = parsePaletteStyle(prefs.paletteStyle.getValue())
    val colorSpec = parseColorSpec(prefs.colorSpec.getValue())

    val keyColor = prefs.keyColor.getValue()
    val dynamicSystemColor = keyColor == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = if (dynamicSystemColor) {
        val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        rememberDynamicColorScheme(
            seedColor = Color.Unspecified,
            isDark = darkTheme,
            isAmoled = amoled,
            style = paletteStyle,
            specVersion = colorSpec,
            primary = base.primary,
            secondary = base.secondary,
            tertiary = base.tertiary,
            neutral = base.surface,
            neutralVariant = base.surfaceVariant,
            error = base.error,
        )
    } else {
        // No seed colour on pre-Android-12 devices: fall back to the first preset.
        val seed = if (keyColor == 0) Color(AppearancePrefs.KEY_COLOR_PRESETS[1]) else Color(keyColor)
        rememberDynamicColorScheme(
            seedColor = seed,
            isDark = darkTheme,
            isAmoled = amoled,
            style = paletteStyle,
            specVersion = colorSpec,
        )
    }

    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
