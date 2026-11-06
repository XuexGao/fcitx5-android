/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.clipboardsync.ui.ClipboardSyncSettingsActivity
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.data.UserDataManager
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceProvider
import org.fcitx.fcitx5.android.ui.compose.component.ConfirmDialog
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.compose.component.SwitchRow
import org.fcitx.fcitx5.android.ui.main.LogActivity
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.utils.AppUtil
import org.fcitx.fcitx5.android.utils.buildDocumentsProviderIntent
import org.fcitx.fcitx5.android.utils.buildPrimaryStorageIntent
import org.fcitx.fcitx5.android.utils.formatDateTime
import org.fcitx.fcitx5.android.utils.importErrorDialog
import org.fcitx.fcitx5.android.utils.iso8601UTCDateTime
import org.fcitx.fcitx5.android.utils.startActivity
import org.fcitx.fcitx5.android.utils.toast

/** Provider-driven screen: rows come from the provider, optional extra rows are appended. */
@Composable
fun ProviderScreen(
    title: String,
    provider: ManagedPreferenceProvider,
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    footer: (LazyListScope.() -> Unit)? = null,
) {
    ManagedPrefsScreen(
        title = title,
        provider = provider,
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
        footer = footer,
    )
}

/** 候选窗口 */
@Composable
fun CandidatesScreen(bottomInnerPadding: Dp, onBack: () -> Unit) {
    ProviderScreen(
        title = stringResource(R.string.candidates_window),
        provider = AppPrefs.getInstance().candidates,
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    )
}

/** 表情与符号 */
@Composable
fun SymbolScreen(bottomInnerPadding: Dp, onBack: () -> Unit) {
    ProviderScreen(
        title = stringResource(R.string.emoji_and_symbols),
        provider = AppPrefs.getInstance().symbols,
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    )
}

/** 剪贴板：偏好行 + 剪贴板同步入口 */
@Composable
fun ClipboardScreen(bottomInnerPadding: Dp, onBack: () -> Unit) {
    val context = LocalContext.current
    ProviderScreen(
        title = stringResource(R.string.clipboard),
        provider = AppPrefs.getInstance().clipboard,
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
        footer = {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    NavRow(
                        title = stringResource(R.string.clipboard_sync_settings),
                        summary = stringResource(R.string.clipboard_sync_settings_summary),
                        icon = Icons.Rounded.Memory,
                        onClick = {
                            context.startActivity(
                                Intent(context, ClipboardSyncSettingsActivity::class.java)
                            )
                        },
                    )
                }
            }
        },
    )
}

/**
 * 高级：偏好行 + 用户数据目录 / 导出 / 导入。
 *
 * The export and import flows are the same ones the old screen used (SAF + [UserDataManager]), so
 * the resulting zip is byte-compatible.
 */
@Composable
fun AdvancedScreen(
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportTimestamp = remember { System.currentTimeMillis() }
    var showImportConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        UserDataManager.export(out, exportTimestamp).getOrThrow()
                    }
                }
            }.onFailure { context.toast(it.localizedMessage ?: it.toString()) }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = context.contentResolver.let { cr ->
                cr.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                }
            }
            if (name == null || !name.endsWith(".zip")) {
                context.importErrorDialog(R.string.exception_user_data_filename, name ?: "")
                return@launch
            }
            try {
                FcitxDaemon.stopFcitx()
                val metadata = withContext(Dispatchers.IO) {
                    UserDataManager.import(context.contentResolver.openInputStream(uri)!!)
                        .getOrThrow()
                }
                AppUtil.showRestartNotification(context)
                context.toast(
                    context.getString(R.string.user_data_imported, formatDateTime(metadata.exportTime))
                )
                delay(400L)
                AppUtil.exit()
            } catch (e: Exception) {
                FcitxDaemon.startFcitx()
                context.importErrorDialog(e)
            }
        }
    }

    if (showImportConfirm) {
        ConfirmDialog(
            title = context.getString(R.string.import_user_data),
            message = context.getString(R.string.confirm_import_user_data),
            onDismiss = { showImportConfirm = false },
            onConfirm = {
                showImportConfirm = false
                importLauncher.launch("application/zip")
            },
        )
    }

    ProviderScreen(
        title = stringResource(R.string.advanced),
        provider = AppPrefs.getInstance().advanced,
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
        footer = {
            item {
                SettingsGroup(
                    title = stringResource(R.string.export_user_data),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    NavRow(
                        title = stringResource(R.string.browse_user_data_dir),
                        icon = Icons.Rounded.Folder,
                        onClick = {
                            runCatching { context.startActivity(buildDocumentsProviderIntent()) }
                                .onFailure { context.toast(it) }
                        },
                    )
                    NavRow(
                        title = stringResource(R.string.export_user_data),
                        icon = Icons.Rounded.Save,
                        onClick = {
                            scope.launch {
                                viewModel.fcitx.runIfReady { save() }
                                exportTimestamp = System.currentTimeMillis()
                                exportLauncher.launch(
                                    "fcitx5-android_${iso8601UTCDateTime(exportTimestamp)}.zip"
                                )
                            }
                        },
                    )
                    NavRow(
                        title = stringResource(R.string.import_user_data),
                        icon = Icons.Rounded.Upload,
                        onClick = {
                            showImportConfirm = true
                        },
                    )
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        NavRow(
                            title = stringResource(R.string.browse_user_data_dir) + " (primary)",
                            icon = Icons.Rounded.Folder,
                            onClick = {
                                runCatching { context.startActivity(buildPrimaryStorageIntent()) }
                                    .onFailure { context.toast(it) }
                            },
                        )
                    }
                }
            }
        },
    )
}

/** 开发者 */
@Composable
fun DeveloperScreen(bottomInnerPadding: Dp, onBack: () -> Unit) {
    val context = LocalContext.current
    val internal = AppPrefs.getInstance().internal
    PageScaffold(
        title = stringResource(R.string.developer),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                NavRow(
                    title = stringResource(R.string.real_time_logs),
                    icon = Icons.Rounded.BugReport,
                    onClick = { context.startActivity<LogActivity>() },
                )
                SwitchRow(
                    pref = internal.verboseLog,
                    title = stringResource(R.string.verbose_log),
                )
                SwitchRow(
                    pref = internal.editorInfoInspector,
                    title = stringResource(R.string.editor_info_inspector),
                )
                NavRow(
                    title = stringResource(R.string.restart_fcitx_instance),
                    icon = Icons.Rounded.Refresh,
                    onClick = {
                        FcitxDaemon.stopFcitx()
                        FcitxDaemon.startFcitx()
                        context.toast(context.getString(R.string.restart_fcitx_instance))
                    },
                )
            }
        }
    }
}

/** 关于 */
@Composable
fun AboutScreen(bottomInnerPadding: Dp, onBack: () -> Unit, onOpenLicenses: () -> Unit) {
    val context = LocalContext.current
    PageScaffold(
        title = stringResource(R.string.about),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                NavRow(
                    title = stringResource(R.string.source_code),
                    summary = stringResource(R.string.github_repo),
                    icon = Icons.Rounded.Info,
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                android.net.Uri.parse(org.fcitx.fcitx5.android.utils.Const.githubRepo),
                            )
                        )
                    },
                )
                NavRow(
                    title = stringResource(R.string.license),
                    summary = org.fcitx.fcitx5.android.utils.Const.licenseSpdxId,
                    icon = Icons.Rounded.LibraryBooks,
                    onClick = { onOpenLicenses() },
                )
            }
        }
        item {
            SettingsGroup(
                title = stringResource(R.string.ksu_app_section_theme),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                NavRow(
                    title = stringResource(R.string.ksu_app_reset),
                    icon = Icons.Rounded.Restore,
                    onClick = { AppearancePrefs.ensureInitialized(context).resetToDefaults() },
                )
            }
        }
    }
}

/** 全局选项：Fcitx 全局配置 */
@Composable
fun GlobalConfigScreen(
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    viewModel: MainViewModel,
    onOpenRoute: (SettingsRoute) -> Unit,
) {
    FcitxConfigScreen(
        title = stringResource(R.string.global_options),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
        fcitx = viewModel.fcitx,
        loadConfig = { getGlobalConfig() },
        saveConfig = { setGlobalConfig(it) },
        onOpenRoute = onOpenRoute,
    )
}

/** 单个输入法的配置 */
@Composable
fun InputMethodConfigScreen(
    uniqueName: String,
    title: String,
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    viewModel: MainViewModel,
    onOpenRoute: (SettingsRoute) -> Unit,
) {
    FcitxConfigScreen(
        title = title,
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
        fcitx = viewModel.fcitx,
        loadConfig = { getImConfig(uniqueName) },
        saveConfig = { setImConfig(uniqueName, it) },
        onOpenRoute = onOpenRoute,
    )
}

/** 附加组件配置；table 组件额外挂上"码表输入法"入口，与旧实现一致。 */
@Composable
fun AddonConfigScreen(
    addon: String,
    title: String,
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    viewModel: MainViewModel,
    onOpenRoute: (SettingsRoute) -> Unit,
) {
    val context = LocalContext.current
    FcitxConfigScreen(
        title = title,
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
        fcitx = viewModel.fcitx,
        loadConfig = {
            val raw = getAddonConfig(addon)
            if (addon == "table") {
                raw.findByName("desc")?.findByName("TableGlobalConfig")?.let { node ->
                    node.subItems = (node.subItems ?: emptyArray()) + org.fcitx.fcitx5.android.core.RawConfig(
                        "AndroidTable",
                        subItems = arrayOf(
                            org.fcitx.fcitx5.android.core.RawConfig("Type", "External"),
                            org.fcitx.fcitx5.android.core.RawConfig(
                                "Description",
                                context.getString(R.string.manage_table_im)
                            ),
                        ),
                    )
                }
            }
            raw
        },
        saveConfig = { setAddonConfig(addon, it) },
        onOpenRoute = onOpenRoute,
    )
}
