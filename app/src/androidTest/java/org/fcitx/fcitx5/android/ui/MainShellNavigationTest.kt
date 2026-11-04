/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.compose.tabTag
import org.fcitx.fcitx5.android.ui.main.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the four-page Compose shell: the bottom bar offers four tabs and every tab renders its
 * page.
 */
@RunWith(AndroidJUnit4::class)
class MainShellNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * Makes the shell deterministic: no first-run setup wizard on top and no notification
     * permission dialog stealing clicks.
     */
    @Before
    fun prepare() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(ctx)
            .edit()
            .putBoolean("setup_wizard_auto_shown", true)
            .putBoolean("need_notifications", false)
            .commit()
    }

    private fun label(res: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(res)

    @Test
    fun bottomBarShowsFourTabs() {
        // The tab labels also appear as page titles, so select them by test tag.
        repeat(4) { index ->
            composeRule.onNodeWithTag(tabTag(index)).assertIsDisplayed()
        }
    }

    @Test
    fun switchingTabsRendersEveryPage() {
        // 界面: the key-appearance section only exists on this page.
        composeRule.onNodeWithTag(tabTag(1)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(label(R.string.ksu_ime_section_key)).assertIsDisplayed()

        // 功能
        composeRule.onNodeWithTag(tabTag(2)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(label(R.string.ksu_features_input)).assertIsDisplayed()

        // 设置
        composeRule.onNodeWithTag(tabTag(3)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(label(R.string.ksu_app_section_theme)).assertIsDisplayed()

        // 主页
        composeRule.onNodeWithTag(tabTag(0)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(label(R.string.ksu_home_status)).assertIsDisplayed()
    }
}
