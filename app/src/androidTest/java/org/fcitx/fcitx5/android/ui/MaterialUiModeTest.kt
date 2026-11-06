/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.ui.compose.tabTag
import org.fcitx.fcitx5.android.ui.main.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.junit.runner.RunWith

/**
 * The Material 3 style is a second implementation of every page, so it needs its own end-to-end
 * check: the app must start, all four pages must render and the bar must still switch pages.
 *
 * The style is written before the activity is created, so the whole tree composes in Material mode.
 */
@RunWith(AndroidJUnit4::class)
class MaterialUiModeTest {

    @get:Rule(order = 0)
    val useMaterialStyle: TestRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                val ctx = InstrumentationRegistry.getInstrumentation().targetContext
                val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
                prefs.edit()
                    .putBoolean("setup_wizard_auto_shown", true)
                    .putBoolean("need_notifications", false)
                    .putString(AppearancePrefs.KEY_UI_MODE, AppearancePrefs.UI_MODE_MATERIAL)
                    .commit()
                try {
                    base.evaluate()
                } finally {
                    prefs.edit()
                        .putString(AppearancePrefs.KEY_UI_MODE, AppearancePrefs.UI_MODE_MIUIX)
                        .commit()
                }
            }
        }
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun materialStyleRendersEveryPage() {
        // Home also carries the new "ready" status card; which of its two titles shows depends on
        // the state of the emulator, so accept either.
        val ready = context.getString(R.string.ksu_home_ready_title)
        val missing = context.getString(R.string.ksu_home_not_ready_title)
        val statusTitles = composeRule.onAllNodes(hasText(ready)).fetchSemanticsNodes().size +
            composeRule.onAllNodes(hasText(missing)).fetchSemanticsNodes().size
        assertTrue("the home status card is missing", statusTitles > 0)
        listOf(
            1 to R.string.ksu_ime_section_key,
            2 to R.string.ksu_features_input,
            3 to R.string.ksu_app_section_theme,
        ).forEach { (index, marker) ->
            composeRule.onNodeWithTag(tabTag(index)).performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText(context.getString(marker)).assertExists()
        }
        composeRule.onNodeWithTag(tabTag(0)).performClick()
        composeRule.waitForIdle()
    }
}
