/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.pages

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.rime.RimePrefs
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.compose.component.StatusCard
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.ui.main.settings.behavior.ButtonsCustomizerActivity
import org.fcitx.fcitx5.android.utils.InputMethodUtil

/**
 * 主页: a status panel plus the few actions people need day to day. Deep configuration lives on the
 * other three pages.
 */
@Composable
fun HomePage(
    bottomInnerPadding: Dp,
    onOpenRoute: (SettingsRoute) -> Unit,
    onOpenActivity: (Class<*>) -> Unit,
) {
    PageScaffold(
        title = stringResource(R.string.ksu_tab_home),
        bottomInnerPadding = bottomInnerPadding,
    ) {
        homeStatusCard()
        homeStatusSection(onOpenRoute)
        homeQuickActionsSection(onOpenRoute, onOpenActivity)
        homeAboutSection(onOpenRoute)
    }
}

/**
 * The "ready" card from the template: green when the input method is enabled and selected, red
 * otherwise, with the app version underneath. Tapping it opens the system input method settings,
 * which is where either missing step is fixed.
 */
private fun LazyListScope.homeStatusCard() {
    item {
        val context = LocalContext.current
        val imeEnabled = remember(context) {
            runCatching { InputMethodUtil.isEnabled() }.getOrDefault(false)
        }
        val imeSelected = remember(context) {
            runCatching { InputMethodUtil.isSelected() }.getOrDefault(false)
        }
        StatusCard(
            ready = imeEnabled && imeSelected,
            readyTitle = stringResource(R.string.ksu_home_ready_title),
            missingTitle = stringResource(R.string.ksu_home_not_ready_title),
            subtitle = BuildConfig.VERSION_NAME,
            onClick = { InputMethodUtil.startSettingsActivity(context) },
        )
    }
}

private fun LazyListScope.homeStatusSection(onOpenRoute: (SettingsRoute) -> Unit) {
    item {
        val context = LocalContext.current
        val imeEnabled = remember(context) { runCatching { InputMethodUtil.isEnabled() }.getOrDefault(false) }
        val imeSelected = remember(context) { runCatching { InputMethodUtil.isSelected() }.getOrDefault(false) }
        SettingsGroup(
            title = stringResource(R.string.ksu_home_status),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            NavRow(
                title = stringResource(R.string.ksu_home_ime_status),
                summary = stringResource(
                    if (imeEnabled) R.string.ksu_home_ime_enabled else R.string.ksu_home_ime_disabled
                ),
                icon = if (imeEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                onClick = { InputMethodUtil.startSettingsActivity(context) },
            )
            NavRow(
                title = stringResource(R.string.ksu_home_ime_default),
                summary = stringResource(
                    if (imeSelected) R.string.ksu_home_ime_enabled
                    else R.string.ksu_home_ime_not_default
                ),
                icon = Icons.Rounded.Keyboard,
                onClick = { InputMethodUtil.startSettingsActivity(context) },
            )
            NavRow(
                title = stringResource(R.string.ksu_home_keyboard_theme),
                summary = ThemeManager.activeTheme.name,
                icon = Icons.Rounded.Palette,
                onClick = { onOpenRoute(SettingsRoute.Theme) },
            )
            NavRow(
                title = stringResource(R.string.ksu_home_default_scheme),
                summary = RimePrefs.ensureInitialized(context).defaultSchema.getValue(),
                icon = Icons.Rounded.Language,
                onClick = { onOpenRoute(SettingsRoute.RimeSchemas) },
            )
        }
    }
}

private fun LazyListScope.homeQuickActionsSection(
    onOpenRoute: (SettingsRoute) -> Unit,
    onOpenActivity: (Class<*>) -> Unit,
) {
    item {
        val context = LocalContext.current
        SettingsGroup(
            title = stringResource(R.string.ksu_home_quick_actions),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            NavRow(
                title = stringResource(R.string.ksu_home_enable_ime),
                summary = stringResource(R.string.ksu_home_enable_ime_summary),
                icon = Icons.Rounded.Android,
                onClick = { InputMethodUtil.startSettingsActivity(context) },
            )
            NavRow(
                title = stringResource(R.string.ksu_home_pick_ime),
                summary = stringResource(R.string.ksu_home_pick_ime_summary),
                icon = Icons.Rounded.Language,
                onClick = { InputMethodUtil.showPicker() },
            )
            NavRow(
                title = stringResource(R.string.ksu_features_toolbar_customize),
                summary = stringResource(R.string.ksu_features_toolbar_customize_summary),
                icon = Icons.Rounded.Tune,
                onClick = { onOpenActivity(ButtonsCustomizerActivity::class.java) },
            )
            NavRow(
                title = stringResource(R.string.ksu_home_advanced_settings),
                summary = stringResource(R.string.ksu_home_advanced_settings_summary),
                icon = Icons.Rounded.Settings,
                onClick = { onOpenRoute(SettingsRoute.Advanced) },
            )
        }
    }
}

private fun LazyListScope.homeAboutSection(onOpenRoute: (SettingsRoute) -> Unit) {
    item {
        val context = LocalContext.current
        SettingsGroup(
            title = stringResource(R.string.ksu_home_about),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            NavRow(
                title = stringResource(R.string.ksu_home_version),
                summary = BuildConfig.VERSION_NAME,
                icon = Icons.Rounded.Verified,
            )
            NavRow(
                title = stringResource(R.string.ksu_home_package),
                summary = context.packageName,
                icon = Icons.Rounded.Extension,
            )
            NavRow(
                title = stringResource(R.string.about),
                icon = Icons.Rounded.Info,
                onClick = { onOpenRoute(SettingsRoute.About) },
            )
            NavRow(
                title = stringResource(R.string.ksu_home_redeploy_rime),
                summary = stringResource(R.string.ksu_rime_redeploy_summary),
                icon = Icons.Rounded.Sync,
                onClick = { onOpenRoute(SettingsRoute.RimeSchemas) },
            )
        }
    }
}
