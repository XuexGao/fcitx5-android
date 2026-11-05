/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.component

import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import org.fcitx.fcitx5.android.R

/**
 * One editable line of an [EditFieldsDialog].
 *
 * [initial] seeds the field, [allowEmpty] controls whether a blank value is accepted (an empty
 * value is reported back as-is), and the two flags only affect the soft keyboard.
 */
data class DialogField(
    val label: String,
    val initial: String = "",
    val allowEmpty: Boolean = false,
    val singleLine: Boolean = true,
    val numeric: Boolean = false,
    val lettersOnly: Boolean = false,
)

/**
 * Small "edit these fields" dialog, the Compose-era replacement for the old
 * `materialTextInput` + `onPositiveButtonClick` combination.
 *
 * The dialog is built once per distinct set of fields and kept in a [DisposableEffect], so
 * recomposition never re-shows it (which the naive `show()` inside composition would do). Blank
 * required fields keep the dialog open and mark the offending input, exactly like the old screens.
 */
@Composable
fun EditFieldsDialog(
    title: String,
    fields: List<DialogField>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    val currentDismiss by rememberUpdatedState(onDismiss)
    val currentConfirm by rememberUpdatedState(onConfirm)
    val key = fields.joinToString("\u0000") { "${it.label}\u0001${it.initial}\u0001${it.allowEmpty}" }

    DisposableEffect(key, title) {
        val inputs = mutableListOf<EditText>()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 24, 56, 0)
        }
        fields.forEach { field ->
            val input = EditText(context).apply {
                hint = field.label
                setText(field.initial)
                isSingleLine = field.singleLine
                if (!field.singleLine) {
                    maxLines = 8
                    inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE
                } else {
                    inputType = when {
                        field.numeric -> InputType.TYPE_CLASS_NUMBER or
                            InputType.TYPE_NUMBER_FLAG_SIGNED
                        field.lettersOnly -> InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                        else -> InputType.TYPE_CLASS_TEXT
                    }
                }
                if (field.initial.isNotEmpty()) setSelection(field.initial.length)
            }
            layout.addView(input)
            inputs += input
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener { currentDismiss() }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val values = inputs.map { it.text.toString() }
                var valid = true
                fields.forEachIndexed { index, field ->
                    val value = values[index]
                    val blank = if (field.singleLine) value.isBlank() else value.isEmpty()
                    if (blank && !field.allowEmpty) {
                        inputs[index].error =
                            context.getString(R.string._cannot_be_empty, field.label)
                        if (valid) inputs[index].requestFocus()
                        valid = false
                    } else {
                        inputs[index].error = null
                    }
                }
                if (valid) {
                    dialog.dismiss()
                    currentConfirm(values)
                }
            }
        }
        dialog.show()
        onDispose { dialog.dismiss() }
    }
}
