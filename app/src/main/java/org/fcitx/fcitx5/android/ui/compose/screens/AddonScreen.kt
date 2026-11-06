/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
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
import org.fcitx.fcitx5.android.core.AddonInfo
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.ui.compose.component.ConfirmDialog
import org.fcitx.fcitx5.android.ui.compose.component.CheckboxRow
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 附加组件 list.
 *
 * Each add-on is a checkbox; disabling one that others depend on asks for confirmation and disables
 * the dependents as well, exactly like the old screen. Add-ons that expose a configuration page get
 * a settings button.
 */
@Composable
fun AddonScreen(
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    viewModel: MainViewModel,
    onOpenRoute: (SettingsRoute) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val addons = remember { mutableStateListOf<AddonInfo>() }
    var loaded by remember { mutableStateOf(false) }
    // Pending "disable this add-on" confirmation: the dialog itself has to be emitted from the
    // composable body, not from a click lambda.
    var pendingDisable by remember { mutableStateOf<PendingDisable?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            viewModel.fcitx.runOnReady { addons().sortedBy { it.uniqueName } }
        }.onSuccess {
            addons.clear()
            addons.addAll(it)
            loaded = true
        }
    }

    fun persist() {
        val ids = addons.map { it.uniqueName }.toTypedArray()
        val states = addons.map { it.enabled }.toBooleanArray()
        scope.launch {
            runCatching { viewModel.fcitx.runOnReady { setAddonState(ids, states) } }
        }
    }

    PageScaffold(
        title = stringResource(R.string.addons),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        if (!loaded) {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    NavRow(title = stringResource(R.string.loading), icon = Icons.Rounded.Extension)
                }
            }
            return@PageScaffold
        }
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                addons.forEachIndexed { index, addon ->
                    CheckboxRow(
                        title = addon.displayName,
                        summary = addon.comment.ifBlank { addon.uniqueName },
                        checked = addon.enabled,
                        // our own frontend add-on must stay enabled
                        enabled = addon.uniqueName != "androidfrontend",
                        endActions = {
                            if (addon.isConfigurable) {
                                IconButton(
                                    onClick = {
                                        onOpenRoute(
                                            SettingsRoute.AddonConfig(addon.displayName, addon.uniqueName)
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
                        },
                        onCheckedChange = { checked ->
                            if (checked) {
                                addons[index] = addon.copy(enabled = true)
                                persist()
                            } else {
                                val ask = disableWarning(context, addon, addons.toList(), viewModel)
                                if (ask == null) {
                                    addons[index] = addon.copy(enabled = false)
                                    persist()
                                } else {
                                    pendingDisable = PendingDisable(index, addon, ask)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    pendingDisable?.let { pending ->
        ConfirmDialog(
            title = context.getString(R.string.disable_addon_warn_title),
            message = pending.warning.message,
            onDismiss = { pendingDisable = null },
            onConfirm = {
                pending.warning.alsoDisable.forEach { name ->
                    val i = addons.indexOfFirst { it.uniqueName == name }
                    if (i >= 0) addons[i] = addons[i].copy(enabled = false)
                }
                if (pending.index in addons.indices) {
                    addons[pending.index] = pending.addon.copy(enabled = false)
                }
                pendingDisable = null
                persist()
            },
        )
    }
}

/** A disable action that needs the user's confirmation first. */
private data class DisableWarning(val message: String, val alsoDisable: Set<String>)

private data class PendingDisable(
    val index: Int,
    val addon: AddonInfo,
    val warning: DisableWarning,
)

/**
 * Works out what disabling [addon] would break. Returns null when nothing depends on it, in which
 * case the caller can disable it straight away.
 */
private fun disableWarning(
    context: android.content.Context,
    addon: AddonInfo,
    all: List<AddonInfo>,
    viewModel: MainViewModel,
): DisableWarning? {
    val dependents = runCatching {
        viewModel.fcitx.runImmediately { getAddonReverseDependencies(addon.uniqueName) }
    }.getOrDefault(emptyList())

    fun collect(type: FcitxAPI.AddonDep): List<String> = dependents
        .filter { it.second == type }
        .map { it.first }

    val required = collect(FcitxAPI.AddonDep.Required)
    val optional = collect(FcitxAPI.AddonDep.Optional)
    if (required.isEmpty() && optional.isEmpty()) return null
    val nameOf = { uniqueName: String ->
        all.find { it.uniqueName == uniqueName }?.displayName ?: uniqueName
    }

    val message = buildString {
        appendLine(context.getString(R.string.disable_addon_warn_name, addon.displayName))
        if (required.isNotEmpty()) {
            append("- ")
            appendLine(
                context.getString(
                    R.string.disable_addon_warn_dep,
                    required.joinToString(", ") { nameOf(it) },
                )
            )
        }
        if (optional.isNotEmpty()) {
            append("- ")
            appendLine(
                context.getString(
                    R.string.disable_addon_warn_optdep,
                    optional.joinToString(", ") { nameOf(it) },
                )
            )
        }
        appendLine(context.getString(R.string.disable_addon_warn_confirm))
    }

    return DisableWarning(message, (required + optional).toSet())
}
