/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 *
 * Ported from KernelSU-Style-UI-Kit (Apache-2.0): the 主题设置 screen and its Miuix / Material
 * variants. The reference drives it with a SettingsViewModel; here the same state is read straight
 * from [AppearancePrefs], which is where this app already keeps those settings.
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs.ColorMode
import org.fcitx.fcitx5.android.ui.compose.LocalUiMode
import org.fcitx.fcitx5.android.ui.compose.UiMode
import org.fcitx.fcitx5.android.ui.compose.component.rememberPref
import org.fcitx.fcitx5.android.ui.compose.theme.parseColorSpec
import org.fcitx.fcitx5.android.ui.compose.theme.parsePaletteStyle

@Immutable
data class ColorPaletteUiState(
    /** Stored colour mode; the three Monet entries are stored three values above their plain twin. */
    val colorMode: Int,
    val currentColorMode: ColorMode,
    val currentPaletteStyle: PaletteStyle,
    val currentColorSpec: ColorSpec.SpecVersion,
    val miuixMonet: Boolean,
    val keyColor: Int,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
)

@Immutable
data class ColorPaletteScreenActions(
    val onBack: () -> Unit,
    val onSetThemeMode: (Int) -> Unit,
    val onSetMiuixMonet: (Boolean) -> Unit,
    val onSetKeyColor: (Int) -> Unit,
    val onSetColorMode: (ColorMode) -> Unit,
    val onSetColorStyle: (String) -> Unit,
    val onSetColorSpec: (String) -> Unit,
    val onSetEnableBlur: (Boolean) -> Unit,
    val onSetEnableFloatingBottomBar: (Boolean) -> Unit,
    val onSetEnableFloatingBottomBarBlur: (Boolean) -> Unit,
)

@Composable
fun ColorPaletteScreen(bottomInnerPadding: Dp, onBack: () -> Unit) {
    val prefs = AppearancePrefs.ensureInitialized(LocalContext.current)
    val colorMode = rememberPref(prefs.colorMode)
    val miuixMonet = rememberPref(prefs.miuixMonet)
    val keyColor = rememberPref(prefs.keyColor)
    val paletteStyle = rememberPref(prefs.paletteStyle)
    val colorSpec = rememberPref(prefs.colorSpec)
    val enableBlur = rememberPref(prefs.enableBlur)
    val floatingBottomBar = rememberPref(prefs.floatingBottomBar)
    val floatingBottomBarBlur = rememberPref(prefs.floatingBottomBarBlur)

    val state = ColorPaletteUiState(
        colorMode = colorMode.value,
        currentColorMode = ColorMode.fromValue(colorMode.value),
        currentPaletteStyle = parsePaletteStyle(paletteStyle.value),
        currentColorSpec = parseColorSpec(colorSpec.value),
        miuixMonet = miuixMonet.value,
        keyColor = keyColor.value,
        enableBlur = enableBlur.value,
        enableFloatingBottomBar = floatingBottomBar.value,
        enableFloatingBottomBarBlur = floatingBottomBarBlur.value,
    )
    val actions = ColorPaletteScreenActions(
        onBack = onBack,
        // The tab row only offers the three plain entries; while Monet is on they are stored three
        // values higher, mirroring the template's `setThemeMode`.
        onSetThemeMode = { index -> colorMode.value = if (miuixMonet.value) index + 3 else index },
        onSetMiuixMonet = { enabled ->
            miuixMonet.value = enabled
            val current = ColorMode.fromValue(colorMode.value)
            colorMode.value = when {
                enabled && !current.isMonet -> current.toMonetMode()
                !enabled && current.isMonet -> current.toNonMonetMode()
                else -> current.value
            }
        },
        onSetKeyColor = { keyColor.value = it },
        onSetColorMode = { colorMode.value = it.value },
        onSetColorStyle = { paletteStyle.value = it },
        onSetColorSpec = { colorSpec.value = it },
        onSetEnableBlur = { enableBlur.value = it },
        onSetEnableFloatingBottomBar = { floatingBottomBar.value = it },
        onSetEnableFloatingBottomBarBlur = { floatingBottomBarBlur.value = it },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> ColorPaletteScreenMiuix(state, actions, bottomInnerPadding)
        UiMode.Material -> ColorPaletteScreenMaterial(state, actions, bottomInnerPadding)
    }
}
