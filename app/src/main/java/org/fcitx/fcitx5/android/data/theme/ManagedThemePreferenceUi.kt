/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.theme

import androidx.annotation.StringRes
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceUi

class ManagedThemePreferenceUi(
    @StringRes
    val title: Int,
    key: String,
    val defaultValue: Theme,
    @StringRes
    val summary: Int? = null,
    enableUiOn: (() -> Boolean)? = null
) : ManagedPreferenceUi<Unit>(key, enableUiOn)