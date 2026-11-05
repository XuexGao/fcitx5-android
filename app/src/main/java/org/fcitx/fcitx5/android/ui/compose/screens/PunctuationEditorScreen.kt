/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.core.getPunctuationConfig
import org.fcitx.fcitx5.android.data.punctuation.PunctuationManager
import org.fcitx.fcitx5.android.data.punctuation.PunctuationMapEntry
import org.fcitx.fcitx5.android.ui.compose.component.DialogField
import org.fcitx.fcitx5.android.ui.compose.component.EditFieldsDialog
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private const val DEFAULT_LANG = "zh_CN"

/** Index used by [PunctuationEditorScreen] while a brand new entry is being typed. */
private const val NEW_ENTRY = -1

/**
 * 标点编辑器: the `key → mapping / altMapping` table of one language.
 *
 * Column titles come from the config descriptor fcitx returns, so fcitx translates them itself.
 * Entries can be reordered, edited and deleted in place; every change is written back immediately
 * through [PunctuationManager.save].
 */
@Composable
fun PunctuationEditorScreen(
    title: String,
    lang: String?,
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    viewModel: MainViewModel,
) {
    val entries = remember { mutableStateListOf<PunctuationMapEntry>() }
    val labels = remember { mutableStateOf(listOf("Key", "Mapping", "AltMapping")) }
    var loaded by remember { mutableStateOf(false) }
    var dialogFor by remember { mutableStateOf<Int?>(null) }
    val language = lang ?: DEFAULT_LANG

    fun persist() {
        val snapshot = entries.toList()
        FcitxApplication.getInstance().coroutineScope.launch {
            runCatching {
                viewModel.fcitx.runOnReady {
                    PunctuationManager.save(this, language, snapshot)
                }
            }.onFailure { Timber.w(it, "Unable to save punctuation config") }
        }
    }

    LaunchedEffect(language) {
        runCatching {
            val raw = viewModel.fcitx.runOnReady { getPunctuationConfig(language) }
            readLabels(raw) to PunctuationManager.parseRawConfig(raw)
        }.onSuccess { (descriptions, parsed) ->
            labels.value = descriptions
            entries.clear()
            entries.addAll(parsed)
        }.onFailure { Timber.w(it, "Unable to load punctuation config") }
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
                    NavRow(title = stringResource(R.string.loading), icon = Icons.Rounded.FormatQuote)
                }
            }
            return@PageScaffold
        }
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                entries.forEachIndexed { index, entry ->
                    NavRow(
                        title = entry.key,
                        summary = listOf(entry.mapping, entry.altMapping)
                            .filter { it.isNotEmpty() }
                            .joinToString(" "),
                        onClick = { dialogFor = index },
                        endActions = {
                            IconButton(
                                enabled = index > 0,
                                onClick = {
                                    val moved = entries.removeAt(index)
                                    entries.add(index - 1, moved)
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
                                enabled = index < entries.lastIndex,
                                onClick = {
                                    val moved = entries.removeAt(index)
                                    entries.add(index + 1, moved)
                                    persist()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariantSummary,
                                )
                            }
                            IconButton(onClick = { dialogFor = index }) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = stringResource(R.string.edit),
                                    tint = colorScheme.onSurfaceVariantSummary,
                                )
                            }
                            IconButton(
                                onClick = {
                                    entries.removeAt(index)
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
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                NavRow(
                    title = stringResource(R.string.add),
                    icon = Icons.Rounded.Add,
                    onClick = { dialogFor = NEW_ENTRY },
                )
            }
        }
    }

    val dialogIndex = dialogFor
    if (dialogIndex != null) {
        val editing = entries.getOrNull(dialogIndex)
        EditFieldsDialog(
            title = title,
            fields = listOf(
                DialogField(label = labels.value[0], initial = editing?.key.orEmpty()),
                DialogField(label = labels.value[1], initial = editing?.mapping.orEmpty()),
                DialogField(
                    label = labels.value[2],
                    initial = editing?.altMapping.orEmpty(),
                    allowEmpty = true,
                ),
            ),
            onDismiss = { dialogFor = null },
            onConfirm = { values ->
                val entry = PunctuationMapEntry(values[0].trim(), values[1], values[2])
                if (dialogIndex in entries.indices) entries[dialogIndex] = entry
                else entries.add(entry)
                dialogFor = null
                persist()
            },
        )
    }
}

/** Descriptions of the three columns, as reported by the fcitx config descriptor. */
private fun readLabels(raw: RawConfig): List<String> {
    val fallback = listOf(
        PunctuationManager.KEY,
        PunctuationManager.MAPPING,
        PunctuationManager.ALT_MAPPING,
    )
    val items = raw["desc"][PunctuationManager.MAP_ENTRY_CONFIG].subItems ?: return fallback
    val byName = items.associate { it.name to it["Description"].value }
    return fallback.map { byName[it]?.takeIf { text -> text.isNotBlank() } ?: it }
}
