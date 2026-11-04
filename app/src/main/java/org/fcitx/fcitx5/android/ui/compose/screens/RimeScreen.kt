/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.rime.RimeAutoDeploy
import org.fcitx.fcitx5.android.data.rime.RimeManager
import org.fcitx.fcitx5.android.data.rime.RimePrefs
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.compose.component.SliderRow
import org.fcitx.fcitx5.android.ui.compose.component.SwitchRow
import java.io.File

/**
 * Rime 方案: default schema, double-pinyin layout, a few parameters, and deploy/import/export.
 *
 * All edits are rendered into `*.custom.yaml` in Rime's user directory by [RimeManager]; the engine
 * picks them up on the next deploy (which fcitx5-rime performs automatically the first time).
 */
@Composable
fun RimeScreen(
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    onRedeploy: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { RimePrefs.ensureInitialized(context) }
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/yaml")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                val tmp = File(context.cacheDir, "rime-export.yaml")
                RimeManager.exportConfiguration(context, tmp)
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        tmp.inputStream().use { it.copyTo(out) }
                    }
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val tmp = File(context.cacheDir, "rime-import.yaml")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tmp.outputStream().use { input.copyTo(it) }
                    }
                    RimeManager.importConfiguration(context, tmp)
                }
            }
            revision++
        }
    }

    PageScaffold(
        title = stringResource(R.string.ksu_rime_title),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        statusSection(prefs, revision)
        defaultSchemaSection(prefs, revision) { revision++ }
        doublePinyinSection(prefs, revision) { revision++ }
        parametersSection(prefs) { revision++ }
        actionsSection(
            onRedeploy = {
                scope.launch { RimeAutoDeploy.ensureDeployed(context) }
                onRedeploy()
            },
            onReset = {
                RimeManager.resetToDefaults(context)
                revision++
            },
            onExport = { exportLauncher.launch("rime-config.yaml") },
            onImport = { importLauncher.launch(arrayOf("application/yaml", "text/plain", "*/*")) },
        )
    }
}

private fun LazyListScope.statusSection(prefs: RimePrefs, revision: Int) {
    item {
        val available = remember(revision) { RimeManager.isWanxiangAvailable() }
        SettingsGroup(
            title = stringResource(R.string.ksu_rime_status),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            NavRow(
                title = stringResource(R.string.ksu_rime_title),
                summary = stringResource(
                    if (available) R.string.ksu_rime_available else R.string.ksu_rime_unavailable
                ),
                icon = if (available) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
            )
            NavRow(
                title = stringResource(R.string.ksu_rime_default_schema),
                summary = prefs.defaultSchema.getValue(),
                icon = Icons.Rounded.Language,
            )
        }
    }
}

private inline fun LazyListScope.defaultSchemaSection(
    prefs: RimePrefs,
    revision: Int,
    crossinline onChanged: () -> Unit,
) {
    item {
        val context = LocalContext.current
        val schemas = remember(revision) { RimeManager.availableSchemas(context) }
        val current = prefs.defaultSchema.getValue()
        SettingsGroup(
            title = stringResource(R.string.ksu_rime_default_schema),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            schemas.forEach { schema ->
                NavRow(
                    title = schema.displayName,
                    summary = if (schema.id == current) {
                        stringResource(R.string.ksu_rime_available)
                    } else {
                        schema.id
                    },
                    icon = if (schema.id == current) Icons.Rounded.CheckCircle
                    else Icons.Rounded.Language,
                    onClick = {
                        prefs.defaultSchema.setValue(schema.id)
                        prefs.enabledSchemas.setValue(prefs.enabledSchemas.getValue() + schema.id)
                        RimeManager.applyConfiguration(context, force = true)
                        onChanged()
                    },
                )
            }
            if (schemas.isEmpty()) {
                NavRow(
                    title = stringResource(R.string.ksu_rime_unavailable),
                    icon = Icons.Rounded.Info,
                )
            }
        }
    }
}

private inline fun LazyListScope.doublePinyinSection(
    prefs: RimePrefs,
    revision: Int,
    crossinline onChanged: () -> Unit,
) {
    item {
        val context = LocalContext.current
        val current = RimePrefs.DoublePinyin.fromId(prefs.doublePinyin.getValue())
        SettingsGroup(
            title = stringResource(R.string.ksu_rime_double_pinyin),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            RimePrefs.DoublePinyin.entries.forEach { option ->
                NavRow(
                    title = option.label,
                    summary = "wanxiang_algebra:/base/${option.id}",
                    icon = if (option == current) Icons.Rounded.CheckCircle
                    else Icons.Rounded.Language,
                    onClick = {
                        prefs.doublePinyin.setValue(option.id)
                        RimeManager.applyConfiguration(context, force = true)
                        onChanged()
                    },
                )
            }
        }
    }
}

private inline fun LazyListScope.parametersSection(prefs: RimePrefs, crossinline onChanged: () -> Unit) {
    item {
        SettingsGroup(
            title = stringResource(R.string.ksu_rime_parameters),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            SliderRow(
                pref = prefs.pageSize,
                title = stringResource(R.string.ksu_rime_page_size),
                range = 3f..10f,
                valueText = { it.toString() },
                onValueSettled = onChanged,
            )
            SwitchRow(
                pref = prefs.fuzzyZhZ,
                title = stringResource(R.string.ksu_rime_fuzzy_z_zh),
                onChanged = onChanged,
            )
            SwitchRow(
                pref = prefs.fuzzyChC,
                title = stringResource(R.string.ksu_rime_fuzzy_c_ch),
                onChanged = onChanged,
            )
            SwitchRow(
                pref = prefs.fuzzyShS,
                title = stringResource(R.string.ksu_rime_fuzzy_s_sh),
                onChanged = onChanged,
            )
            SwitchRow(
                pref = prefs.fuzzyNL,
                title = stringResource(R.string.ksu_rime_fuzzy_n_l),
                onChanged = onChanged,
            )
            SwitchRow(
                pref = prefs.fuzzyInIng,
                title = stringResource(R.string.ksu_rime_fuzzy_in_ing),
                onChanged = onChanged,
            )
            SwitchRow(
                pref = prefs.fuzzyAnAng,
                title = stringResource(R.string.ksu_rime_fuzzy_an_ang),
                onChanged = onChanged,
            )
        }
    }
}

private fun LazyListScope.actionsSection(
    onRedeploy: () -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    item {
        SettingsGroup(
            title = stringResource(R.string.ksu_rime_actions),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            NavRow(
                title = stringResource(R.string.ksu_rime_redeploy),
                summary = stringResource(R.string.ksu_rime_redeploy_summary),
                icon = Icons.Rounded.Sync,
                onClick = onRedeploy,
            )
            NavRow(
                title = stringResource(R.string.ksu_rime_export),
                icon = Icons.Rounded.Share,
                onClick = onExport,
            )
            NavRow(
                title = stringResource(R.string.ksu_rime_import),
                icon = Icons.Rounded.Upload,
                onClick = onImport,
            )
            NavRow(
                title = stringResource(R.string.ksu_rime_reset),
                icon = Icons.Rounded.SettingsBackupRestore,
                onClick = onReset,
            )
        }
    }

    item {
        androidx.compose.material3.Text(
            text = stringResource(R.string.ksu_rime_bundled_license),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}
