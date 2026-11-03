/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.design

import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.data.theme.ThemeManager

/**
 * Gboard-style key appearance preset.
 *
 * The keyboard's own layout, key order and input behaviour are untouched — this only writes the
 * appearance knobs that the keyboard already exposes through [ThemeManager.prefs]:
 *
 *  * corner radius, so keys are softly rounded rather than square or fully round
 *  * horizontal/vertical gaps, matching Gboard's tighter grid
 *  * flat fills instead of borders, with a ripple press feedback
 *  * pill-shaped enter and "?123" keys
 *
 * Applied once on first run so a fresh install defaults to the Gboard look, and re-appliable from
 * the appearance page. Because the values live in the normal preferences, a user who dislikes the
 * preset can simply drag the sliders afterwards.
 */
object KeyboardStylePreset {

    /** Gboard-ish geometry. */
    const val GBOARD_KEY_RADIUS = 9
    const val GBOARD_H_MARGIN = 4
    const val GBOARD_V_MARGIN = 6

    /** The previous fcitx5-android defaults, kept so "经典" can restore them. */
    const val CLASSIC_KEY_RADIUS = 4
    const val CLASSIC_H_MARGIN = 3
    const val CLASSIC_V_MARGIN = 7

    fun applyGboard() {
        val prefs = ThemeManager.prefs
        prefs.keyRadius.setValue(GBOARD_KEY_RADIUS)
        prefs.keyHorizontalMargin.setValue(GBOARD_H_MARGIN)
        prefs.keyHorizontalMarginLandscape.setValue(GBOARD_H_MARGIN)
        prefs.keyVerticalMargin.setValue(GBOARD_V_MARGIN)
        prefs.keyVerticalMarginLandscape.setValue(4)
        prefs.keyBorder.setValue(false)
        prefs.keyBorderStroke.setValue(false)
        prefs.keyRippleEffect.setValue(true)
        prefs.specialKeyOvalShape.setValue(true)
        ThemeManager.notifyAppearanceChanged()
    }

    fun applyClassic() {
        val prefs = ThemeManager.prefs
        prefs.keyRadius.setValue(CLASSIC_KEY_RADIUS)
        prefs.keyHorizontalMargin.setValue(CLASSIC_H_MARGIN)
        prefs.keyHorizontalMarginLandscape.setValue(CLASSIC_H_MARGIN)
        prefs.keyVerticalMargin.setValue(CLASSIC_V_MARGIN)
        prefs.keyVerticalMarginLandscape.setValue(4)
        prefs.keyBorder.setValue(false)
        prefs.keyBorderStroke.setValue(false)
        prefs.keyRippleEffect.setValue(false)
        prefs.specialKeyOvalShape.setValue(false)
        ThemeManager.notifyAppearanceChanged()
    }

    fun apply(style: AppearancePrefs.KeyboardStyle) {
        when (style) {
            AppearancePrefs.KeyboardStyle.Gboard -> applyGboard()
            AppearancePrefs.KeyboardStyle.Classic -> applyClassic()
        }
    }

    /**
     * Writes the Gboard preset on a fresh install.
     *
     * Guarded by its own flag rather than `settings_initialized`, so existing installs that upgrade
     * into this build still get the new default once, and later user tweaks are never overwritten.
     */
    fun applyDefaultOnce() {
        val appearance = AppearancePrefs.getInstance()
        if (appearance.keyboardStyleApplied.getValue()) return
        apply(appearance.keyboardStyleValue)
        appearance.keyboardStyleApplied.setValue(true)
    }
}
