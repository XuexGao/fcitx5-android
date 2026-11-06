/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.core.SubtypeManager
import org.fcitx.fcitx5.android.ui.compose.component.PickerDialog
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 输入法 list: the enabled input methods, in order, plus the ones that can still be added.
 *
 * Reordering uses explicit up/down buttons rather than drag-and-drop: the ordering semantics (write
 * the enabled list back to fcitx) are identical and it works with one hand, which matters on an IME
 * settings screen. Add/remove and the per-entry configuration entry are unchanged.
 */
@Composable
fun InputMethodListScreen(
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    viewModel: MainViewModel,
    onOpenRoute: (SettingsRoute) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enabled = remember { mutableStateListOf<InputMethodEntry>() }
    val available = remember { mutableStateListOf<InputMethodEntry>() }
    var loaded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            viewModel.fcitx.runOnReady {
                availableIme().toSet() to enabledIme().toList()
            }
        }.onSuccess { (all, active) ->
            available.clear()
            available.addAll(all)
            enabled.clear()
            enabled.addAll(active)
            loaded = true
        }
    }

    fun persist() {
        val names = enabled.map { it.uniqueName }.toTypedArray()
        scope.launch {
            runCatching {
                viewModel.fcitx.runOnReady {
                    setEnabledIme(names)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        SubtypeManager.syncWith(enabledIme())
                    }
                }
            }
        }
    }

    PageScaffold(
        title = stringResource(R.string.input_methods),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        if (!loaded) {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    NavRow(title = stringResource(R.string.loading), icon = Icons.Rounded.Language)
                }
            }
            return@PageScaffold
        }

        item {
            SettingsGroup(
                title = stringResource(R.string.plugins_loaded),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                enabled.forEachIndexed { index, entry ->
                    NavRow(
                        title = entry.displayName,
                        summary = entry.uniqueName,
                        endActions = {
                            IconButton(
                                enabled = index > 0,
                                onClick = {
                                    val moved = enabled.removeAt(index)
                                    enabled.add(index - 1, moved)
                                    persist()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariantSummary,
                                )
                            }
                            IconButton(
                                enabled = index < enabled.lastIndex,
                                onClick = {
                                    val moved = enabled.removeAt(index)
                                    enabled.add(index + 1, moved)
                                    persist()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariantSummary,
                                )
                            }
                            if (entry.isConfigurable) {
                                IconButton(
                                    onClick = {
                                        onOpenRoute(
                                            SettingsRoute.InputMethodConfig(
                                                entry.displayName, entry.uniqueName
                                            )
                                        )
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = stringResource(R.string.configure),
                                        tint = colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    enabled.removeAt(index)
                                    persist()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        },
                    )
                }
            }
        }

        val addable = available.filterNot { candidate ->
            enabled.any { it.uniqueName == candidate.uniqueName }
        }
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                NavRow(
                    title = stringResource(R.string.add),
                    icon = Icons.Rounded.Add,
                    enabled = addable.isNotEmpty(),
                    onClick = { showAddDialog = true },
                )
            }
        }
    }

    if (showAddDialog) {
        val addable = available.filterNot { candidate ->
            enabled.any { it.uniqueName == candidate.uniqueName }
        }
        PickerDialog(
            title = context.getString(R.string.input_methods),
            items = addable.map { it.displayName },
            onDismiss = { showAddDialog = false },
            onSelected = { which ->
                enabled.add(addable[which])
                persist()
                showAddDialog = false
            },
        )
    }
}
