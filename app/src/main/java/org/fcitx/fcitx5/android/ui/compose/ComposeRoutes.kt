/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose

import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute

/**
 * Which settings routes already have a Compose implementation.
 *
 * Everything else is still served by the legacy navigation graph, which the host activity shows in
 * an overlay. As screens get ported, they are added here and the fragment is deleted.
 */
fun isComposeRoute(route: SettingsRoute): Boolean = when (route) {
    is SettingsRoute.RimeSchemas,
    is SettingsRoute.CandidatesWindow,
    is SettingsRoute.Symbol,
    is SettingsRoute.Clipboard,
    is SettingsRoute.Advanced,
    is SettingsRoute.Developer,
    is SettingsRoute.About,
    is SettingsRoute.GlobalConfig,
    is SettingsRoute.InputMethodConfig,
    is SettingsRoute.AddonConfig,
    is SettingsRoute.Theme,
    is SettingsRoute.Plugin,
    is SettingsRoute.AddonList,
    is SettingsRoute.InputMethodList,
    is SettingsRoute.QuickPhraseList,
    is SettingsRoute.QuickPhraseEdit,
    is SettingsRoute.Punctuation,
    is SettingsRoute.TableInputMethods,
    is SettingsRoute.PinyinDict,
    is SettingsRoute.PinyinCustomPhrase,
    is SettingsRoute.MultiSelect,
    is SettingsRoute.AddonDirProfileManager,
    is SettingsRoute.License,
    is SettingsRoute.VirtualKeyboard,
    is SettingsRoute.KeyboardGroup,
    is SettingsRoute.IconTheme,
    -> true

    else -> false
}
