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
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
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
import org.fcitx.fcitx5.android.core.reloadPinyinDict
import org.fcitx.fcitx5.android.data.pinyin.PinyinDictManager
import org.fcitx.fcitx5.android.data.pinyin.dict.BuiltinDictionary
import org.fcitx.fcitx5.android.data.pinyin.dict.LibIMEDictionary
import org.fcitx.fcitx5.android.data.pinyin.dict.PinyinDictionary
import org.fcitx.fcitx5.android.ui.compose.component.CheckboxRow
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.utils.importErrorDialog
import org.fcitx.fcitx5.android.utils.queryFileName
import org.fcitx.fcitx5.android.utils.toast
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/** Immutable row snapshot so enabling/disabling a dictionary redraws its checkbox. */
private data class PinyinDictRow(
    val item: PinyinDictionary,
    val name: String,
    val enabled: Boolean,
    val builtin: Boolean,
)

private fun dictRowOf(item: PinyinDictionary) = PinyinDictRow(
    item = item,
    name = item.name,
    enabled = (item as? LibIMEDictionary)?.isEnabled ?: true,
    builtin = item is BuiltinDictionary,
)

/**
 * 拼音词典: the dictionaries libime loads, built-in ones first.
 *
 * Built-in dictionaries can be disabled but not removed; user ones (`.dict`, `.scel`, `.txt`) are
 * imported through the system picker and converted on a background thread. Disabling, deleting or
 * importing asks the running fcitx instance to reload the dictionaries.
 */
@Composable
fun PinyinDictionaryScreen(
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val rows = remember { mutableStateListOf<PinyinDictRow>() }
    var loaded by remember { mutableStateOf(false) }

    fun pushReload() {
        FcitxApplication.getInstance().coroutineScope.launch {
            runCatching { viewModel.fcitx.runOnReady { reloadPinyinDict() } }
                .onFailure { Timber.w(it, "reloadPinyinDict failed") }
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) { PinyinDictManager.listDictionaries().map(::dictRowOf) }
        }.onSuccess {
            rows.clear()
            rows.addAll(it)
        }.onFailure { Timber.w(it, "Unable to list pinyin dictionaries") }
        loaded = true
    }

    fun importFrom(uri: Uri) {
        FcitxApplication.getInstance().coroutineScope.launch {
            val fileName = context.contentResolver.queryFileName(uri) ?: return@launch
            if (PinyinDictionary.Type.fromFileName(fileName) == null) {
                context.importErrorDialog(R.string.invalid_dict)
                return@launch
            }
            val entryName = fileName.substringBeforeLast('.')
            if (rows.any { it.name == entryName }) {
                context.importErrorDialog(R.string.dict_already_exists)
                return@launch
            }
            val imported = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: error("Unable to open $uri")
                    PinyinDictManager.importFromInputStream(stream, fileName).getOrThrow()
                }
            }
            imported.onSuccess {
                rows.add(dictRowOf(it))
                pushReload()
                context.toast(R.string.done)
            }.onFailure { context.importErrorDialog(it) }
        }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) importFrom(uri)
    }

    PageScaffold(
        title = stringResource(R.string.pinyin_dict),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        if (!loaded) {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    NavRow(title = stringResource(R.string.loading), icon = Icons.AutoMirrored.Rounded.LibraryBooks)
                }
            }
            return@PageScaffold
        }
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                rows.forEachIndexed { index, row ->
                    CheckboxRow(
                        title = row.name,
                        checked = row.enabled,
                        enabled = !row.builtin,
                        endActions = {
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
                            val dict = row.item as? LibIMEDictionary
                            if (dict != null) {
                                if (checked) dict.enable() else dict.disable()
                                rows[index] = dictRowOf(dict)
                                pushReload()
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
                    onClick = { importer.launch("*/*") },
                )
            }
        }
    }
}
