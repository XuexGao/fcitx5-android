/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Key
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor
import org.fcitx.fcitx5.android.utils.config.ConfigType
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * Editor for a `ConfigList` / `ConfigEnumList` descriptor, shown as a dialog on top of the config
 * screen rather than as another route.
 *
 * Staying in the same screen matters: the list writes back into the [RawConfig] tree the parent
 * screen is editing, so a pushed screen (which unmounts the parent) could not persist the change.
 * Values are kept as the raw strings fcitx uses, and every edit is committed to [cfg] and handed to
 * [onChanged] immediately.
 */
@Composable
fun ListConfigDialog(
    title: String,
    descriptor: ConfigDescriptor<*, *>,
    cfg: RawConfig,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    val values = remember(descriptor, cfg) {
        mutableStateListOf<String>().apply {
            addAll(cfg.findByName(descriptor.name)?.subItems?.map { it.value }.orEmpty())
        }
    }
    var dialogFor by remember { mutableStateOf<Int?>(null) }
    var chooserFor by remember { mutableStateOf<Int?>(null) }

    val enumEntries = (descriptor as? ConfigDescriptor.ConfigEnumList)?.entries
    val enumLabels = (descriptor as? ConfigDescriptor.ConfigEnumList)?.entriesI18n
    val subtype = (descriptor as? ConfigDescriptor.ConfigList)?.let {
        (it.ty as? ConfigType.TyList)?.subtype
    }
    val fixedChoices: List<String>? = when {
        enumEntries != null -> enumEntries
        subtype == ConfigType.TyBool -> listOf("True", "False")
        else -> null
    }

    fun labelOf(value: String): String = when {
        enumEntries != null -> enumLabels?.getOrNull(enumEntries.indexOf(value)) ?: value
        subtype == ConfigType.TyKey ->
            runCatching { Key.parse(value).localizedString }.getOrDefault(value)

        else -> value
    }

    fun commit() {
        cfg.getOrCreate(descriptor.name).subItems =
            values.mapIndexed { index, value -> RawConfig(index.toString(), value) }.toTypedArray()
        onChanged()
    }

    fun put(index: Int, value: String) {
        if (index < values.size) values[index] = value else values.add(value)
        commit()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = title,
                            style = MiuixTheme.textStyles.title3,
                            color = colorScheme.onSurface,
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        if (values.isEmpty()) {
                            item {
                                NavRow(
                                    title = stringResource(R.string.none),
                                    enabled = false,
                                    onClick = {},
                                )
                            }
                        }
                        itemsIndexed(values) { index, value ->
                            NavRow(
                                title = labelOf(value),
                                endActions = {
                                    IconButton(
                                        enabled = index > 0,
                                        onClick = {
                                            val moved = values.removeAt(index)
                                            values.add(index - 1, moved)
                                            commit()
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowUpward,
                                            contentDescription = null,
                                            tint = colorScheme.onSurfaceVariantSummary,
                                        )
                                    }
                                    IconButton(
                                        enabled = index < values.lastIndex,
                                        onClick = {
                                            val moved = values.removeAt(index)
                                            values.add(index + 1, moved)
                                            commit()
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowDownward,
                                            contentDescription = null,
                                            tint = colorScheme.onSurfaceVariantSummary,
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (fixedChoices != null) chooserFor = index
                                            else dialogFor = index
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = stringResource(R.string.edit),
                                            tint = colorScheme.onSurfaceVariantSummary,
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            values.removeAt(index)
                                            commit()
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
                        item {
                            NavRow(
                                title = stringResource(R.string.add),
                                icon = Icons.Rounded.Add,
                                onClick = {
                                    if (fixedChoices != null) chooserFor = values.size
                                    else dialogFor = values.size
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    val chooserIndex = chooserFor
    if (chooserIndex != null && fixedChoices != null) {
        // Everything except the slot being edited is already taken.
        val taken = values.filterIndexed { index, _ -> index != chooserIndex }.toSet()
        val choices = fixedChoices.filterNot { it in taken }
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(choices.map(::labelOf).toTypedArray()) { _, which ->
                val value = choices[which]
                chooserFor = null
                put(chooserIndex, value)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> chooserFor = null }
            .show()
    }

    val index = dialogFor
    if (index != null) {
        EditFieldsDialog(
            title = title,
            fields = listOf(
                DialogField(
                    label = when (subtype) {
                        ConfigType.TyInt -> "integer"
                        ConfigType.TyKey -> "key"
                        else -> "value"
                    },
                    initial = values.getOrNull(index).orEmpty(),
                    numeric = subtype == ConfigType.TyInt,
                )
            ),
            onDismiss = { dialogFor = null },
            onConfirm = { input ->
                dialogFor = null
                put(index, input[0])
            },
        )
    }
}
