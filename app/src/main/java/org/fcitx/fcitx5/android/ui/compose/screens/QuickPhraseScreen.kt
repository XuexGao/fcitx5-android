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
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.MoreHoriz
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.reloadQuickPhrase
import org.fcitx.fcitx5.android.data.quickphrase.BuiltinQuickPhrase
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhrase
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhraseData
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhraseEntry
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhraseManager
import org.fcitx.fcitx5.android.ui.compose.component.DialogField
import org.fcitx.fcitx5.android.ui.compose.component.EditFieldsDialog
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.utils.importErrorDialog
import org.fcitx.fcitx5.android.utils.queryFileName
import org.fcitx.fcitx5.android.utils.toast
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/** Immutable view of a [QuickPhrase] row, so mutating the entry still triggers recomposition. */
private data class QuickPhraseRow(
    val item: QuickPhrase,
    val name: String,
    val enabled: Boolean,
    val builtin: Boolean,
    val hasOverride: Boolean,
)

private fun rowOf(item: QuickPhrase) = QuickPhraseRow(
    item = item,
    name = item.name,
    enabled = item.isEnabled,
    builtin = item is BuiltinQuickPhrase,
    hasOverride = (item as? BuiltinQuickPhrase)?.override != null,
)

/**
 * 快捷短语 list: every `.mb` file fcitx ships plus the user's own, each with an on/off checkbox.
 *
 * Built-in phrases cannot be removed, but they can be disabled or edited — editing one materialises
 * an override file, and "恢复" drops that override again, exactly like the old screen's popup menu.
 * Importing uses the system file picker; creating asks for a name.
 */
@Composable
fun QuickPhraseListScreen(
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    viewModel: MainViewModel,
    onOpenRoute: (SettingsRoute) -> Unit,
) {
    val context = LocalContext.current
    val rows = remember { mutableStateListOf<QuickPhraseRow>() }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var addMenu by remember { mutableStateOf(false) }
    var createDialog by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<QuickPhraseRow?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) { QuickPhraseManager.listQuickPhrase().map(::rowOf) }
        }.onSuccess {
            rows.clear()
            rows.addAll(it)
        }.onFailure { Timber.w(it, "Unable to list quick phrases") }
        loaded = true
    }

    fun pushReload() {
        if (busy) return
        busy = true
        FcitxApplication.getInstance().coroutineScope.launch {
            runCatching { viewModel.fcitx.runOnReady { reloadQuickPhrase() } }
                .onFailure { Timber.w(it, "reloadQuickPhrase failed") }
            busy = false
        }
    }

    fun importFrom(uri: Uri) {
        FcitxApplication.getInstance().coroutineScope.launch {
            val fileName = context.contentResolver.queryFileName(uri)
            if (fileName == null) {
                context.importErrorDialog(R.string.exception_quickphrase_filename, "")
                return@launch
            }
            if (fileName.substringAfterLast('.') != QuickPhrase.EXT) {
                context.importErrorDialog(R.string.exception_quickphrase_filename, fileName)
                return@launch
            }
            val entryName = fileName.substringBeforeLast('.')
            if (rows.any { it.name == entryName }) {
                context.importErrorDialog(R.string.quickphrase_already_exists)
                return@launch
            }
            val imported = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: error("Unable to open $uri")
                    QuickPhraseManager.importFromInputStream(stream, fileName).getOrThrow()
                }
            }
            imported.onSuccess { rows.add(rowOf(it)) }
                .onFailure { context.importErrorDialog(it) }
            pushReload()
        }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) importFrom(uri)
    }

    PageScaffold(
        title = stringResource(R.string.quickphrase_editor),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        if (!loaded) {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    NavRow(title = stringResource(R.string.loading), icon = Icons.Rounded.FormatQuote)
                }
            }
            return@PageScaffold
        }
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                rows.forEachIndexed { index, row ->
                    CheckboxPreference(
                        title = row.name,
                        checked = row.enabled,
                        endActions = {
                            if (row.enabled) {
                                IconButton(
                                    onClick = {
                                        if (row.builtin && row.hasOverride) {
                                            menuFor = row
                                        } else {
                                            onOpenRoute(SettingsRoute.QuickPhraseEdit(row.item))
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (row.builtin && row.hasOverride)
                                            Icons.Rounded.MoreHoriz else Icons.Rounded.Edit,
                                        contentDescription = stringResource(R.string.edit),
                                        tint = colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                            }
                            if (!row.builtin) {
                                IconButton(
                                    onClick = {
                                        row.item.file.delete()
                                        rows.removeAt(index)
                                        pushReload()
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                            }
                        },
                        onCheckedChange = { checked ->
                            if (checked) row.item.enable() else row.item.disable()
                            rows[index] = rowOf(row.item)
                            pushReload()
                        },
                    )
                }
            }
        }
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                NavRow(
                    title = stringResource(R.string.add),
                    icon = Icons.Rounded.Add,
                    onClick = { addMenu = true },
                )
            }
        }
    }

    if (addMenu) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.quickphrase_editor)
            .setItems(
                arrayOf(
                    context.getString(R.string.import_from_file),
                    context.getString(R.string.create_new),
                )
            ) { _, which ->
                addMenu = false
                if (which == 0) importer.launch("*/*") else createDialog = true
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> addMenu = false }
            .show()
    }

    if (createDialog) {
        EditFieldsDialog(
            title = stringResource(R.string.create_new),
            fields = listOf(DialogField(label = stringResource(R.string.name))),
            onDismiss = { createDialog = false },
            onConfirm = { values ->
                createDialog = false
                runCatching { QuickPhraseManager.newEmpty(values[0].trim()) }
                    .onSuccess { rows.add(rowOf(it)) }
                    .onFailure { context.toast(it) }
            },
        )
    }

    val menuRow = menuFor
    if (menuRow != null) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(menuRow.name)
            .setItems(
                arrayOf(
                    context.getString(R.string.edit),
                    context.getString(R.string.reset),
                )
            ) { _, which ->
                menuFor = null
                when (which) {
                    0 -> onOpenRoute(SettingsRoute.QuickPhraseEdit(menuRow.item))
                    1 -> {
                        (menuRow.item as? BuiltinQuickPhrase)?.deleteOverride()
                        val index = rows.indexOfFirst { it.name == menuRow.name }
                        if (index >= 0) rows[index] = rowOf(menuRow.item)
                        pushReload()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> menuFor = null }
            .show()
    }
}

/**
 * 快捷短语 editor: keyword → phrase pairs of one `.mb` file.
 *
 * Every confirmed edit is written through [QuickPhrase.saveData] immediately (the old screen waited
 * for `onStop`), then the running fcitx instance is asked to reload, so a change can never be lost
 * by leaving the screen and is visible to the keyboard right away.
 */
@Composable
fun QuickPhraseEditScreen(
    quickPhrase: QuickPhrase,
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    viewModel: MainViewModel,
) {
    val entries = remember { mutableStateListOf<QuickPhraseEntry>() }
    var loaded by remember { mutableStateOf(false) }
    var dialogOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<QuickPhraseEntry?>(null) }

    fun pushReload() {
        FcitxApplication.getInstance().coroutineScope.launch {
            runCatching { viewModel.fcitx.runOnReady { reloadQuickPhrase() } }
                .onFailure { Timber.w(it, "reloadQuickPhrase failed") }
        }
    }

    fun save() {
        val snapshot = QuickPhraseData(entries.toList())
        FcitxApplication.getInstance().coroutineScope.launch {
            withContext(Dispatchers.IO) { quickPhrase.saveData(snapshot) }
            pushReload()
        }
    }

    LaunchedEffect(quickPhrase) {
        runCatching { withContext(Dispatchers.IO) { quickPhrase.loadData().toList() } }
            .onSuccess {
                entries.clear()
                entries.addAll(it)
            }
            .onFailure { Timber.w(it, "Unable to load quick phrase ${quickPhrase.name}") }
        loaded = true
    }

    PageScaffold(
        title = quickPhrase.name,
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        if (!loaded) {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    NavRow(title = stringResource(R.string.loading), icon = Icons.Rounded.FormatQuote)
                }
            }
            return@PageScaffold
        }
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                entries.forEachIndexed { index, entry ->
                    NavRow(
                        title = entry.keyword,
                        summary = entry.phrase.replace("\n", "\\n"),
                        onClick = {
                            editing = entry
                            dialogOpen = true
                        },
                        endActions = {
                            IconButton(
                                onClick = {
                                    entries.removeAt(index)
                                    save()
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
                    title = stringResource(R.string.add),
                    icon = Icons.Rounded.Add,
                    onClick = {
                        editing = null
                        dialogOpen = true
                    },
                )
            }
        }
    }

    if (dialogOpen) {
        EditFieldsDialog(
            title = stringResource(R.string.quickphrase_editor),
            fields = listOf(
                DialogField(
                    label = stringResource(R.string.quickphrase_keyword),
                    initial = editing?.keyword.orEmpty(),
                ),
                DialogField(
                    label = stringResource(R.string.quickphrase_phrase),
                    initial = editing?.phrase.orEmpty(),
                    singleLine = false,
                ),
            ),
            onDismiss = { dialogOpen = false },
            onConfirm = { values ->
                dialogOpen = false
                val newEntry = QuickPhraseEntry(values[0].trim(), values[1])
                val index = entries.indexOf(editing)
                if (index >= 0) entries[index] = newEntry else entries.add(newEntry)
                save()
            },
        )
    }
}
