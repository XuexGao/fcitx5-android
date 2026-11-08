/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.Slider
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceEnum
import org.fcitx.fcitx5.android.ui.compose.LocalUiMode
import org.fcitx.fcitx5.android.ui.compose.UiMode
import org.fcitx.fcitx5.android.ui.compose.component.material.LocalListItemShapes
import org.fcitx.fcitx5.android.ui.compose.component.material.SegmentedDropdownItem
import org.fcitx.fcitx5.android.ui.compose.component.material.SegmentedListItem
import org.fcitx.fcitx5.android.ui.compose.component.material.SegmentedSwitchItem
import org.fcitx.fcitx5.android.ui.compose.component.material.uniformSegmentedShapes
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

/**
 * A card holding a group of rows, optionally preceded by a small section title.
 *
 * Both component styles live behind this one call: [LocalUiMode] decides whether the group is a
 * miuix card or the template's Material segmented list (title in the primary colour, each row in
 * its own rounded container).
 */
@Composable
fun SettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> Column(modifier = modifier.fillMaxWidth()) {
            if (title != null) SmallTitle(text = title)
            Card(modifier = Modifier.fillMaxWidth()) { content() }
        }

        UiMode.Material -> Column(modifier = modifier.fillMaxWidth()) {
            if (title != null) {
                MaterialText(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
                )
            }
            // Each row resolves its corners from this local; without it every row fell back to
            // material3's single-row shape (one big rounded block) and the group lost its seams.
            CompositionLocalProvider(LocalListItemShapes provides uniformSegmentedShapes()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) { content() }
            }
        }
    }
}

/** Standard navigable row, with optional trailing action buttons. */
@Composable
fun NavRow(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ArrowPreference(
            title = title,
            summary = summary,
            enabled = enabled,
            startAction = icon?.let { vector -> { LeadingIcon(vector, title) } },
            endActions = endActions ?: {},
            onClick = onClick,
        )

        UiMode.Material -> SegmentedListItem(
            onClick = onClick,
            enabled = enabled,
            headlineContent = { MaterialText(title) },
            supportingContent = summary?.let { { MaterialText(it) } },
            leadingContent = icon?.let { vector ->
                { MaterialIcon(vector, contentDescription = title) }
            },
            trailingContent = when {
                // SegmentedListItem wants a plain composable; the actions are RowScope receivers.
                endActions != null -> endActions?.let { actions -> { Row { actions() } } }
                onClick != null -> {
                    {
                        MaterialIcon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                        )
                    }
                }

                else -> null
            },
        )
    }
}

@Composable
fun LeadingIcon(icon: ImageVector, contentDescription: String? = null) {
    MiuixIcon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.padding(end = 6.dp),
        tint = MiuixTheme.colorScheme.onBackground,
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
    SwitchRowRaw(
        title = title,
        summary = summary,

        checked = state.value,
        enabled = enabled,
        onCheckedChange = {
            state.value = it
            onChanged?.invoke()
        },
    )
}

/** Switch row for a plain boolean that is not backed by a preference. */
@Composable
fun SwitchRowRaw(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> SwitchPreference(
            title = title,
            summary = summary,
            checked = checked,
            enabled = enabled,
            startAction = icon?.let { vector -> { LeadingIcon(vector, title) } },
            onCheckedChange = onCheckedChange,
        )

        UiMode.Material -> SegmentedSwitchItem(
            icon = icon,
            title = title,
            summary = summary,
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
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
    SliderRowRaw(
        title = title,
        summary = summary,

        value = state.value,
        range = range,
        steps = steps,
        valueText = valueText,
        enabled = enabled,
        onValueChange = { state.value = it },
        onValueChangeFinished = { onValueSettled?.invoke() },
    )
}

/** Slider row for a plain value that is not backed by a preference. */
@Composable
fun SliderRowRaw(
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    summary: String? = null,
    steps: Int = 0,
    valueText: (Int) -> String = { it.toString() },
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> SliderPreference(
            title = title,
            summary = summary,
            value = value.toFloat(),
            valueRange = range,
            steps = steps,
            enabled = enabled,
            valueText = valueText(value),
            startAction = icon?.let { vector -> { LeadingIcon(vector, title) } },
            onValueChange = { onValueChange(it.toInt()) },
            onValueChangeFinished = { onValueChangeFinished?.invoke() },
        )

        UiMode.Material -> Column(
            // Same card treatment as the other rows: without it a slider looked like a gap in the
            // group, because every neighbouring row is a filled segmented item.
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    MaterialIcon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                MaterialText(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                MaterialText(
                    text = valueText(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = range,
                steps = steps,
                enabled = enabled,
                onValueChangeFinished = onValueChangeFinished,
            )
            if (summary != null) {
                MaterialText(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

/** Checkbox row, optionally with trailing action buttons. */
@Composable
fun CheckboxRow(
    title: String,
    checked: Boolean,
    summary: String? = null,
    enabled: Boolean = true,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> CheckboxPreference(
            title = title,
            summary = summary,
            checked = checked,
            enabled = enabled,
            endActions = endActions ?: {},
            onCheckedChange = onCheckedChange,
        )

        UiMode.Material -> SegmentedListItem(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            headlineContent = { MaterialText(title) },
            supportingContent = summary?.let { { MaterialText(it) } },
            leadingContent = {
                Checkbox(
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = null,
                    modifier = Modifier.size(24.dp),
                )
            },
            trailingContent = endActions?.let { actions -> { Row { actions() } } },
        )
    }
}

/** Dropdown row with an explicit list of labels. */
@Composable
fun DropdownRow(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onSelectedIndexChange: (Int) -> Unit,
) {
    if (items.isEmpty()) return
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
    when (LocalUiMode.current) {
        UiMode.Miuix -> OverlayDropdownPreference(
            title = title,
            summary = summary,
            items = items,
            selectedIndex = safeIndex,
            enabled = enabled,
            startAction = icon?.let { vector -> { LeadingIcon(vector, title) } },
            onSelectedIndexChange = onSelectedIndexChange,
        )

        UiMode.Material -> SegmentedDropdownItem(
            icon = icon,
            title = title,
            summary = summary,
            items = items,
            enabled = enabled,
            selectedIndex = safeIndex,
            onItemSelected = onSelectedIndexChange,
        )
    }
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
    DropdownRow(
        title = title,
        summary = summary,
        icon = icon,
        items = labels,
        selectedIndex = values.indexOf(state.value).coerceAtLeast(0),
        enabled = enabled,
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
    DropdownRow(
        title = title,
        summary = summary,
        icon = icon,
        items = values,
        selectedIndex = state.value.coerceIn(0, (values.size - 1).coerceAtLeast(0)),
        enabled = enabled,
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
    DropdownRow(
        title = title,
        summary = summary,
        icon = icon,
        items = labels,
        selectedIndex = entries.indexOf(state.value).coerceAtLeast(0),
        enabled = enabled,
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
    DropdownRow(
        title = title,
        summary = summary,
        icon = icon,
        items = labels,
        selectedIndex = values.indexOf(state.value).coerceAtLeast(0),
        enabled = enabled,
        onSelectedIndexChange = { index -> state.value = values[index] },
    )
}

/** Exposed for callers that need the raw miuix inside margin. */
val DefaultInsideMargin: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
