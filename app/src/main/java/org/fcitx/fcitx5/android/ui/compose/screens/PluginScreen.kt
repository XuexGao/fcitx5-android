/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.suspendCancellableCoroutine
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.data.DataManager
import org.fcitx.fcitx5.android.core.data.FileSource
import org.fcitx.fcitx5.android.core.data.PluginLoadFailed
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import kotlin.coroutines.resume

/**
 * 插件 list.
 *
 * Mirrors the old screen: waits for the data manager to finish syncing, lists the loaded and failed
 * plugins, offers a reload entry when the detected set no longer matches the synced one, and
 * refreshes when packages are installed or removed while the page is open.
 */
@Composable
fun PluginScreen(bottomInnerPadding: Dp, onBack: () -> Unit) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    var synced by remember { mutableStateOf<DataManager.PluginSet?>(null) }
    var detected by remember { mutableStateOf<DataManager.PluginSet?>(null) }

    LaunchedEffect(revision) {
        whenSynced()
        synced = DataManager.getSyncedPluginSet()
        detected = DataManager.detectPlugins()
    }

    // Refresh when a plugin is installed/removed while this page is visible.
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                revision++
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            },
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    PageScaffold(
        title = stringResource(R.string.plugins),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        val syncedSet = synced
        val detectedSet = detected
        if (syncedSet == null || detectedSet == null) {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    NavRow(title = stringResource(R.string.loading), icon = Icons.Rounded.Extension)
                }
            }
            return@PageScaffold
        }

        if (syncedSet != detectedSet) {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    NavRow(
                        title = stringResource(R.string.plugin_needs_reload),
                        icon = Icons.Rounded.Refresh,
                        onClick = { FcitxDaemon.restartFcitx() },
                    )
                }
            }
        }

        val loaded = syncedSet.loaded.toList().sortedBy { it.name }
        val failed = syncedSet.failed

        if (loaded.isEmpty() && failed.isEmpty()) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.no_plugins),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    NavRow(title = stringResource(R.string.no_plugins), icon = Icons.Rounded.Info)
                }
            }
            return@PageScaffold
        }

        if (loaded.isNotEmpty()) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.plugins_loaded),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    loaded.forEach { plugin ->
                        NavRow(
                            title = plugin.name,
                            summary = "${plugin.versionName}\n${plugin.description}",
                            icon = Icons.Rounded.Extension,
                            onClick = { startPluginAbout(context, plugin.packageName) },
                        )
                    }
                }
            }
        }

        if (failed.isNotEmpty()) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.plugins_failed),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    failed.forEach { (packageName, reason) ->
                        NavRow(
                            title = packageName,
                            summary = failureSummary(context, reason),
                            icon = Icons.Rounded.Info,
                        )
                    }
                }
            }
        }
    }
}

private suspend fun whenSynced() {
    if (DataManager.synced) return
    suspendCancellableCoroutine { continuation ->
        if (DataManager.synced) {
            continuation.resume(Unit)
        } else {
            DataManager.addOnNextSyncedCallback { continuation.resume(Unit) }
        }
    }
}

@Composable
private fun failureSummary(context: Context, reason: PluginLoadFailed): String = when (reason) {
    is PluginLoadFailed.DataDescriptorParseError -> stringResource(R.string.invalid_data_descriptor)
    is PluginLoadFailed.MissingDataDescriptor -> stringResource(R.string.missing_data_descriptor)
    PluginLoadFailed.MissingPluginDescriptor -> stringResource(R.string.missing_plugin_descriptor)
    is PluginLoadFailed.PathConflict -> {
        val owner = when (val src = reason.existingSrc) {
            FileSource.Main -> context.getString(R.string.main_program)
            is FileSource.Plugin -> src.descriptor.name
        }
        context.getString(R.string.path_conflict, reason.path, owner)
    }
    is PluginLoadFailed.PluginAPIIncompatible ->
        context.getString(R.string.incompatible_api, reason.api)
    PluginLoadFailed.PluginDescriptorParseError ->
        stringResource(R.string.invalid_plugin_descriptor)
}

/**
 * Opens the plugin's own About activity, resolved through the plugin manifest intent; falls back to
 * the system app-info page, same as the old screen.
 */
private fun startPluginAbout(context: Context, packageName: String) {
    val pm = context.packageManager
    val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(
            Intent(DataManager.PLUGIN_INTENT),
            android.content.pm.PackageManager.ResolveInfoFlags.of(
                android.content.pm.PackageManager.MATCH_ALL.toLong()
            ),
        )
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(
            Intent(DataManager.PLUGIN_INTENT),
            android.content.pm.PackageManager.MATCH_ALL,
        )
    }
    val target = resolveInfos.firstOrNull { it.activityInfo.packageName == packageName }
    runCatching {
        if (target != null) {
            context.startActivity(
                Intent().apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    component = android.content.ComponentName(
                        target.activityInfo.packageName, target.activityInfo.name
                    )
                }
            )
        } else {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    data = android.net.Uri.fromParts("package", packageName, null)
                }
            )
        }
    }
}
