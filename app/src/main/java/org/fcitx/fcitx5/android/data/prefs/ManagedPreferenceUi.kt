/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.prefs

import androidx.annotation.StringRes

/**
 * Declarative description of one settings row.
 *
 * This used to also build the corresponding `androidx.preference` view; that path is gone with the
 * View-era screens, so only the metadata the Compose renderer needs is left: the key, the title and
 * summary resources, the value range and the enable-condition.
 */
abstract class ManagedPreferenceUi<T>(
    val key: String,
    private val enableUiOn: (() -> Boolean)? = null
) {

    fun isEnabled() = enableUiOn?.invoke() ?: true

    class Switch(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: Boolean,
        @StringRes
        val summary: Int? = null,
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<Unit>(key, enableUiOn) {
    }

    class StringSet(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: Set<String>,
        @StringRes
        val summary: Int? = null,
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<Unit>(key, enableUiOn) {
    }

    class StringList<T : Any>(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: T,
        val codec: ManagedPreference.StringLikeCodec<T>,
        val entryValues: List<T>,
        @StringRes
        val entryLabels: List<Int>,
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<Unit>(key, enableUiOn) {
    }

    class VoiceInputList(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: String,
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<Unit>(key, enableUiOn) {
    }

    class EditTextInt(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: Int,
        val min: Int,
        val max: Int,
        val unit: String = "",
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<Unit>(key, enableUiOn) {
    }

    class SeekBarInt(
        @StringRes
        val title: Int,
        key: String,
        val defaultValue: Int,
        val min: Int,
        val max: Int,
        val unit: String = "",
        val step: Int = 1,
        @StringRes
        val defaultLabel: Int? = null,
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<Unit>(key, enableUiOn) {
    }

    class TwinSeekBarInt(
        @StringRes
        val title: Int,
        @StringRes
        val label: Int,
        key: String,
        val defaultValue: Int,
        @StringRes
        val secondaryLabel: Int,
        val secondaryKey: String,
        val secondaryDefaultValue: Int,
        val min: Int,
        val max: Int,
        val unit: String = "",
        val step: Int = 1,
        @StringRes
        val defaultLabel: Int? = null,
        enableUiOn: (() -> Boolean)? = null
    ) : ManagedPreferenceUi<Unit>(key, enableUiOn) {
    }
}