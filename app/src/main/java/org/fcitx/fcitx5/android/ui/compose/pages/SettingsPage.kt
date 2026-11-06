/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.pages

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.ui.compose.component.DropdownRow
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.compose.component.SwitchRow
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.utils.InputMethodUtil

/**
 * 设置: settings of the application itself — its own theme, shell options, clipboard sync,
 * diagnostics and about. Nothing here changes how the keyboard looks or behaves; that is 界面/功能.
 */
@Composable
fun SettingsPage(
    bottomInnerPadding: Dp,
    onOpenRoute: (SettingsRoute) -> Unit,
) {
    PageScaffold(
        title = stringResource(R.string.ksu_tab_settings),
        bottomInnerPadding = bottomInnerPadding,
    ) {
        appThemeSection(onOpenRoute)
        shellSection()
        appSection(onOpenRoute)
        aboutSection(onOpenRoute)
    }
}

private fun LazyListScope.appThemeSection(onOpenRoute: (SettingsRoute) -> Unit) {
    item {
        SettingsGroup(
            title = stringResource(R.string.ksu_app_section_theme),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            NavRow(
                title = stringResource(R.string.ksu_app_theme_settings),
                summary = stringResource(R.string.ksu_app_theme_settings_summary),
                icon = Icons.Rounded.Palette,
                onClick = { onOpenRoute(SettingsRoute.ColorPalette) },
            )
        }
    }
}

private fun LazyListScope.shellSection() {
    item {
        val prefs = AppearancePrefs.ensureInitialized(LocalContext.current)
        SettingsGroup(
            title = stringResource(R.string.ksu_app_other),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            DropdownRow(
                title = stringResource(R.string.ksu_app_ui_mode),
                summary = stringResource(R.string.ksu_app_ui_mode_summary),
                icon = Icons.Rounded.Dashboard,
                items = listOf("Miuix", "Material"),
                selectedIndex = if (prefs.uiMode.getValue() == AppearancePrefs.UI_MODE_MATERIAL) 1 else 0,
                onSelectedIndexChange = { index ->
                    prefs.uiMode.setValue(
                        if (index == 1) AppearancePrefs.UI_MODE_MATERIAL
                        else AppearancePrefs.UI_MODE_MIUIX
                    )
                },
            )
            SwitchRow(
                pref = prefs.enableBlur,
                title = stringResource(R.string.ksu_app_enable_blur),
                summary = stringResource(R.string.ksu_app_enable_blur_summary),
                icon = Icons.Rounded.BlurOn,
            )
            SwitchRow(
                pref = prefs.floatingBottomBar,
                title = stringResource(R.string.ksu_app_floating_bar),
                icon = Icons.Rounded.ViewCarousel,
            )
            SwitchRow(
                pref = prefs.floatingBottomBarBlur,
                title = stringResource(R.string.ksu_app_floating_bar_blur),
                enabled = prefs.floatingBottomBar.getValue(),
            )
            SwitchRow(
                pref = prefs.showKeyboardPreview,
                title = stringResource(R.string.ksu_app_show_preview),
                icon = Icons.Rounded.Layers,
            )
        }
    }
}

private fun LazyListScope.appSection(onOpenRoute: (SettingsRoute) -> Unit) {
    item {
        val context = LocalContext.current
        SettingsGroup(
            title = stringResource(R.string.ksu_app_other),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            NavRow(
                title = stringResource(R.string.ksu_clipboard_sync),
                summary = stringResource(R.string.clipboard_sync_settings_summary),
                icon = Icons.Rounded.Memory,
                onClick = {
                    onOpenRoute(SettingsRoute.Clipboard)
                },
            )
            NavRow(
                title = stringResource(R.string.ksu_app_notification),
                summary = stringResource(R.string.notification_permission_title),
                icon = Icons.Rounded.Android,
                onClick = { InputMethodUtil.startSettingsActivity(context) },
            )
            NavRow(
                title = stringResource(R.string.advanced),
                icon = Icons.Rounded.BugReport,
                onClick = { onOpenRoute(SettingsRoute.Advanced) },
            )
        }
    }
}

private fun LazyListScope.aboutSection(onOpenRoute: (SettingsRoute) -> Unit) {
    item {
        val context = LocalContext.current
        SettingsGroup(
            title = stringResource(R.string.ksu_settings_about),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            NavRow(
                title = stringResource(R.string.ksu_settings_faq),
                icon = Icons.Rounded.Info,
                onClick = {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(org.fcitx.fcitx5.android.utils.Const.faqUrl),
                        )
                    )
                },
            )
            NavRow(
                title = stringResource(R.string.developer),
                icon = Icons.Rounded.BugReport,
                onClick = { onOpenRoute(SettingsRoute.Developer) },
            )
            NavRow(
                title = stringResource(R.string.about),
                icon = Icons.Rounded.ContactPage,
                onClick = { onOpenRoute(SettingsRoute.About) },
            )
            NavRow(
                title = stringResource(R.string.license),
                icon = Icons.Rounded.LibraryBooks,
                onClick = { onOpenRoute(SettingsRoute.License) },
            )
        }
    }
}
