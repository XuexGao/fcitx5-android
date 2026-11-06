/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose

import androidx.compose.runtime.mutableStateOf
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute

/**
 * Hand-off point for routes opened from outside the Compose tree.
 *
 * Deep links, the keyboard's toolbar buttons and other activities all launch the settings activity
 * with a [SettingsRoute] extra. Since the detail screens now live in a Compose back stack owned by
 * [FcitxApp], the activity cannot navigate them itself — it drops the route here and the Compose
 * host picks it up, deciding whether to push it or hand it to the legacy overlay.
 */
class ComposeRouteHost {

    internal val pending = mutableStateOf<SettingsRoute?>(null)

    fun open(route: SettingsRoute) {
        pending.value = route
    }
}
