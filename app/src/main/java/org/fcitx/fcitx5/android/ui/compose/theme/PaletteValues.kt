/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.theme

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

/**
 * Bridges the palette preferences (plain strings, so the data layer stays free of UI libraries) with
 * materialKolor's enums.
 *
 * "Spec2025" is accepted as well: it is what earlier versions of this app stored before the names
 * were aligned with materialKolor's own enum names.
 */
fun parsePaletteStyle(value: String): PaletteStyle =
    runCatching { PaletteStyle.valueOf(value) }.getOrDefault(PaletteStyle.TonalSpot)

fun parseColorSpec(value: String): ColorSpec.SpecVersion = when (value) {
    ColorSpec.SpecVersion.SPEC_2025.name, "Spec2025" -> ColorSpec.SpecVersion.SPEC_2025
    else -> ColorSpec.SpecVersion.Default
}

/** The selectable palette styles and colour specs, as stored in the preferences. */
val paletteStyleNames: List<String> = PaletteStyle.entries.map { it.name }

val colorSpecNames: List<String> = ColorSpec.SpecVersion.entries.map { it.name }
