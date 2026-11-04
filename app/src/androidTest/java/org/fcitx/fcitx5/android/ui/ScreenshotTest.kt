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
import androidx.compose.ui.test.performClick
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

    private val outputDir: File
        get() = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }

    private var index = 0

    private fun capture(name: String) {
        composeRule.waitForIdle()
        Thread.sleep(350)
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val file = File(outputDir, "%02d-%s.png".format(index++, name))
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
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
        composeRule.onNodeWithText(
            context.getString(R.string.ksu_rime_title)
        ).performClick()
        composeRule.waitForIdle()
        Thread.sleep(500)
        capture("rime-scheme")
    }
}
