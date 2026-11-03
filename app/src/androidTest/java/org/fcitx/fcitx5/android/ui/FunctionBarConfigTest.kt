/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.fcitx.fcitx5.android.input.config.ButtonIconFile
import org.fcitx.fcitx5.android.input.config.ButtonsLayoutConfig
import org.fcitx.fcitx5.android.input.config.ConfigProviders
import org.fcitx.fcitx5.android.input.config.ConfigurableButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Function bar customisation: defaults exist, edits round-trip through the config file, and the
 * stored order/visibility is what the bar reads back.
 */
@RunWith(AndroidJUnit4::class)
class FunctionBarConfigTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun write(config: ButtonsLayoutConfig) {
        val file = ConfigProviders.provider.buttonsLayoutConfigFile()
        assertNotNull("buttons layout config file must be resolvable", file)
        file!!.parentFile?.mkdirs()
        file.writeText(json.encodeToString(config) + "\n")
    }

    private fun read(): ButtonsLayoutConfig? =
        ConfigProviders.readButtonsLayoutConfig<ButtonsLayoutConfig>()?.value

    @Test
    fun defaultLayoutHasButtonsForBothBars() {
        val config = ButtonsLayoutConfig.default()
        assertTrue(config.kawaiiBarButtons.isNotEmpty())
        assertTrue(config.statusAreaButtons.isNotEmpty())
    }

    @Test
    fun defaultsSurviveSerialisation() {
        val config = ButtonsLayoutConfig.default()
        val decoded = json.decodeFromString<ButtonsLayoutConfig>(json.encodeToString(config))
        assertEquals(config.kawaiiBarButtons.map { it.id }, decoded.kawaiiBarButtons.map { it.id })
        assertEquals(config.statusAreaButtons.map { it.id }, decoded.statusAreaButtons.map { it.id })
    }

    @Test
    fun customOrderIsPersisted() {
        val original = read() ?: ButtonsLayoutConfig.default()
        try {
            val reversed = original.kawaiiBarButtons.reversed()
            write(original.copy(kawaiiBarButtons = reversed))
            assertEquals(reversed.map { it.id }, read()?.kawaiiBarButtons?.map { it.id })
        } finally {
            write(original)
        }
    }

    @Test
    fun hidingAButtonIsPersisted() {
        val original = read() ?: ButtonsLayoutConfig.default()
        if (original.kawaiiBarButtons.isEmpty()) return
        try {
            // Removing an entry from the list is how the UI hides a button.
            write(original.copy(kawaiiBarButtons = original.kawaiiBarButtons.drop(1)))
            assertEquals(original.kawaiiBarButtons.size - 1, read()?.kawaiiBarButtons?.size)
        } finally {
            write(original)
        }
    }

    @Test
    fun restoreDefaultIsIdempotent() {
        val original = read() ?: ButtonsLayoutConfig.default()
        try {
            write(ButtonsLayoutConfig.default())
            val first = read()
            write(ButtonsLayoutConfig.default())
            assertEquals(first?.kawaiiBarButtons?.map { it.id }, read()?.kawaiiBarButtons?.map { it.id })
        } finally {
            write(original)
        }
    }

    @Test
    fun iconFileReferencesAreRecognised() {
        assertTrue(ButtonIconFile.isFileIcon("file:button_icons/custom.png"))
        assertTrue(!ButtonIconFile.isFileIcon("ic_clipboard"))
        assertTrue(!ButtonIconFile.isFileIcon(null))
        assertNotNull(ConfigurableButton(id = "clipboard"))
    }
}
