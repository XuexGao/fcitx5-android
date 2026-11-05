/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
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
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.FcitxApplication
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.utils.toast
import timber.log.Timber
import top.yukonga.miuix.kmp.preference.CheckboxPreference

/** One selectable entry of an add-on's multi-select option. */
private data class MultiItem(val selected: Boolean, val id: String, val label: String)

/**
 * 多选列表 for add-ons that declare a `MultiSelect` external: a `\t`-separated payload of
 * `selected / id / label` lines.
 *
 * Nothing is written while the user toggles boxes; the payload is saved when the screen is left, so
 * the "at least N items" rule the descriptor can carry is checked once, exactly like before.
 */
@Composable
fun MultiSelectScreen(
    title: String,
    addon: String,
    path: String,
    option: String,
    min: Int,
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<MultiItem>() }
    var loaded by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }

    LaunchedEffect(addon, path, option) {
        runCatching {
            viewModel.fcitx.runOnReady {
                val raw = getAddonSubConfig(addon, path)
                val cfg = raw.findByName("cfg") ?: raw
                val payload = cfg.findByName(option)?.value.orEmpty()
                Timber.i(
                    "MultiSelect load addon=%s path=%s option=%s rawItems=%s payloadLen=%d",
                    addon,
                    path,
                    option,
                    cfg.subItems?.joinToString(",") { it.name },
                    payload.length,
                )
                parseItems(payload)
            }
        }.onSuccess {
            items.clear()
            items.addAll(it)
        }.onFailure { Timber.w(it, "Unable to load multi-select payload") }
        loaded = true
    }

    DisposableEffect(addon, path, option) {
        onDispose {
            if (!dirty) return@onDispose
            val selected = items.count { it.selected }
            if (selected < min) {
                context.toast(context.getString(R.string.generic_multiselect_min, min))
                return@onDispose
            }
            val payload = encodeItems(items.toList())
            FcitxApplication.getInstance().coroutineScope.launch {
                runCatching {
                    viewModel.fcitx.runOnReady {
                        setAddonSubConfig(addon, path, RawConfig(arrayOf(RawConfig(option, payload))))
                    }
                }.onFailure { Timber.w(it, "Unable to save multi-select payload") }
            }
        }
    }

    PageScaffold(
        title = title,
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        if (!loaded) {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    NavRow(title = stringResource(R.string.loading), icon = Icons.Rounded.CheckBox)
                }
            }
            return@PageScaffold
        }
        item {
            SettingsGroup(
                title = if (min > 0) stringResource(R.string.generic_multiselect_min, min) else null,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                items.forEachIndexed { index, item ->
                    CheckboxPreference(
                        title = item.label,
                        checked = item.selected,
                        onCheckedChange = { checked ->
                            items[index] = item.copy(selected = checked)
                            dirty = true
                        },
                    )
                }
            }
        }
    }
}

private fun parseItems(text: String): List<MultiItem> {
    if (text.isBlank()) return emptyList()
    return text.lineSequence().mapNotNull { line ->
        if (line.isBlank()) return@mapNotNull null
        val parts = line.split('\t', limit = 3)
        if (parts.size < 3) return@mapNotNull null
        MultiItem(
            selected = parts[0] == "1",
            id = parts[1],
            label = sanitizeLabel(parts[2]),
        )
    }.toList()
}

private fun sanitizeLabel(raw: String): String {
    val cleaned = raw.replace('\t', ' ').replace('|', ' ').trim()
    return cleaned.ifEmpty { raw.trim() }
}

private fun encodeItems(items: List<MultiItem>): String =
    items.joinToString("\n") { "${if (it.selected) "1" else "0"}\t${it.id}\t${it.label}" }
