/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.component

import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.ui.compose.LocalUiMode
import org.fcitx.fcitx5.android.ui.compose.UiMode

/**
 * "Pick one of these" dialog, in both styles.
 *
 * Uses the platform list dialog for miuix (matching the rest of the miuix screens) and a Material 3
 * [AlertDialog] with radio rows otherwise.
 */
@Composable
fun PickerDialog(
    title: String,
    items: List<String>,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit,
    selectedIndex: Int = -1,
) {
    if (items.isEmpty()) return
    when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixPickerDialog(title, items, onDismiss, onSelected, selectedIndex)
        UiMode.Material -> MaterialPickerDialog(title, items, onDismiss, onSelected, selectedIndex)
    }
}

/**
 * Yes/no dialog in both styles, used where a screen has to confirm a destructive action.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String? = null,
    confirmLabel: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixConfirmDialog(title, message, onDismiss, onConfirm)
        UiMode.Material -> MaterialConfirmDialog(title, message, confirmLabel, onDismiss, onConfirm)
    }
}

@Composable
private fun MiuixConfirmDialog(
    title: String,
    message: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val context = LocalContext.current
    val currentDismiss by rememberUpdatedState(onDismiss)
    val currentConfirm by rememberUpdatedState(onConfirm)

    DisposableEffect(title, message) {
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> currentConfirm() }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener { currentDismiss() }
            .create()
        dialog.show()
        onDispose { dialog.dismiss() }
    }
}

@Composable
private fun MaterialConfirmDialog(
    title: String,
    message: String?,
    confirmLabel: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { MaterialText(title) },
        text = message?.let { text -> { MaterialText(text) } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                MaterialText(confirmLabel ?: stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { MaterialText(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun MiuixPickerDialog(
    title: String,
    items: List<String>,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit,
    selectedIndex: Int,
) {
    val context = LocalContext.current
    val currentDismiss by rememberUpdatedState(onDismiss)
    val currentSelected by rememberUpdatedState(onSelected)

    DisposableEffect(title, items) {
        val builder = AlertDialog.Builder(context).setTitle(title)
        if (selectedIndex >= 0) {
            builder.setSingleChoiceItems(items.toTypedArray(), selectedIndex) { _, which ->
                currentSelected(which)
            }
        } else {
            builder.setItems(items.toTypedArray()) { _, which -> currentSelected(which) }
        }
        val dialog = builder
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener { currentDismiss() }
            .create()
        dialog.show()
        onDispose { dialog.dismiss() }
    }
}

@Composable
private fun MaterialPickerDialog(
    title: String,
    items: List<String>,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit,
    selectedIndex: Int,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { MaterialText(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = index == selectedIndex,
                                role = Role.RadioButton,
                                onClick = { onSelected(index) },
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = null,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp),
                        )
                        MaterialText(
                            text = item,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { MaterialText(stringResource(android.R.string.cancel)) }
        },
    )
}
