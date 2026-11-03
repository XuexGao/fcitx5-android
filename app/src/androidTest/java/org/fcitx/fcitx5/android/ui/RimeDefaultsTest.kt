/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.fcitx.fcitx5.android.data.rime.RimeManager
import org.fcitx.fcitx5.android.data.rime.RimePrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Verifies the 万象拼音 integration defaults and that the generated custom config really contains
 * what the preferences promise.
 */
@RunWith(AndroidJUnit4::class)
class RimeDefaultsTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs get() = RimePrefs.ensureInitialized(context)

    @Test
    fun defaultSchemaIsWanxiang() {
        assertEquals("wanxiang", RimePrefs.DEFAULT_SCHEMA)
        assertEquals("wanxiang", prefs.defaultSchema.getValue())
        assertTrue(prefs.enabledSchemas.getValue().contains("wanxiang"))
    }

    @Test
    fun defaultDoublePinyinIsNaturalCode() {
        assertEquals(
            RimePrefs.DoublePinyin.自然码, RimePrefs.DoublePinyin.fromId(prefs.doublePinyin.getValue())
        )
    }

    @Test
    fun generatedConfigEnablesWanxiangAndDoublePinyin() {
        RimeManager.applyConfiguration(context, force = true)
        val userDir = RimeManager.userDir
        val defaultCustom = File(userDir, "default.custom.yaml")
        val wanxiangCustom = File(userDir, "wanxiang.custom.yaml")
        assertTrue("default.custom.yaml must be written", defaultCustom.isFile)
        assertTrue("wanxiang.custom.yaml must be written", wanxiangCustom.isFile)

        val defaultText = defaultCustom.readText()
        assertTrue(defaultText.contains("- schema: wanxiang"))
        // 万象拼音 must be the first entry so Rime starts on it.
        assertTrue(
            "wanxiang should lead the schema list",
            defaultText.indexOf("- schema: wanxiang") < defaultText.indexOf("- schema: luna_pinyin")
        )

        val wanxiangText = wanxiangCustom.readText()
        assertTrue(wanxiangText.contains("wanxiang_algebra:/base/自然码"))
        assertTrue(wanxiangText.contains("wanxiang_algebra:/26jian"))
    }

    @Test
    fun resetRewritesDefaults() {
        prefs.defaultSchema.setValue("luna_pinyin_simp")
        prefs.doublePinyin.setValue(RimePrefs.DoublePinyin.小鹤双拼.id)
        RimeManager.resetToDefaults(context)
        assertEquals("wanxiang", prefs.defaultSchema.getValue())
        assertEquals(
            RimePrefs.DoublePinyin.自然码.id, prefs.doublePinyin.getValue()
        )
        val text = File(RimeManager.userDir, "wanxiang.custom.yaml").readText()
        assertTrue(text.contains("wanxiang_algebra:/base/自然码"))
    }

    @Test
    fun availableSchemasAreDiscoverable() {
        // The rime plugin may not be installed in the test environment, but the API must not throw
        // and must always include whatever the user enabled.
        val schemas = RimeManager.availableSchemas(context)
        assertTrue(schemas.any { it.id == "wanxiang" })
    }
}
