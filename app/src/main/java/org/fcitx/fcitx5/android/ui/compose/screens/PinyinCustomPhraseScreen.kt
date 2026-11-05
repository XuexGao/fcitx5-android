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
import androidx.compose.material.icons.rounded.Translate
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.reloadPinyinCustomPhrase
import org.fcitx.fcitx5.android.data.pinyin.CustomPhraseManager
import org.fcitx.fcitx5.android.data.pinyin.customphrase.PinyinCustomPhrase
import org.fcitx.fcitx5.android.ui.compose.component.DialogField
import org.fcitx.fcitx5.android.ui.compose.component.EditFieldsDialog
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import kotlin.math.absoluteValue

private const val NEW_ENTRY = -1
private const val CHINESE_ADDONS_DOMAIN = "fcitx5-chinese-addons"
private const val KEY = "Key"
private const val ORDER = "Order"
private const val PHRASE = "Phrase"

/** One row's label is the serialized phrase, clipped like the old screen did. */
private fun previewOf(phrase: PinyinCustomPhrase): String {
    val text = phrase.serialize()
    val firstBreak = text.indexOf('\n')
    val end = minOf(if (firstBreak > 0) firstBreak else text.length, 20)
    return if (end == text.length) text else text.take(end) + "…"
}

/**
 * 拼音自定义短语: the `key,order=phrase` list libime keeps for the pinyin engine.
 *
 * The three column labels are translated by fcitx itself (`fcitx5-chinese-addons` domain) and fall
 * back to the English names when the connection is not ready. Editing, enabling and deleting write
 * through [CustomPhraseManager] and ask fcitx to reload; order is preserved because libime uses it
 * to rank identical keys.
 */
@Composable
fun PinyinCustomPhraseScreen(
    title: String,
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    viewModel: MainViewModel,
) {
    val entries = remember { mutableStateListOf<PinyinCustomPhrase>() }
    val labels = remember { mutableStateOf(listOf(KEY, ORDER, PHRASE)) }
    var loaded by remember { mutableStateOf(false) }
    var dialogFor by remember { mutableStateOf<Int?>(null) }

    fun persist() {
        val snapshot = entries.toTypedArray()
        FcitxApplication.getInstance().coroutineScope.launch {
            withContext(Dispatchers.IO) { CustomPhraseManager.save(snapshot) }
            runCatching { viewModel.fcitx.runOnReady { reloadPinyinCustomPhrase() } }
                .onFailure { Timber.w(it, "reloadPinyinCustomPhrase failed") }
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            val loadedEntries = withContext(Dispatchers.IO) {
                CustomPhraseManager.load()?.toList().orEmpty()
            }
            val translated = runCatching {
                viewModel.fcitx.runOnReady {
                    listOf(
                        translate(KEY, CHINESE_ADDONS_DOMAIN),
                        translate(ORDER, CHINESE_ADDONS_DOMAIN),
                        translate(PHRASE, CHINESE_ADDONS_DOMAIN),
                    )
                }
            }.getOrNull()
            loadedEntries to translated
        }.onSuccess { (items, translated) ->
            entries.clear()
            entries.addAll(items)
            if (translated != null && translated.all { it.isNotBlank() }) labels.value = translated
        }.onFailure { Timber.w(it, "Unable to load custom phrases") }
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
                    NavRow(title = stringResource(R.string.loading), icon = Icons.Rounded.Translate)
                }
            }
            return@PageScaffold
        }
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                entries.forEachIndexed { index, entry ->
                    CheckboxPreference(
                        title = previewOf(entry),
                        checked = entry.enabled,
                        endActions = {
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
                        onCheckedChange = { checked ->
                            entries[index] = entry.copyEnabled(checked)
                            persist()
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
                DialogField(
                    label = labels.value[0],
                    initial = editing?.key.orEmpty(),
                    lettersOnly = true,
                ),
                DialogField(
                    label = labels.value[1],
                    initial = editing?.order?.absoluteValue?.toString().orEmpty(),
                    numeric = true,
                    allowEmpty = true,
                ),
                DialogField(
                    label = labels.value[2],
                    initial = editing?.value.orEmpty(),
                    singleLine = false,
                ),
            ),
            onDismiss = { dialogFor = null },
            onConfirm = { values ->
                val entry = PinyinCustomPhrase(
                    key = values[0],
                    order = values[1].toIntOrNull() ?: 1,
                    value = values[2],
                )
                if (dialogIndex in entries.indices) entries[dialogIndex] = entry
                else entries.add(entry)
                dialogFor = null
                persist()
            },
        )
    }
}
