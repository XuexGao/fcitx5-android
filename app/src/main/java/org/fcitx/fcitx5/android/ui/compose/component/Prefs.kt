/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceEnum
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * Two-way binding between a [ManagedPreference] and Compose state.
 *
 * Existing settings keep living in the same `ManagedPreference` objects the old UI used, so
 * persistence, defaults and cross-component listeners behave exactly as before; this just makes
 * them observable from Compose.
 */
class PrefState<T : Any>(
    private val state: MutableState<T>,
    private val pref: ManagedPreference<T>,
) {
    var value: T
        get() = state.value
        set(newValue) {
            pref.setValue(newValue)
            state.value = newValue
        }
}

@Composable
fun <T : Any> rememberPref(pref: ManagedPreference<T>): PrefState<T> {
    val state = remember(pref) { mutableStateOf(pref.getValue()) }
    DisposableEffect(pref) {
        val listener = ManagedPreference.OnChangeListener<T> { _, value -> state.value = value }
        pref.registerOnChangeListener(listener)
        onDispose { pref.unregisterOnChangeListener(listener) }
    }
    return remember(pref) { PrefState(state, pref) }
}

/** A card holding a group of rows, optionally preceded by a small section title. */
@Composable
fun SettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) SmallTitle(text = title)
        Card(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

/** Standard navigable row. */
@Composable
fun NavRow(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    ArrowPreference(
        title = title,
        summary = summary,
        enabled = enabled,
        startAction = icon?.let { vector -> { LeadingIcon(vector, title) } },
        onClick = onClick,
    )
}

@Composable
fun LeadingIcon(icon: ImageVector, contentDescription: String? = null) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.padding(end = 6.dp),
        tint = colorScheme.onBackground,
    )
}

/** Switch row bound to a boolean preference. */
@Composable
fun SwitchRow(
    pref: ManagedPreference.PBool,
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onChanged: (() -> Unit)? = null,
) {
    val state = rememberPref(pref)
    SwitchPreference(
        title = title,
        summary = summary,
        checked = state.value,
        enabled = enabled,
        startAction = icon?.let { vector -> { LeadingIcon(vector, title) } },
        onCheckedChange = {
            state.value = it
            onChanged?.invoke()
        },
    )
}

/** Slider row bound to an int preference. */
@Composable
fun SliderRow(
    pref: ManagedPreference.PInt,
    title: String,
    range: ClosedFloatingPointRange<Float>,
    summary: String? = null,
    steps: Int = 0,
    valueText: (Int) -> String = { it.toString() },
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onValueSettled: (() -> Unit)? = null,
) {
    val state = rememberPref(pref)
    SliderPreference(
        title = title,
        summary = summary,
        value = state.value.toFloat(),
        valueRange = range,
        steps = steps,
        enabled = enabled,
        valueText = valueText(state.value),
        startAction = icon?.let { vector -> { LeadingIcon(vector, title) } },
        onValueChange = { state.value = it.toInt() },
        onValueChangeFinished = { onValueSettled?.invoke() },
    )
}

/** Dropdown row bound to a string preference, with an explicit list of allowed values. */
@Composable
fun ChoiceRow(
    pref: ManagedPreference.PString,
    title: String,
    values: List<String>,
    summary: String? = null,
    labels: List<String> = values,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val state = rememberPref(pref)
    OverlayDropdownPreference(
        title = title,
        summary = summary,
        items = labels,
        selectedIndex = values.indexOf(state.value).coerceAtLeast(0),
        enabled = enabled,
        startAction = icon?.let { vector -> { LeadingIcon(vector, title) } },
        onSelectedIndexChange = { index -> state.value = values[index] },
    )
}

/** Dropdown row bound to an int preference where the value is the index into [values]. */
@Composable
fun IndexChoiceRow(
    pref: ManagedPreference.PInt,
    title: String,
    values: List<String>,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val state = rememberPref(pref)
    OverlayDropdownPreference(
        title = title,
        summary = summary,
        items = values,
        selectedIndex = state.value.coerceIn(0, (values.size - 1).coerceAtLeast(0)),
        enabled = enabled,
        startAction = icon?.let { vector -> { LeadingIcon(vector, title) } },
        onSelectedIndexChange = { index -> state.value = index },
    )
}

/**
 * Dropdown row bound to an enum preference. Labels come from the enum's own string resource, so
 * translations keep working.
 */
@Composable
fun <T> EnumChoiceRow(
    pref: ManagedPreference.PStringLike<T>,
    title: String,
    entries: List<T>,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) where T : Enum<T>, T : ManagedPreferenceEnum {
    val state = rememberPref(pref)
    val labels = entries.map { stringResource(it.stringRes) }
    OverlayDropdownPreference(
        title = title,
        summary = summary,
        items = labels,
        selectedIndex = entries.indexOf(state.value).coerceAtLeast(0),
        enabled = enabled,
        startAction = icon?.let { vector -> { LeadingIcon(vector, title) } },
        onSelectedIndexChange = { index -> state.value = entries[index] },
    )
}

/**
 * Dropdown row for a `PStringLike<*>` preference whose concrete enum type is only known at runtime
 * (used by the generic provider renderer). [values] and [labels] must line up.
 */
@Composable
fun StringLikeRow(
    pref: ManagedPreference.PStringLike<Any>,
    title: String,
    values: List<Any>,
    labels: List<String>,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val state = rememberPref(pref)
    OverlayDropdownPreference(
        title = title,
        summary = summary,
        items = labels,
        selectedIndex = values.indexOf(state.value).coerceAtLeast(0),
        enabled = enabled,
        startAction = icon?.let { vector -> { LeadingIcon(vector, title) } },
        onSelectedIndexChange = { index -> state.value = values[index] },
    )
}

/** Exposed for callers that need the raw miuix inside margin. */
val DefaultInsideMargin: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
