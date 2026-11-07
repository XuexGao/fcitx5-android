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
import org.fcitx.fcitx5.android.ui.compose.component.DropdownRow
import org.fcitx.fcitx5.android.data.rime.RimeDictState
import org.fcitx.fcitx5.android.data.rime.RimeDictPackages
import org.fcitx.fcitx5.android.data.rime.RimeDictDownloader
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
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
    val dictState by RimeDictDownloader.state.collectAsStateWithLifecycle()

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
        dictionarySection(prefs, revision, scope) { revision++ }
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

/**
 * 词库: installs or updates the 万象拼音 schema package (Base, or a Pro 辅助码 variant) and the
 * optional language model. The package is downloaded from the project's GitHub releases, through a
 * mirror first because the direct host is often unreachable.
 */
private fun LazyListScope.dictionarySection(
    prefs: RimePrefs,
    revision: Int,
    scope: kotlinx.coroutines.CoroutineScope,
    onChanged: () -> Unit,
) {
    item {
        val variant = RimeDictPackages.Variant.fromId(prefs.dictVariant.getValue())
        val scheme = prefs.dictScheme.getValue().ifBlank { RimeDictPackages.PRO_SCHEMES.first().first }
        val installedTag = prefs.dictInstalledTag.getValue()
        val installed = remember(revision) { RimeManager.isWanxiangAvailable() }
        val version = remember(revision) {
            RimeManager.installedDictVersion() ?: installedTag.ifBlank { null }
        }
        val schemeLabels = RimeDictPackages.PRO_SCHEMES.map { it.second }
        val schemeIndex = RimeDictPackages.PRO_SCHEMES.indexOfFirst { it.first == scheme }
            .coerceAtLeast(0)

        SettingsGroup(
            title = stringResource(R.string.ksu_rime_dict),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            DropdownRow(
                title = stringResource(R.string.ksu_rime_dict_variant),
                summary = variant.label,
                icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                items = RimeDictPackages.Variant.entries.map { it.label },
                selectedIndex = RimeDictPackages.Variant.entries.indexOf(variant),
                onSelectedIndexChange = { index ->
                    prefs.dictVariant.setValue(RimeDictPackages.Variant.entries[index].id)
                    onChanged()
                },
            )
            if (variant == RimeDictPackages.Variant.Pro) {
                DropdownRow(
                    title = stringResource(R.string.ksu_rime_dict_scheme),
                    icon = Icons.Rounded.Tune,
                    items = schemeLabels,
                    selectedIndex = schemeIndex,
                    onSelectedIndexChange = { index ->
                        prefs.dictScheme.setValue(RimeDictPackages.PRO_SCHEMES[index].first)
                        onChanged()
                    },
                )
            }
            SwitchRow(
                pref = prefs.dictGrammar,
                title = stringResource(R.string.ksu_rime_dict_grammar),
                summary = stringResource(R.string.ksu_rime_dict_grammar_summary),
            )
            NavRow(
                title = if (installed) stringResource(R.string.ksu_rime_dict_update)
                else stringResource(R.string.ksu_rime_dict_install),
                summary = when (val state = dictState) {
                    is RimeDictState.Downloading ->
                        "${state.what} ${state.percent}%"

                    is RimeDictState.Extracting -> "${state.what}…"
                    is RimeDictState.Resolving -> stringResource(R.string.ksu_rime_dict_resolving)
                    is RimeDictState.Failed -> state.message
                    is RimeDictState.Done -> state.detail
                    RimeDictState.Idle -> buildString {
                        if (installed) {
                            append(stringResource(R.string.ksu_rime_available))
                            version?.let { append(" · ").append(it) }
                        } else {
                            append(stringResource(R.string.ksu_rime_unavailable))
                        }
                    }
                },
                icon = Icons.Rounded.CloudDownload,
                enabled = dictState !is RimeDictState.Downloading &&
                    dictState !is RimeDictState.Extracting &&
                    dictState !is RimeDictState.Resolving,
                onClick = {
                    scope.launch {
                        RimeDictDownloader.install(
                            variant = variant,
                            scheme = scheme,
                            withGrammar = prefs.dictGrammar.getValue(),
                        )
                        onChanged()
                    }
                },
            )
        }
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

private fun LazyListScope.defaultSchemaSection(
    prefs: RimePrefs,
    revision: Int,
    onChanged: () -> Unit,
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

private fun LazyListScope.doublePinyinSection(
    prefs: RimePrefs,
    revision: Int,
    onChanged: () -> Unit,
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

private fun LazyListScope.parametersSection(prefs: RimePrefs, onChanged: () -> Unit) {
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
        top.yukonga.miuix.kmp.basic.Text(
            text = stringResource(R.string.ksu_rime_bundled_license),
            style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.footnote1,
            color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}
