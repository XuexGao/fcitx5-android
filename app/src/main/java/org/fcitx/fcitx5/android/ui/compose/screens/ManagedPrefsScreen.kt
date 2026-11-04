/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceProvider
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceUi
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.compose.component.SliderRow
import org.fcitx.fcitx5.android.ui.compose.component.SwitchRow
import org.fcitx.fcitx5.android.ui.compose.component.rememberPref
import org.fcitx.fcitx5.android.ui.compose.component.StringLikeRow
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference

/**
 * Renders a [ManagedPreferenceProvider] as a miuix settings page.
 *
 * Every existing preference screen that was built from a provider (keyboard, candidates, clipboard,
 * symbols, advanced, theme settings, …) becomes a Compose screen without hand-writing each row:
 * the provider already declares its rows, their titles, ranges and enable-conditions, and this maps
 * those declarations onto miuix components. Persistence is untouched because the same
 * [ManagedPreference] objects are read and written.
 */
@Composable
fun ManagedPrefsScreen(
    title: String,
    provider: ManagedPreferenceProvider,
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    header: (LazyListScope.() -> Unit)? = null,
    footer: (LazyListScope.() -> Unit)? = null,
) {
    PageScaffold(
        title = title,
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        header?.invoke(this)
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                ProviderRows(provider)
            }
        }
        footer?.invoke(this)
    }
}

/** All rows declared by [provider], in declaration order. */
@Composable
fun ProviderRows(provider: ManagedPreferenceProvider) {
    provider.managedPreferencesUi.forEach { ui ->
        val pref = provider.managedPreferences[ui.key] ?: return@forEach
        ManagedPrefRow(provider, ui, pref)
    }
}

@Composable
private fun ManagedPrefRow(
    provider: ManagedPreferenceProvider,
    ui: ManagedPreferenceUi<*>,
    pref: ManagedPreference<*>,
) {
    @Suppress("UNCHECKED_CAST")
    when (ui) {
        is ManagedPreferenceUi.Switch -> {
            val typed = pref as? ManagedPreference.PBool ?: return
            SwitchRow(
                pref = typed,
                title = stringResource(ui.title),
                summary = ui.summary?.let { stringResource(it) },
                enabled = ui.isEnabled(),
            )
        }

        is ManagedPreferenceUi.SeekBarInt -> {
            val typed = pref as? ManagedPreference.PInt ?: return
            SliderRow(
                pref = typed,
                title = stringResource(ui.title),
                range = ui.min.toFloat()..ui.max.toFloat(),
                steps = stepsFor(ui.min, ui.max, ui.step),
                valueText = { value -> "$value${ui.unit}" },
                enabled = ui.isEnabled(),
            )
        }

        is ManagedPreferenceUi.EditTextInt -> {
            val typed = pref as? ManagedPreference.PInt ?: return
            SliderRow(
                pref = typed,
                title = stringResource(ui.title),
                range = ui.min.toFloat()..ui.max.toFloat(),
                valueText = { value -> "$value${ui.unit}" },
                enabled = ui.isEnabled(),
            )
        }

        is ManagedPreferenceUi.TwinSeekBarInt -> {
            val primary = pref as? ManagedPreference.PInt ?: return
            val secondary = provider.managedPreferences[ui.secondaryKey]
                as? ManagedPreference.PInt ?: return
            SliderRow(
                pref = primary,
                title = stringResource(ui.title) + " · " + stringResource(ui.label),
                range = ui.min.toFloat()..ui.max.toFloat(),
                steps = stepsFor(ui.min, ui.max, ui.step),
                valueText = { value -> "$value${ui.unit}" },
                enabled = ui.isEnabled(),
            )
            SliderRow(
                pref = secondary,
                title = stringResource(ui.title) + " · " + stringResource(ui.secondaryLabel),
                range = ui.min.toFloat()..ui.max.toFloat(),
                steps = stepsFor(ui.min, ui.max, ui.step),
                valueText = { value -> "$value${ui.unit}" },
                enabled = ui.isEnabled(),
            )
        }

        is ManagedPreferenceUi.StringList<*> -> {
            @Suppress("UNCHECKED_CAST")
            val typed = pref as? ManagedPreference.PStringLike<Any> ?: return
            val labels = ui.entryLabels.map { stringResource(it) }
            @Suppress("UNCHECKED_CAST")
            val values = ui.entryValues as List<Any>
            StringLikeRow(
                pref = typed,
                title = stringResource(ui.title),
                values = values,
                labels = labels,
                enabled = ui.isEnabled(),
            )
        }

        is ManagedPreferenceUi.VoiceInputList -> {
            VoiceInputRow(
                pref = pref as? ManagedPreference.PString ?: return,
                title = stringResource(ui.title),
                enabled = ui.isEnabled(),
            )
        }

        is ManagedPreferenceUi.StringSet -> {
            val typed = pref as? ManagedPreference.PStringSet ?: return
            StringSetRow(
                pref = typed,
                title = stringResource(ui.title),
                summary = ui.summary?.let { stringResource(it) },
                enabled = ui.isEnabled(),
            )
        }

        else -> Unit
    }
}

/** miuix counts intermediate steps, not divisions. */
private fun stepsFor(min: Int, max: Int, step: Int): Int {
    if (step <= 1) return 0
    val divisions = (max - min) / step
    return (divisions - 1).coerceAtLeast(0)
}

/** Voice input provider picker, mirroring the ListPreference the old screen built. */
@Composable
private fun VoiceInputRow(
    pref: ManagedPreference.PString,
    title: String,
    enabled: Boolean,
) {
    val context = LocalContext.current
    val state = rememberPref(pref)
    val entries = remember {
        val system = org.fcitx.fcitx5.android.utils.InputMethodUtil.listVoiceInputMethods()
        val plugins = org.fcitx.fcitx5.android.input.voice.VoiceInputProviderManager.listProviders(context)
        val values = mutableListOf("")
        val labels = mutableListOf(context.getString(R.string.system_default))
        system.forEach {
            values += it.first.id
            labels += it.first.loadLabel(context.packageManager).toString()
        }
        plugins.forEach {
            values += it.id
            labels += it.label.toString()
        }
        values to labels
    }
    OverlayDropdownPreference(
        title = title,
        items = entries.second,
        selectedIndex = entries.first.indexOf(state.value).coerceAtLeast(0),
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        onSelectedIndexChange = { index -> state.value = entries.first[index] },
    )
}

/** Multi-line comma separated editor for set-valued preferences. */
@Composable
private fun StringSetRow(
    pref: ManagedPreference.PStringSet,
    title: String,
    summary: String?,
    enabled: Boolean,
) {
    val context = LocalContext.current
    val state = rememberPref(pref)
    var showDialog by remember { mutableStateOf(false) }
    val rendered = state.value.joinToString(", ").ifEmpty { "—" }
    top.yukonga.miuix.kmp.preference.ArrowPreference(
        title = title,
        summary = summary ?: rendered,
        enabled = enabled,
        onClick = { showDialog = true },
    )
    if (showDialog) {
        val input = remember { mutableStateOf(state.value.joinToString("\n")) }
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setView(
                android.widget.EditText(context).apply {
                    setText(input.value)
                    isSingleLine = false
                    minLines = 3
                    setPadding(48, 24, 48, 24)
                }.also { field ->
                    field.addTextChangedListener(
                        object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                            override fun afterTextChanged(s: android.text.Editable?) {
                                input.value = s?.toString().orEmpty()
                            }
                        }
                    )
                }
            )
            .setPositiveButton(android.R.string.ok) { _, _ ->
                state.value = input.value
                    .split(',', '\n')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
                showDialog = false
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> showDialog = false }
            .show()
    }
}
