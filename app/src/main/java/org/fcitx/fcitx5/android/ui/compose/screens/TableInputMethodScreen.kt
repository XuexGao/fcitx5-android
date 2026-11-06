/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.data.table.TableBasedInputMethod
import org.fcitx.fcitx5.android.data.table.TableManager
import org.fcitx.fcitx5.android.data.table.dict.Dictionary
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.utils.importErrorDialog
import org.fcitx.fcitx5.android.utils.queryFileName
import org.fcitx.fcitx5.android.utils.toast
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/** Immutable row snapshot so replacing an input method's dictionary redraws its icon. */
private data class TableRow(
    val item: TableBasedInputMethod,
    val name: String,
    val hasTable: Boolean,
)

private fun tableRowOf(item: TableBasedInputMethod) =
    TableRow(item, item.name, item.tableFileExists)

/**
 * 码表输入法: the table-based input methods installed under `data/inputmethod`.
 *
 * Importing supports both a single zip (conf + dict) and picking the two files one after another —
 * the old screen showed one dialog with two slots, which in Compose becomes two system pickers in
 * sequence with the same validation in between. A row whose dictionary is missing offers to repair
 * itself, and any change restarts fcitx once, when the screen is left.
 */
@Composable
fun TableInputMethodScreen(
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val rows = remember { mutableStateListOf<TableRow>() }
    var loaded by remember { mutableStateOf(false) }
    var importMenu by remember { mutableStateOf(false) }
    val pendingConf = remember { mutableStateOf<Uri?>(null) }
    val replaceFor = remember { mutableStateOf<TableRow?>(null) }
    val missingFor = remember { mutableStateOf<TableRow?>(null) }
    var dirty by remember { mutableStateOf(false) }

    // fcitx has to re-read the input method directory; one restart per visit is enough.
    DisposableEffect(Unit) {
        onDispose { if (dirty) FcitxDaemon.restartFcitx() }
    }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) { TableManager.inputMethods().map(::tableRowOf) }
        }.onSuccess {
            rows.clear()
            rows.addAll(it)
        }.onFailure { Timber.w(it, "Unable to list table input methods") }
        loaded = true
    }

    fun importZip(uri: Uri) {
        FcitxApplication.getInstance().coroutineScope.launch {
            val fileName = context.contentResolver.queryFileName(uri) ?: return@launch
            if (!fileName.endsWith(".zip")) {
                context.importErrorDialog(R.string.exception_table_im_filename, fileName)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: error("Unable to open $uri")
                    TableManager.importFromZip(stream).getOrThrow()
                }
            }
            result.onSuccess {
                rows.add(tableRowOf(it))
                dirty = true
                context.toast(R.string.done)
            }.onFailure { context.importErrorDialog(it) }
        }
    }

    /**
     * Checks the picked `.conf` synchronously (only the import itself needs a background thread), so
     * the dictionary picker is not opened for a file that would be rejected anyway.
     */
    fun acceptConf(uri: Uri): Boolean {
        val fileName = context.contentResolver.queryFileName(uri) ?: return false
        if (!fileName.removeSuffix(".in").endsWith(".conf")) {
            FcitxApplication.getInstance().coroutineScope.launch {
                context.importErrorDialog(R.string.exception_table_conf_filename, fileName)
            }
            return false
        }
        pendingConf.value = uri
        return true
    }

    fun importConfAndDict(dictUri: Uri) {
        val confUri = pendingConf.value ?: return
        FcitxApplication.getInstance().coroutineScope.launch {
            val confName = context.contentResolver.queryFileName(confUri) ?: return@launch
            val dictName = context.contentResolver.queryFileName(dictUri) ?: return@launch
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val confStream = context.contentResolver.openInputStream(confUri)
                        ?: error("Unable to open $confUri")
                    val dictStream = context.contentResolver.openInputStream(dictUri)
                        ?: error("Unable to open $dictUri")
                    TableManager.importFromConfAndDict(confName, confStream, dictName, dictStream)
                        .getOrThrow()
                }
            }
            result.onSuccess {
                pendingConf.value = null
                rows.add(tableRowOf(it))
                dirty = true
                context.toast(R.string.done)
            }.onFailure { context.importErrorDialog(it) }
        }
    }

    fun replaceDict(uri: Uri) {
        val target = replaceFor.value
        replaceFor.value = null
        if (target == null) return
        FcitxApplication.getInstance().coroutineScope.launch {
            val dictName = context.contentResolver.queryFileName(uri) ?: return@launch
            if (Dictionary.Type.fromFileName(dictName) == null) {
                context.importErrorDialog(R.string.exception_table_dict_filename, dictName)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: error("Unable to open $uri")
                    TableManager.replaceTableDict(target.item, dictName, stream).getOrThrow()
                }
            }
            result.onSuccess { table ->
                target.item.table = table
                val index = rows.indexOfFirst { it.item === target.item }
                if (index >= 0) rows[index] = tableRowOf(target.item)
                dirty = true
                context.toast(R.string.done)
            }.onFailure { context.importErrorDialog(it) }
        }
    }

    val zipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) importZip(uri)
    }
    val replaceLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) replaceDict(uri)
        }

    // Declared before the conf picker: picking a conf immediately asks for the dictionary.
    val dictLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) importConfAndDict(uri)
    }
    val confLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && acceptConf(uri)) dictLauncher.launch("*/*")
    }

    PageScaffold(
        title = stringResource(R.string.table_im),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        if (!loaded) {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    NavRow(title = stringResource(R.string.loading), icon = Icons.Rounded.Keyboard)
                }
            }
            return@PageScaffold
        }
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                rows.forEachIndexed { index, row ->
                    val requestEdit = {
                        if (row.hasTable) replaceFor.value = row else missingFor.value = row
                        Unit
                    }
                    NavRow(
                        title = row.name,
                        summary = if (row.hasTable) null
                        else stringResource(R.string.table_file_does_not_exist_title),
                        onClick = requestEdit,
                        endActions = {
                            IconButton(onClick = requestEdit) {
                                Icon(
                                    imageVector = if (row.hasTable) Icons.Rounded.Edit
                                    else Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariantSummary,
                                )
                            }
                            IconButton(
                                onClick = {
                                    row.item.delete()
                                    rows.removeAt(index)
                                    dirty = true
                                    FcitxDaemon.restartFcitx()
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
                    title = stringResource(R.string.import_table),
                    icon = Icons.Rounded.Add,
                    onClick = { importMenu = true },
                )
            }
        }
    }

    if (importMenu) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.import_table)
            .setItems(
                arrayOf(
                    context.getString(R.string.table_from_zip),
                    context.getString(R.string.table_select_files),
                )
            ) { _, which ->
                importMenu = false
                when (which) {
                    0 -> zipLauncher.launch("application/zip")
                    1 -> {
                        pendingConf.value = null
                        confLauncher.launch("*/*")
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> importMenu = false }
            .show()
    }

    val missing = missingFor.value
    if (missing != null) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setTitle(R.string.table_file_does_not_exist_title)
            .setMessage(
                context.getString(
                    R.string.table_file_does_not_exist_message,
                    missing.item.tableFileName,
                )
            )
            .setPositiveButton(android.R.string.ok) { _, _ -> missingFor.value = null }
            .setNeutralButton(R.string.table_file_placeholder) { _, _ ->
                missingFor.value = null
                replaceFor.value = missing
                replaceLauncher.launch("*/*")
            }
            .show()
    }

    val replacing = replaceFor.value
    if (replacing != null && replacing.hasTable) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.update_table)
            .setMessage(
                context.getString(
                    R.string.table_dict_replace_message,
                    replacing.item.tableFileName,
                )
            )
            .setNeutralButton(R.string.table_file_placeholder) { _, _ ->
                replaceLauncher.launch("*/*")
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> replaceFor.value = null }
            .show()
    }
}
