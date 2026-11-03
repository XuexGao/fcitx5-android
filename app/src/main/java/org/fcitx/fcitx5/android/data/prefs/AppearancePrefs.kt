/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs.CardStyle
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs.FunctionBarStyle

/**
 * Appearance preferences backing the new "界面" (Appearance) page.
 *
 * Every value here is a plain [ManagedPreference], so changes fire the usual
 * [ManagedPreferenceProvider.OnChangeListener] callbacks. The appearance page listens to them and
 * repaints immediately, which is what makes live preview work without restarting anything.
 */
class AppearancePrefs(sharedPreferences: SharedPreferences) : ManagedPreferenceProvider() {

    enum class CardStyle {
        /** Solid card with a soft shadow, closest to the KernelSU/Miuix look. */
        Elevated,

        /** Borderless flat surface. */
        Flat,

        /** Transparent card with a hairline outline. */
        Outlined
    }

    enum class KeyboardStyle {
        /** fcitx5-android's historical look: tight radius, square action keys. */
        Classic,

        /** Gboard-like: softer radius, tighter gaps, ripple feedback, pill action keys. */
        Gboard
    }

    enum class FunctionBarStyle {
        /** Icon only, like Gboard's compact tool row. */
        Icon,

        /** Text label only. */
        Label,

        /** Icon plus label, safest for discoverability. */
        IconAndLabel
    }

    val accentColor =
        ManagedPreference.PInt(sharedPreferences, KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR)
            .apply { register() }

    val cornerRadius =
        ManagedPreference.PInt(sharedPreferences, KEY_CORNER_RADIUS, DEFAULT_CORNER_RADIUS)
            .apply { register() }

    val spacing =
        ManagedPreference.PInt(sharedPreferences, KEY_SPACING, DEFAULT_SPACING)
            .apply { register() }

    val cardStyle =
        ManagedPreference.PString(sharedPreferences, KEY_CARD_STYLE, CardStyle.Elevated.name)
            .apply { register() }

    val functionBarStyle =
        ManagedPreference.PString(
            sharedPreferences, KEY_FUNCTION_BAR_STYLE, FunctionBarStyle.IconAndLabel.name
        ).apply { register() }

    val liquidGlass =
        ManagedPreference.PBool(sharedPreferences, KEY_LIQUID_GLASS, true)
            .apply { register() }

    val floatingBottomBar =
        ManagedPreference.PBool(sharedPreferences, KEY_FLOATING_BOTTOM_BAR, true)
            .apply { register() }

    val keyboardStyle =
        ManagedPreference.PString(sharedPreferences, KEY_KEYBOARD_STYLE, KeyboardStyle.Gboard.name)
            .apply { register() }

    /** Set once the default keyboard preset has been written, so user tweaks are never clobbered. */
    val keyboardStyleApplied =
        ManagedPreference.PBool(sharedPreferences, KEY_KEYBOARD_STYLE_APPLIED, false)
            .apply { register() }

    /** Card style as an enum, falling back to the default when the stored value is unknown. */
    val cardStyleValue: CardStyle
        get() = enumOrDefault(cardStyle.getValue(), CardStyle.entries, CardStyle.Elevated)

    /** Function bar style as an enum, falling back to the default. */
    val functionBarStyleValue: FunctionBarStyle
        get() = enumOrDefault(
            functionBarStyle.getValue(), FunctionBarStyle.entries, FunctionBarStyle.IconAndLabel
        )

    /** Keyboard style as an enum, falling back to the default. */
    val keyboardStyleValue: KeyboardStyle
        get() = enumOrDefault(
            keyboardStyle.getValue(), KeyboardStyle.entries, KeyboardStyle.Gboard
        )

    /**
     * Keys written by [resetToDefaults]. Kept explicit so a future preference is never
     * silently dropped from the reset path.
     */
    private val allKeys: List<ManagedPreference<*>>
        get() = listOf(
            accentColor, cornerRadius, spacing, cardStyle,
            functionBarStyle, liquidGlass, floatingBottomBar, keyboardStyle
        )

    fun resetToDefaults() {
        allKeys.forEach { it.setValue(it.defaultValue) }
        allKeys.forEach { fireChange(it.key) }
    }

    companion object {
        const val KEY_ACCENT_COLOR = "appearance_accent_color"
        const val KEY_CORNER_RADIUS = "appearance_corner_radius"
        const val KEY_SPACING = "appearance_spacing"
        const val KEY_CARD_STYLE = "appearance_card_style"
        const val KEY_FUNCTION_BAR_STYLE = "appearance_function_bar_style"
        const val KEY_LIQUID_GLASS = "appearance_liquid_glass"
        const val KEY_FLOATING_BOTTOM_BAR = "appearance_floating_bottom_bar"
        const val KEY_KEYBOARD_STYLE = "appearance_keyboard_style"
        const val KEY_KEYBOARD_STYLE_APPLIED = "appearance_keyboard_style_applied"

        /** KernelSU/Miuix-ish blue. */
        const val DEFAULT_ACCENT_COLOR = 0xFF0A84FF.toInt()
        const val DEFAULT_CORNER_RADIUS = 20
        const val DEFAULT_SPACING = 16

        /** Presets offered by the appearance page. */
        val ACCENT_PRESETS = listOf(
            0xFF0A84FF.toInt(), // Blue
            0xFF34C759.toInt(), // Green
            0xFFFF9F0A.toInt(), // Orange
            0xFFFF375F.toInt(), // Pink
            0xFFAF52DE.toInt(), // Purple
            0xFF00C7BE.toInt(), // Teal
            0xFFFFD60A.toInt(), // Yellow
            0xFF8E8E93.toInt()  // Graphite
        )

        val ALL_KEYS = listOf(
            KEY_ACCENT_COLOR, KEY_CORNER_RADIUS, KEY_SPACING, KEY_CARD_STYLE,
            KEY_FUNCTION_BAR_STYLE, KEY_LIQUID_GLASS, KEY_FLOATING_BOTTOM_BAR, KEY_KEYBOARD_STYLE
        )

        private var instance: AppearancePrefs? = null

        /**
         * The canonical instance lives in [AppPrefs], where it is registered as a provider so the
         * global `OnSharedPreferenceChangeListener` forwards changes to [ManagedPreferenceProvider]
         * listeners. That is what drives live preview.
         */
        fun getInstance(): AppearancePrefs =
            instance ?: AppPrefs.getInstance().appearance

        /**
         * Best-effort accessor usable from places that may run before [AppPrefs] is initialised
         * (for example a view previewed by the layout editor). Falls back to defaults rather than
         * throwing.
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

        private fun <T : Enum<T>> enumOrDefault(raw: String, values: List<T>, default: T): T =
            values.find { it.name == raw } ?: default
    }
}
