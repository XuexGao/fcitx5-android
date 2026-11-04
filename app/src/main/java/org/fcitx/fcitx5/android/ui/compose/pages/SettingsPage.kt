/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.ui.compose.component.ChoiceRow
import org.fcitx.fcitx5.android.ui.compose.component.IndexChoiceRow
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.compose.component.SwitchRow
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.utils.InputMethodUtil
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

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
        appThemeSection()
        shellSection()
        appSection(onOpenRoute)
        aboutSection(onOpenRoute)
    }
}

private fun LazyListScope.appThemeSection() {
    item {
        val prefs = AppearancePrefs.ensureInitialized(LocalContext.current)
        val colorModes = listOf(
            stringResource(R.string.ksu_app_color_mode_system),
            stringResource(R.string.ksu_app_color_mode_light),
            stringResource(R.string.ksu_app_color_mode_dark),
            stringResource(R.string.ksu_app_color_mode_monet_system),
            stringResource(R.string.ksu_app_color_mode_monet_light),
            stringResource(R.string.ksu_app_color_mode_monet_dark),
            stringResource(R.string.ksu_app_color_mode_amoled),
        )
        SettingsGroup(
            title = stringResource(R.string.ksu_app_section_theme),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            IndexChoiceRow(
                pref = prefs.colorMode,
                title = stringResource(R.string.ksu_app_color_mode),
                values = colorModes,
                icon = Icons.Rounded.Dashboard,
            )
            NavRow(
                title = stringResource(R.string.ksu_app_key_color),
                summary = prefs.keyColor.getValue().let { value ->
                    if (value == 0) stringResource(R.string.ksu_app_key_color_default)
                    else "#%06X".format(value and 0xFFFFFF)
                },
                icon = Icons.Rounded.ColorLens,
            )
            ColorSwatchRow(pref = prefs.keyColor)
            ChoiceRow(
                pref = prefs.paletteStyle,
                title = stringResource(R.string.ksu_app_palette_style),
                values = AppearancePrefs.PALETTE_STYLES,
                icon = Icons.Rounded.Palette,
            )
            ChoiceRow(
                pref = prefs.colorSpec,
                title = stringResource(R.string.ksu_app_color_spec),
                values = listOf("Default", "Spec2025"),
                labels = listOf("Default (2021)", "Spec 2025"),
                icon = Icons.Rounded.Translate,
            )
        }
    }
}

/** Seed-colour picker: a row of tappable swatches, the first one meaning "automatic". */
@Composable
private fun ColorSwatchRow(pref: org.fcitx.fcitx5.android.data.prefs.ManagedPreference.PInt) {
    val state = org.fcitx.fcitx5.android.ui.compose.component.rememberPref(pref)
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppearancePrefs.KEY_COLOR_PRESETS.forEach { value ->
            val selected = state.value == value
            val color = if (value == 0) colorScheme.surfaceContainerHighest else Color(value)
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) colorScheme.primary else colorScheme.outline,
                        shape = CircleShape,
                    )
                    .clickable { state.value = value },
                contentAlignment = Alignment.Center,
            ) {
                if (value == 0) {
                    top.yukonga.miuix.kmp.basic.Text(
                        text = "A",
                        color = colorScheme.onSurfaceContainerHighest,
                        style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.footnote1,
                    )
                }
            }
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
