/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.pages

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
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
        homeVersionSection()
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
/** 版本信息: what this build is — nothing here navigates anywhere. */
private fun LazyListScope.homeVersionSection() {
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
        }
    }
}
