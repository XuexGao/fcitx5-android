/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import org.fcitx.fcitx5.android.ui.compose.LocalUiMode
import org.fcitx.fcitx5.android.ui.compose.UiMode
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.screens.ProviderRows
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 键盘主题: pick the keyboard skin, with a live colour swatch per theme, plus the theme's own
 * extra settings below (day/night following, light/dark theme choice).
 *
 * Applying a theme goes through [ThemeManager.setNormalModeTheme], the same entry point the old
 * screen used, so the keyboard and its preview update instantly.
 */
@Composable
fun ThemeScreen(bottomInnerPadding: Dp, onBack: () -> Unit) {
    var revision by remember { mutableIntStateOf(0) }
    val themes = remember(revision) { ThemeManager.getAllThemes() }
    val activeTheme = ThemeManager.activeTheme

    PageScaffold(
        title = stringResource(R.string.theme),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                themes.forEach { theme ->
                    ThemeRow(
                        theme = theme,
                        selected = theme.name == activeTheme.name,
                        onSelect = {
                            ThemeManager.setNormalModeTheme(theme)
                            revision++
                        },
                    )
                }
            }
        }
        item {
            SettingsGroup(
                title = stringResource(R.string.configure),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                ProviderRows(ThemeManager.prefs)
            }
        }
    }
}

@Composable
private fun ThemeRow(theme: Theme, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ThemeSwatch(theme)
        Column(modifier = Modifier.weight(1f)) {
            when (LocalUiMode.current) {
                UiMode.Miuix -> {
                    Text(
                        text = theme.name,
                        style = MiuixTheme.textStyles.body1,
                        color = colorScheme.onSurface,
                    )
                    Text(
                        text = if (theme.isDark) stringResource(R.string.theme_dark)
                        else stringResource(R.string.theme_light),
                        style = MiuixTheme.textStyles.footnote1,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }

                UiMode.Material -> {
                    MaterialText(text = theme.name, style = MaterialTheme.typography.bodyLarge)
                    MaterialText(
                        text = if (theme.isDark) stringResource(R.string.theme_dark)
                        else stringResource(R.string.theme_light),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
        val (trailingIcon, trailingTint) = when (LocalUiMode.current) {
            UiMode.Miuix -> if (selected) Icons.Rounded.CheckCircle to colorScheme.primary
            else Icons.Rounded.Palette to colorScheme.onSurfaceVariantSummary

            UiMode.Material -> if (selected) {
                Icons.Rounded.CheckCircle to MaterialTheme.colorScheme.primary
            } else {
                Icons.Rounded.Palette to MaterialTheme.colorScheme.outline
            }
        }
        when (LocalUiMode.current) {
            UiMode.Miuix -> top.yukonga.miuix.kmp.basic.Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = trailingTint,
            )

            UiMode.Material -> androidx.compose.material3.Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = trailingTint,
            )
        }
    }
}

/** A miniature of the keyboard skin: surface, key background and accent key. */
@Composable
private fun ThemeSwatch(theme: Theme) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(theme.keyboardColor))
            .border(1.dp, colorScheme.outline, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(theme.keyBackgroundColor))
            )
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(theme.accentKeyBackgroundColor))
            )
        }
    }
}
