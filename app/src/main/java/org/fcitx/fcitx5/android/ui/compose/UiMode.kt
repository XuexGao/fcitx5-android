/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 *
 * Ported from KernelSU-Style-UI-Kit (Apache-2.0), which is the reference implementation this
 * settings app follows. Each screen has a Miuix and a Material variant, selected by this mode.
 */
package org.fcitx.fcitx5.android.ui.compose

import androidx.compose.runtime.staticCompositionLocalOf

/** Which component style the settings UI is drawn with. */
enum class UiMode(val value: String) {
    Miuix("miuix"),
    Material("material");

    companion object {
        fun fromValue(value: String): UiMode = when (value) {
            Material.value -> Material
            else -> Miuix
        }

        const val DEFAULT_VALUE = "miuix"
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Miuix }
