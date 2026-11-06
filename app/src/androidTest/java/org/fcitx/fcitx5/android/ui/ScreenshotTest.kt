/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.compose.tabTag
import org.fcitx.fcitx5.android.ui.main.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.junit.runner.RunWith
import java.io.File

/**
 * Walks every page and detail screen and writes a PNG per step to the app's external files dir.
 *
 * The emulator workflow pulls that directory and uploads it, which is how the visual details of the
 * redesign (spacing, corner radii, weights, blur) are checked against the reference without a
 * device in hand. Not an assertion test: it fails only if a screen cannot be reached.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule(order = 0)
    val quietStartup: TestRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                val ctx = InstrumentationRegistry.getInstrumentation().targetContext
                PreferenceManager.getDefaultSharedPreferences(ctx)
                    .edit()
                    .putBoolean("setup_wizard_auto_shown", true)
                    .putBoolean("need_notifications", false)
                    .commit()
                base.evaluate()
            }
        }
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Internal storage: `adb pull` cannot read app-specific external storage on API 30+, while
     * `run-as <pkg> tar` can read the internal dir of a debuggable build.
     */
    private val outputDir: File
        get() = File(context.filesDir, "screenshots").apply { mkdirs() }

    private var index = 0

    private fun capture(name: String) {
        composeRule.waitForIdle()
        Thread.sleep(350)
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val file = File(outputDir, "%02d-%s.png".format(index++, name))
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /**
     * Clicks a row, scrolling the enclosing list to it first: rows can sit under the floating bottom
     * bar or below the fold, where touch injection would fail.
     */
    private fun clickRow(text: String) {
        runCatching {
            composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text).performClick()
    }

    /** Pops the current detail screen through the activity's back dispatcher. */
    private fun back() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun tab(index: Int, name: String, assertText: Int) {
        composeRule.onNodeWithTag(tabTag(index)).performClick()
        composeRule.waitForIdle()
        Thread.sleep(400)
        capture(name)
        composeRule.onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(assertText)
        ).assertExists()
    }

    @Test
    fun captureAllScreens() {
        // Four pages
        capture("home")
        tab(1, "appearance", R.string.ksu_ime_section_key)
        tab(2, "features", R.string.ksu_features_input)
        tab(3, "settings", R.string.ksu_app_section_theme)
        composeRule.onNodeWithTag(tabTag(0)).performClick()
        composeRule.waitForIdle()

        // Detail screens that are already Compose
        composeRule.onNodeWithTag(tabTag(2)).performClick()
        composeRule.waitForIdle()
        clickRow(context.getString(R.string.ksu_rime_title))
        composeRule.waitForIdle()
        Thread.sleep(600)
        capture("rime-scheme")
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        // 界面 → 键盘主题
        composeRule.onNodeWithTag(tabTag(1)).performClick()
        composeRule.waitForIdle()
        clickRow(context.getString(R.string.theme))
        composeRule.waitForIdle()
        Thread.sleep(600)
        capture("theme-list")
        back()

        // 界面 → 图标主题
        clickRow(context.getString(R.string.icon_theme))
        composeRule.waitForIdle()
        Thread.sleep(600)
        capture("icon-theme")
        back()

        // 功能 → 全局选项（Fcitx 动态配置）
        composeRule.onNodeWithTag(tabTag(2)).performClick()
        composeRule.waitForIdle()
        clickRow(context.getString(R.string.global_options))
        composeRule.waitForIdle()
        Thread.sleep(1500)
        capture("global-config")
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        // 功能 → 候选窗口
        clickRow(context.getString(R.string.candidates_window))
        composeRule.waitForIdle()
        Thread.sleep(500)
        capture("candidates")
        back()

        // 功能 → 快捷短语（列表 + 编辑）
        clickRow(context.getString(R.string.quickphrase_editor))
        composeRule.waitForIdle()
        Thread.sleep(600)
        capture("quickphrase-list")
        back()

        // 功能 → 码表输入法
        clickRow(context.getString(R.string.table_im))
        composeRule.waitForIdle()
        Thread.sleep(600)
        capture("table-input-methods")
        back()

        // 功能 → 拼音自定义短语
        clickRow(context.getString(R.string.ksu_pinyin_custom_phrase))
        composeRule.waitForIdle()
        Thread.sleep(600)
        capture("pinyin-custom-phrase")
        back()

        // 设置 → 虚拟键盘（分组列表）
        composeRule.onNodeWithTag(tabTag(3)).performClick()
        composeRule.waitForIdle()
        clickRow(context.getString(R.string.virtual_keyboard))
        composeRule.waitForIdle()
        Thread.sleep(600)
        capture("virtual-keyboard")
        back()

        // 设置 → 许可证
        clickRow(context.getString(R.string.license))
        composeRule.waitForIdle()
        Thread.sleep(600)
        capture("licenses")
    }
}
