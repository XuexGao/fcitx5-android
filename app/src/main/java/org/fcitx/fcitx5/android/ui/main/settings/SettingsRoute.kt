/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.ui.main.settings

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhrase

/**
 * Every settings screen the app can open, as data.
 *
 * Routes are carried through intents (the keyboard's toolbar buttons, deep links) and pushed onto
 * the Compose back stack by the settings host. Nothing here knows about navigation any more: the
 * View-era `createGraph` and its fragment destinations are gone.
 */
@Parcelize
sealed class SettingsRoute : Parcelable {

    /* ========== Fcitx ========== */

    @Serializable
    data object GlobalConfig : SettingsRoute()

    @Serializable
    data object InputMethodList : SettingsRoute()

    @Serializable
    data class InputMethodConfig(val name: String, val uniqueName: String) : SettingsRoute()

    @Serializable
    data object AddonList : SettingsRoute()

    @Serializable
    data class AddonConfig(val name: String, val uniqueName: String) : SettingsRoute()

    /* ========== Android ========== */

    @Serializable
    data object Theme : SettingsRoute()

    @Serializable
    data object IconTheme : SettingsRoute()

    @Serializable
    data object VirtualKeyboard : SettingsRoute()

    @Serializable
    data class KeyboardGroup(val group: Int) : SettingsRoute()

    @Serializable
    data object CandidatesWindow : SettingsRoute()

    @Serializable
    data object Clipboard : SettingsRoute()

    @Serializable
    data object Symbol : SettingsRoute()

    @Serializable
    data object RimeSchemas : SettingsRoute()

    @Serializable
    data object Plugin : SettingsRoute()

    @Serializable
    data object Advanced : SettingsRoute()

    @Serializable
    data object Developer : SettingsRoute()

    @Serializable
    data object License : SettingsRoute()

    @Serializable
    data object About : SettingsRoute()

    /* ========== External ========== */

    @Serializable
    data class PinyinDict(val uri: String? = null) : SettingsRoute() {
        constructor(uri: Uri) : this(uri.toString())
    }

    @Serializable
    data class Punctuation(val title: String, val lang: String? = null) : SettingsRoute()

    @Serializable
    data object QuickPhraseList : SettingsRoute()

    @Serializable
    data class QuickPhraseEdit(val param: Param) : SettingsRoute() {
        constructor(quickPhrase: QuickPhrase) : this(Param(quickPhrase))

        @Serializable
        @Parcelize
        data class Param(val quickPhrase: QuickPhrase) : Parcelable
    }

    @Serializable
    data object TableInputMethods : SettingsRoute()

    @Serializable
    data object PinyinCustomPhrase : SettingsRoute()

    @Serializable
    data class MultiSelect(
        val title: String,
        val addon: String,
        val path: String,
        val option: String,
        val min: Int = 0
    ) : SettingsRoute()

    @Serializable
    data class AddonDirProfileManager(
        val title: String,
        val addon: String,
        val path: String
    ) : SettingsRoute()
}
