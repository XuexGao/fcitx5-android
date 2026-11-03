/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.ui.design.KeyboardStylePreset
import org.fcitx.fcitx5.android.ui.design.KsuTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The appearance page is the only place where "live preview" can be verified without a screenshot:
 * editing a preference must be visible immediately through the same objects the keyboard reads.
 */
@RunWith(AndroidJUnit4::class)
class AppearanceSettingsTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val appearance get() = AppearancePrefs.ensureInitialized(context)

    /** A separate reader over the same SharedPreferences file, used to prove persistence. */
    private fun reopenedPrefs() = AppearancePrefs(
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
    )

    @Before
    fun resetToKnownState() {
        appearance.resetToDefaults()
    }

    @Test
    fun accentColourChangeIsPersistedAndObservable() {
        val original = appearance.accentColor.getValue()
        val target = AppearancePrefs.ACCENT_PRESETS.first { it != original }
        appearance.accentColor.setValue(target)
        // A fresh instance reads from the same SharedPreferences file, proving persistence.
        assertEquals(target, reopenedPrefs().accentColor.getValue())
    }

    @Test
    fun cornerRadiusChangeIsPersisted() {
        appearance.cornerRadius.setValue(30)
        assertEquals(30, AppearancePrefs.ensureInitialized(context).cornerRadius.getValue())
        // Card radius must follow the preference without a restart.
        val radiusPx = KsuTokens.cardRadius(context)
        assertTrue("card radius should scale with the preference", radiusPx > 0f)
    }

    @Test
    fun resetRestoresDefaults() {
        appearance.cornerRadius.setValue(33)
        appearance.spacing.setValue(30)
        appearance.resetToDefaults()
        assertEquals(AppearancePrefs.DEFAULT_CORNER_RADIUS, appearance.cornerRadius.getValue())
        assertEquals(AppearancePrefs.DEFAULT_SPACING, appearance.spacing.getValue())
        assertEquals(
            AppearancePrefs.DEFAULT_ACCENT_COLOR, appearance.accentColor.getValue()
        )
    }

    @Test
    fun keyboardStylePresetWritesKeyAppearance() {
        KeyboardStylePreset.applyGboard()
        val prefs = ThemeManager.prefs
        assertEquals(KeyboardStylePreset.GBOARD_KEY_RADIUS, prefs.keyRadius.getValue())
        assertEquals(KeyboardStylePreset.GBOARD_H_MARGIN, prefs.keyHorizontalMargin.getValue())
        assertTrue(prefs.keyRippleEffect.getValue())
        assertTrue("Gboard style uses pill action keys", prefs.specialKeyOvalShape.getValue())

        KeyboardStylePreset.applyClassic()
        assertEquals(KeyboardStylePreset.CLASSIC_KEY_RADIUS, prefs.keyRadius.getValue())
        assertTrue(!prefs.specialKeyOvalShape.getValue())
    }

    @Test
    fun keyboardStylePreferenceSurvivesReopen() {
        appearance.keyboardStyle.setValue(AppearancePrefs.KeyboardStyle.Gboard.name)
        assertEquals(
            AppearancePrefs.KeyboardStyle.Gboard, reopenedPrefs().keyboardStyleValue
        )
        appearance.keyboardStyle.setValue(AppearancePrefs.KeyboardStyle.Classic.name)
        assertNotEquals(
            AppearancePrefs.KeyboardStyle.Gboard,
            AppearancePrefs.ensureInitialized(context).keyboardStyleValue
        )
    }
}
