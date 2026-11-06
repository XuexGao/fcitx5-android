/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.ui.compose.component.CheckboxRow
import org.fcitx.fcitx5.android.ui.compose.component.DialogField
import org.fcitx.fcitx5.android.ui.compose.component.EditFieldsDialog
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.utils.toast
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private data class ProfileEntry(val name: String, val active: Boolean)

private data class ManagerState(
    val current: String,
    val profiles: List<String>,
    val error: String,
)

/**
 * 附加组件目录档案: add-ons that ship several data directories (Rime profiles, for instance) expose
 * a `Current` / `Profiles` pair plus an action protocol (`switch` / `create` / `rename` / `delete`).
 *
 * Actions are sent as a small `RawConfig` through `setAddonSubConfig` and the screen then re-reads
 * the state, so whatever the add-on decided (including its `Error` message) is what the user sees.
 */
@Composable
fun AddonDirProfileScreen(
    title: String,
    addon: String,
    path: String,
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val profiles = remember { mutableStateListOf<ProfileEntry>() }
    var loaded by remember { mutableStateOf(false) }
    var createDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ProfileEntry?>(null) }

    suspend fun loadState(): ManagerState {
        val raw = viewModel.fcitx.runOnReady { getAddonSubConfig(addon, path) }
        val cfg = raw.findByName("cfg") ?: raw
        return ManagerState(
            current = cfg.findByName("Current")?.value.orEmpty().trim(),
            profiles = cfg.findByName("Profiles")?.value.orEmpty()
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .toList(),
            error = cfg.findByName("Error")?.value.orEmpty(),
        )
    }

    suspend fun runAction(action: String, name: String = "", newName: String = "") {
        val items = mutableListOf(RawConfig("Action", action))
        if (name.isNotBlank()) items.add(RawConfig("Name", name))
        if (newName.isNotBlank()) items.add(RawConfig("NewName", newName))
        viewModel.fcitx.runOnReady {
            setAddonSubConfig(addon, path, RawConfig(items.toTypedArray()))
        }
        val state = loadState()
        profiles.clear()
        profiles.addAll(state.profiles.map { ProfileEntry(it, it == state.current) })
        if (state.error.isNotBlank()) context.toast(state.error)
    }

    fun launchAction(action: String, name: String = "", newName: String = "") {
        FcitxApplication.getInstance().coroutineScope.launch {
            runCatching { runAction(action, name, newName) }
                .onFailure { Timber.w(it, "addon dir profile action failed") }
        }
    }

    LaunchedEffect(addon, path) {
        runCatching { loadState() }
            .onSuccess { state ->
                profiles.clear()
                profiles.addAll(state.profiles.map { ProfileEntry(it, it == state.current) })
                if (state.error.isNotBlank()) context.toast(state.error)
            }
            .onFailure { Timber.w(it, "Unable to load addon dir profiles") }
        loaded = true
    }

    PageScaffold(
        title = title,
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        if (!loaded) {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    NavRow(title = stringResource(R.string.loading), icon = Icons.Rounded.Folder)
                }
            }
            return@PageScaffold
        }
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                profiles.forEach { entry ->
                    CheckboxRow(
                        title = if (entry.active) {
                            stringResource(R.string.addon_dir_profile_current_format, entry.name)
                        } else {
                            entry.name
                        },
                        checked = entry.active,
                        onCheckedChange = { checked ->
                            // The checkbox only ever turns on: switching away from a profile means
                            // switching to another one, which happens on that other row.
                            if (checked && !entry.active) launchAction("switch", entry.name)
                        },
                        endActions = {
                            IconButton(onClick = { renameTarget = entry }) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = stringResource(
                                        R.string.addon_dir_profile_rename
                                    ),
                                    tint = colorScheme.onSurfaceVariantSummary,
                                )
                            }
                            IconButton(
                                enabled = profiles.size > 1,
                                onClick = {
                                    if (profiles.size <= 1) {
                                        context.toast(R.string.addon_dir_profile_delete_last_forbidden)
                                        return@IconButton
                                    }
                                    androidx.appcompat.app.AlertDialog.Builder(context)
                                        .setTitle(R.string.delete)
                                        .setMessage(
                                            context.getString(
                                                R.string.addon_dir_profile_delete_confirm,
                                                entry.name,
                                            )
                                        )
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .setPositiveButton(android.R.string.ok) { _, _ ->
                                            launchAction("delete", entry.name)
                                        }
                                        .show()
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
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                NavRow(
                    title = stringResource(R.string.addon_dir_profile_create),
                    icon = Icons.Rounded.Add,
                    onClick = { createDialog = true },
                )
            }
        }
    }

    if (createDialog) {
        EditFieldsDialog(
            title = stringResource(R.string.addon_dir_profile_create),
            fields = listOf(DialogField(label = stringResource(R.string.name))),
            onDismiss = { createDialog = false },
            onConfirm = { values ->
                createDialog = false
                launchAction("create", values[0].trim())
            },
        )
    }

    val renaming = renameTarget
    if (renaming != null) {
        EditFieldsDialog(
            title = stringResource(R.string.addon_dir_profile_rename),
            fields = listOf(DialogField(label = stringResource(R.string.name), initial = renaming.name)),
            onDismiss = { renameTarget = null },
            onConfirm = { values ->
                renameTarget = null
                val newName = values[0].trim()
                if (newName.isNotEmpty() && newName != renaming.name) {
                    launchAction("rename", renaming.name, newName)
                }
            },
        )
    }
}
