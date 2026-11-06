/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs.ColorMode

/**
 * Application-level appearance settings: the theme of the settings app itself, plus a few shell
 * options (blur, floating bottom bar).
 *
 * Deliberately separate from [org.fcitx.fcitx5.android.data.theme.ThemePrefs], which describes the
 * *keyboard* skin: "设置 → 外观" edits this class, "界面 → 键盘外观" edits the keyboard one.
 */
class AppearancePrefs(sharedPreferences: SharedPreferences) : ManagedPreferenceProvider() {

    /** How the settings app picks its colours. Mirrors KernelSU-Style-UI-Kit's ColorMode. */
    enum class ColorMode(val value: Int) {
        System(0),
        Light(1),
        Dark(2),
        MonetSystem(3),
        MonetLight(4),
        MonetDark(5),
        Amoled(6);

        val isSystem: Boolean get() = this == System || this == MonetSystem
        val isDark: Boolean get() = this == Dark || this == MonetDark || this == Amoled
        val isAmoled: Boolean get() = this == Amoled
        val isMonet: Boolean get() = value >= 3

        /** The matching mode without Monet, used when the miuix Monet switch is turned off. */
        fun toNonMonetMode(): Int = when (this) {
            MonetSystem -> System.value
            MonetLight -> Light.value
            MonetDark, Amoled -> Dark.value
            else -> value
        }

        /** The matching mode with Monet, used when the miuix Monet switch is turned on. */
        fun toMonetMode(): Int = when (this) {
            System -> MonetSystem.value
            Light -> MonetLight.value
            Dark -> MonetDark.value
            else -> value
        }

        companion object {
            fun fromValue(value: Int): ColorMode = entries.find { it.value == value } ?: System
        }
    }

    /**
     * Which component style the settings app is drawn with (miuix or Material 3), mirroring
     * KernelSU-Style-UI-Kit's `UiMode`.
     */
    val uiMode = ManagedPreference.PString(
        sharedPreferences, KEY_UI_MODE, UI_MODE_MIUIX
    ).apply { register() }

    /** Whether miuix's own Monet (dynamic colour) path is used instead of a seeded palette. */
    val miuixMonet = ManagedPreference.PBool(sharedPreferences, KEY_MIUIX_MONET, false)
        .apply { register() }

    /** Whether the wide-screen navigation rail is expanded; remembered across rotations. */
    val navigationRailExpanded =
        ManagedPreference.PBool(sharedPreferences, KEY_NAVIGATION_RAIL_EXPANDED, false)
            .apply { register() }

    val colorMode = ManagedPreference.PInt(
        sharedPreferences, KEY_COLOR_MODE, ColorMode.System.value
    ).apply { register() }

    /** Seed colour for the generated palette; 0 means "no custom seed". */
    val keyColor = ManagedPreference.PInt(sharedPreferences, KEY_KEY_COLOR, 0).apply { register() }

    /** materialKolor palette style name. */
    val paletteStyle = ManagedPreference.PString(
        sharedPreferences, KEY_PALETTE_STYLE, DEFAULT_PALETTE_STYLE
    ).apply { register() }

    /** materialKolor colour spec name. */
    val colorSpec = ManagedPreference.PString(
        sharedPreferences, KEY_COLOR_SPEC, DEFAULT_COLOR_SPEC
    ).apply { register() }

    /** Frosted-glass surfaces. Ignored automatically where RenderEffect is unavailable. */
    val enableBlur = ManagedPreference.PBool(sharedPreferences, KEY_ENABLE_BLUR, true)
        .apply { register() }

    val floatingBottomBar =
        ManagedPreference.PBool(sharedPreferences, KEY_FLOATING_BOTTOM_BAR, true)
            .apply { register() }

    val floatingBottomBarBlur =
        ManagedPreference.PBool(sharedPreferences, KEY_FLOATING_BOTTOM_BAR_BLUR, true)
            .apply { register() }

    /** Whether the "界面" page shows the live keyboard preview. */
    val showKeyboardPreview =
        ManagedPreference.PBool(sharedPreferences, KEY_SHOW_KEYBOARD_PREVIEW, true)
            .apply { register() }

    val colorModeValue: ColorMode
        get() = ColorMode.fromValue(colorMode.getValue())

    private val allKeys: List<ManagedPreference<*>>
        get() = listOf(
            uiMode, miuixMonet, navigationRailExpanded,
            colorMode, keyColor, paletteStyle, colorSpec,
            enableBlur, floatingBottomBar, floatingBottomBarBlur, showKeyboardPreview
        )

    fun resetToDefaults() {
        allKeys.forEach { it.setValue(it.defaultValue) }
        allKeys.forEach { fireChange(it.key) }
    }

    companion object {
        const val KEY_UI_MODE = "appearance_ui_mode"
        const val KEY_MIUIX_MONET = "appearance_miuix_monet"
        const val KEY_NAVIGATION_RAIL_EXPANDED = "appearance_navigation_rail_expanded"
        const val KEY_COLOR_MODE = "appearance_color_mode"
        const val KEY_KEY_COLOR = "appearance_key_color"
        const val KEY_PALETTE_STYLE = "appearance_palette_style"
        const val KEY_COLOR_SPEC = "appearance_color_spec"
        const val KEY_ENABLE_BLUR = "appearance_enable_blur"
        const val KEY_FLOATING_BOTTOM_BAR = "appearance_floating_bottom_bar"
        const val KEY_FLOATING_BOTTOM_BAR_BLUR = "appearance_floating_bottom_bar_blur"
        const val KEY_SHOW_KEYBOARD_PREVIEW = "appearance_show_keyboard_preview"

        const val DEFAULT_PALETTE_STYLE = "TonalSpot"
        const val DEFAULT_COLOR_SPEC = "Default"

        /** Palette styles offered in the UI (subset of materialKolor's list). */
        val PALETTE_STYLES = listOf(
            "TonalSpot", "Neutral", "Vibrant", "Expressive", "Content", "Fidelity",
            "Monochrome", "Rainbow", "FruitSalad"
        )

        /** Seed colours offered in the UI; 0 = let the system/library pick. */
        val KEY_COLOR_PRESETS = listOf(
            0,
            0xFF0A84FF.toInt(),
            0xFF34C759.toInt(),
            0xFFFF9F0A.toInt(),
            0xFFFF375F.toInt(),
            0xFFAF52DE.toInt(),
            0xFF00C7BE.toInt(),
            0xFFFFD60A.toInt()
        )

        const val UI_MODE_MIUIX = "miuix"
        const val UI_MODE_MATERIAL = "material"

        val ALL_KEYS = listOf(
            KEY_UI_MODE, KEY_MIUIX_MONET, KEY_NAVIGATION_RAIL_EXPANDED,
            KEY_COLOR_MODE, KEY_KEY_COLOR, KEY_PALETTE_STYLE, KEY_COLOR_SPEC,
            KEY_ENABLE_BLUR, KEY_FLOATING_BOTTOM_BAR, KEY_FLOATING_BOTTOM_BAR_BLUR,
            KEY_SHOW_KEYBOARD_PREVIEW
        )

        private var instance: AppearancePrefs? = null

        fun getInstance(): AppearancePrefs = instance ?: AppPrefs.getInstance().appearance

        /**
         * Best-effort accessor usable before [AppPrefs] is initialised (for example by a Compose
         * preview or a service that starts early).
         */
        fun ensureInitialized(context: Context): AppearancePrefs {
            instance?.let { return it }
            return runCatching { AppPrefs.getInstance().appearance }.getOrElse {
                synchronized(this) {
                    instance ?: AppearancePrefs(
                        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
                    ).also { instance = it }
                }
            }
        }
    }
}
