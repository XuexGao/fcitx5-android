/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui

import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The settings app's own appearance must persist, and the keyboard appearance preferences the
 * "界面" page edits must round-trip too.
 */
@RunWith(AndroidJUnit4::class)
class AppearanceSettingsTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val appearance get() = AppearancePrefs.ensureInitialized(context)

    /** A separate reader over the same file, used to prove persistence. */
    private fun reopenedPrefs() = AppearancePrefs(
        PreferenceManager.getDefaultSharedPreferences(context)
    )

    @Before
    fun resetToKnownState() {
        appearance.resetToDefaults()
    }

    @Test
    fun appThemeSettingsPersist() {
        appearance.colorMode.setValue(AppearancePrefs.ColorMode.Dark.value)
        appearance.paletteStyle.setValue("Vibrant")
        appearance.keyColor.setValue(0xFF34C759.toInt())
        val reopened = reopenedPrefs()
        assertEquals(AppearancePrefs.ColorMode.Dark.value, reopened.colorMode.getValue())
        assertEquals("Vibrant", reopened.paletteStyle.getValue())
        assertEquals(0xFF34C759.toInt(), reopened.keyColor.getValue())
    }

    @Test
    fun shellOptionsPersist() {
        appearance.enableBlur.setValue(false)
        appearance.floatingBottomBar.setValue(false)
        appearance.floatingBottomBarBlur.setValue(false)
        val reopened = reopenedPrefs()
        assertTrue(!reopened.enableBlur.getValue())
        assertTrue(!reopened.floatingBottomBar.getValue())
        assertTrue(!reopened.floatingBottomBarBlur.getValue())
    }

    @Test
    fun resetRestoresDefaults() {
        appearance.colorMode.setValue(AppearancePrefs.ColorMode.Amoled.value)
        appearance.enableBlur.setValue(false)
        appearance.resetToDefaults()
        assertEquals(
            AppearancePrefs.ColorMode.System.value, appearance.colorMode.getValue()
        )
        assertTrue(appearance.enableBlur.getValue())
    }

    @Test
    fun colourModeParsingIsSafe() {
        assertEquals(
            AppearancePrefs.ColorMode.System, AppearancePrefs.ColorMode.fromValue(-1)
        )
        assertTrue(AppearancePrefs.ColorMode.MonetDark.isMonet)
        assertTrue(AppearancePrefs.ColorMode.Amoled.isDark)
        assertTrue(AppearancePrefs.ColorMode.System.isSystem)
    }

    /**
     * The keyboard look is owned by ThemePrefs; the "界面" page edits these values directly, so they
     * must round-trip without any UI in the loop.
     */
    @Test
    fun keyboardAppearanceRoundTrips() {
        val prefs = ThemeManager.prefs
        val originalRadius = prefs.keyRadius.getValue()
        val originalRipple = prefs.keyRippleEffect.getValue()
        try {
            prefs.keyRadius.setValue(9)
            prefs.keyRippleEffect.setValue(!originalRipple)
            val reopened = ThemeManager.prefs
            assertEquals(9, reopened.keyRadius.getValue())
            assertEquals(!originalRipple, reopened.keyRippleEffect.getValue())
            assertNotEquals(originalRadius, reopened.keyRadius.getValue())
        } finally {
            prefs.keyRadius.setValue(originalRadius)
            prefs.keyRippleEffect.setValue(originalRipple)
        }
    }
}
