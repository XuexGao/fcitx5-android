/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.config.ConfigProviders
import org.fcitx.fcitx5.android.input.config.UserConfigFiles
import org.fcitx.fcitx5.android.ui.compose.component.PickerDialog
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.compose.component.rememberPref
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.ui.main.settings.behavior.ButtonsCustomizerActivity
import org.fcitx.fcitx5.android.ui.main.settings.behavior.FontsetEditorActivity
import org.fcitx.fcitx5.android.ui.main.settings.behavior.PopupEditorActivity
import org.fcitx.fcitx5.android.ui.main.settings.behavior.SplitKeyboardCalibrationActivity
import org.fcitx.fcitx5.android.ui.main.settings.behavior.TextKeyboardLayoutEditorActivity
import org.fcitx.fcitx5.android.ui.main.settings.behavior.data.LayoutDataManager
import org.fcitx.fcitx5.android.ui.main.settings.behavior.webeditor.ImeWebEditorBridgeServer
import org.fcitx.fcitx5.android.utils.toast
import kotlin.math.max

/** Keyboard settings groups; the order is the order they appear in the picker. */
const val KEYBOARD_GROUP_LAYOUT = 0
const val KEYBOARD_GROUP_BEHAVIOR = 1
const val KEYBOARD_GROUP_FEEDBACK = 2
const val KEYBOARD_GROUP_TOOLBAR = 3
const val KEYBOARD_GROUP_EDITORS = 4

private val KEYBOARD_KEYS_BY_GROUP: Map<Int, Set<String>> = mapOf(
    KEYBOARD_GROUP_LAYOUT to setOf(
        "keyboard_height_percent", "keyboard_side_padding",
        "keyboard_bottom_padding", "expand_keypress_area",
        "split_keyboard_enabled", "split_keyboard_use_landscape_layout",
    ),
    KEYBOARD_GROUP_BEHAVIOR to setOf(
        "popup_on_key_press", "keyboard_long_press_delay",
        "swipe_symbol_behavior", "keep_keyboard_letters_uppercase",
        "reset_keyboard_on_focus_change",
        "space_long_press_behavior", "space_key_label_mode",
        "space_swipe_move_cursor", "show_lang_switch_key",
        "lang_switch_key_behavior",
    ),
    KEYBOARD_GROUP_FEEDBACK to setOf(
        "haptic_on_keypress", "haptic_on_keyup", "haptic_on_repeat",
        "button_vibration_press_milliseconds", "button_vibration_press_amplitude",
        "sound_on_keypress", "button_sound_volume",
    ),
    KEYBOARD_GROUP_TOOLBAR to setOf(
        "expand_toolbar_by_default", "toolbar_manually_toggled",
        "inline_suggestions", "toolbar_num_row_on_password",
        "horizontal_candidate_style", "expanded_candidate_style",
        "expanded_candidate_grid_span_count_portrait",
        "show_voice_input_button", "preferred_voice_input",
    ),
    KEYBOARD_GROUP_EDITORS to emptySet(),
)

private fun groupTitleRes(group: Int): Int = when (group) {
    KEYBOARD_GROUP_LAYOUT -> R.string.keyboard_category_layout
    KEYBOARD_GROUP_BEHAVIOR -> R.string.keyboard_category_behavior
    KEYBOARD_GROUP_FEEDBACK -> R.string.keyboard_category_feedback
    KEYBOARD_GROUP_TOOLBAR -> R.string.keyboard_category_toolbar
    KEYBOARD_GROUP_EDITORS -> R.string.keyboard_category_editors
    else -> throw IllegalArgumentException("Unknown keyboard settings group: $group")
}

/**
 * 虚拟键盘: entry point of the keyboard settings, split into five groups.
 *
 * The bulk of the options already live on 界面 / 功能; this page exists for the ones that need the
 * full width of their own screen, plus the editor entries (font set, popup preset, keyboard
 * definition, online editor) that open their own activities.
 */
@Composable
fun KeyboardSettingsScreen(
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    onOpenRoute: (SettingsRoute) -> Unit,
) {
    val bridgeRunning = ImeWebEditorBridgeServer.currentSession() != null
    PageScaffold(
        title = stringResource(R.string.virtual_keyboard),
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
    ) {
        item {
            SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                NavRow(
                    title = stringResource(R.string.keyboard_category_layout),
                    icon = Icons.Rounded.Straighten,
                    onClick = { onOpenRoute(SettingsRoute.KeyboardGroup(KEYBOARD_GROUP_LAYOUT)) },
                )
                NavRow(
                    title = stringResource(R.string.keyboard_category_behavior),
                    icon = Icons.Rounded.Keyboard,
                    onClick = { onOpenRoute(SettingsRoute.KeyboardGroup(KEYBOARD_GROUP_BEHAVIOR)) },
                )
                NavRow(
                    title = stringResource(R.string.keyboard_category_feedback),
                    icon = Icons.Rounded.Build,
                    onClick = { onOpenRoute(SettingsRoute.KeyboardGroup(KEYBOARD_GROUP_FEEDBACK)) },
                )
                NavRow(
                    title = stringResource(R.string.keyboard_category_toolbar),
                    icon = Icons.Rounded.Build,
                    onClick = { onOpenRoute(SettingsRoute.KeyboardGroup(KEYBOARD_GROUP_TOOLBAR)) },
                )
                NavRow(
                    title = stringResource(R.string.keyboard_category_editors),
                    summary = if (bridgeRunning) {
                        stringResource(R.string.web_editor_bridge_running_indicator)
                    } else {
                        null
                    },
                    icon = Icons.Rounded.Public,
                    onClick = { onOpenRoute(SettingsRoute.KeyboardGroup(KEYBOARD_GROUP_EDITORS)) },
                )
            }
        }
    }
}

/**
 * One keyboard settings group: the managed rows belonging to it, followed by the tool entries the
 * old fragment appended by hand.
 */
@Composable
fun KeyboardGroupScreen(
    group: Int,
    bottomInnerPadding: Dp,
    onBack: () -> Unit,
    onOpenActivity: (Class<*>) -> Unit,
) {
    val keys = KEYBOARD_KEYS_BY_GROUP[group].orEmpty()
    ManagedPrefsScreen(
        title = stringResource(groupTitleRes(group)),
        provider = AppPrefs.getInstance().keyboard,
        bottomInnerPadding = bottomInnerPadding,
        onBack = onBack,
        filter = { it in keys },
        footer = {
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp)) {
                    when (group) {
                        KEYBOARD_GROUP_LAYOUT -> LayoutTools(onOpenActivity)
                        KEYBOARD_GROUP_TOOLBAR -> ToolbarTools(onOpenActivity)
                        KEYBOARD_GROUP_EDITORS -> EditorsTools(onOpenActivity)
                    }
                }
            }
        },
    )
}

@Composable
private fun LayoutTools(onOpenActivity: (Class<*>) -> Unit) {
    // Bound to the preference so the row follows the split keyboard switch without leaving the page.
    val splitEnabled = rememberPref(AppPrefs.getInstance().keyboard.splitKeyboardEnabled)
    NavRow(
        title = stringResource(R.string.split_keyboard_calibration_title),
        icon = Icons.Rounded.CenterFocusStrong,
        enabled = splitEnabled.value,
        onClick = { onOpenActivity(SplitKeyboardCalibrationActivity::class.java) },
    )
}

@Composable
private fun ToolbarTools(onOpenActivity: (Class<*>) -> Unit) {
    NavRow(
        title = stringResource(R.string.edit_buttons),
        icon = Icons.Rounded.Tune,
        summary = stringResource(R.string.edit_buttons_summary),
        onClick = { onOpenActivity(ButtonsCustomizerActivity::class.java) },
    )
}

@Composable
private fun EditorsTools(onOpenActivity: (Class<*>) -> Unit) {
    val context = LocalContext.current
    val prefs = AppPrefs.getInstance().keyboard
    // Bound through the preference objects themselves, so the summaries follow every change.
    val layoutProfile = rememberPref(prefs.textKeyboardLayoutProfile)
    val numericOverride = rememberPref(prefs.numericLayoutOverride)
    var bridgeRevision by remember { mutableIntStateOf(0) }
    var layoutPicker by remember { mutableStateOf(false) }
    var numericPicker by remember { mutableStateOf(false) }
    var bridgeDialog by remember { mutableStateOf(false) }
    val bridgeSession = remember(bridgeRevision) { ImeWebEditorBridgeServer.currentSession() }

    NavRow(
        title = stringResource(R.string.edit_fontset),
        icon = Icons.Rounded.TextFields,
        onClick = { onOpenActivity(FontsetEditorActivity::class.java) },
    )
    NavRow(
        title = stringResource(R.string.edit_popup_preset),
        icon = Icons.Rounded.GridView,
        onClick = { onOpenActivity(PopupEditorActivity::class.java) },
    )
    NavRow(
        title = stringResource(R.string.edit_text_keyboard_layout),
        icon = Icons.Rounded.Edit,
        onClick = { onOpenActivity(TextKeyboardLayoutEditorActivity::class.java) },
    )
    NavRow(
        title = stringResource(R.string.text_keyboard_layout_file_select_title),
        icon = Icons.Rounded.FolderOpen,
        summary = stringResource(
            R.string.text_keyboard_layout_file_select_summary,
            displayProfile(layoutProfile.value),
        ),
        onClick = { layoutPicker = true },
    )
    NavRow(
        title = stringResource(R.string.numeric_layout_override_title),
        icon = Icons.Rounded.Dialpad,
        summary = numericOverrideSummary(context, numericOverride.value),
        onClick = { numericPicker = true },
    )
    NavRow(
        title = stringResource(R.string.web_editor_bridge_title),
        icon = Icons.Rounded.Public,
        summary = bridgeSession?.let { session ->
            context.getString(
                R.string.web_editor_bridge_running_status,
                session.host,
                session.port,
            )
        } ?: stringResource(R.string.web_editor_bridge_summary),
        onClick = {
            if (bridgeSession != null) {
                bridgeDialog = true
            } else {
                runCatching { ImeWebEditorBridgeServer.start() }
                    .onSuccess {
                        bridgeRevision++
                        bridgeDialog = true
                    }
                    .onFailure {
                        context.toast(
                            context.getString(
                                R.string.web_editor_bridge_start_failed,
                                it.localizedMessage.orEmpty(),
                            )
                        )
                    }
            }
        },
    )

    if (layoutPicker) {
        val profiles = UserConfigFiles.listTextKeyboardLayoutProfiles().toMutableList()
        val current = layoutProfile.value
        if (current !in profiles) profiles += current
        val sorted = profiles.distinct().sortedWith(
            compareBy({ it != UserConfigFiles.DEFAULT_TEXT_KEYBOARD_LAYOUT_PROFILE }, { it })
        )
        PickerDialog(
            title = context.getString(R.string.text_keyboard_layout_file_select_title),
            items = sorted.map { displayProfile(it) },
            selectedIndex = sorted.indexOf(current).coerceAtLeast(0),
            onDismiss = { layoutPicker = false },
            onSelected = { which ->
                val selected = sorted.getOrNull(which) ?: return@PickerDialog
                layoutProfile.value = selected
                ConfigProviders.provider = ConfigProviders.provider
                layoutPicker = false
            },
        )
    }

    if (numericPicker) {
        val builtin = context.getString(R.string.numeric_layout_override_builtin)
        val layers = collectLayoutLayers(context)
        val items = listOf(builtin) + layers
        val current = numericOverride.value.trim()
        PickerDialog(
            title = context.getString(R.string.numeric_layout_override_title),
            items = items,
            selectedIndex = if (current.isEmpty()) 0 else max(0, items.indexOf(current)),
            onDismiss = { numericPicker = false },
            onSelected = { which ->
                numericOverride.value = if (which == 0) "" else items[which]
                numericPicker = false
            },
        )
    }

    if (bridgeDialog && bridgeSession != null) {
        // Three actions (open / stop / cancel) do not fit the two-button confirm dialog, so the two
        // real actions are offered as a picker.
        PickerDialog(
            title = context.getString(R.string.web_editor_bridge_title),
            items = listOf(
                context.getString(R.string.web_editor_bridge_open),
                context.getString(R.string.web_editor_bridge_stop),
            ),
            onDismiss = { bridgeDialog = false },
            onSelected = { which ->
                bridgeDialog = false
                when (which) {
                    0 -> runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(bridgeSession.editorUrl))
                        )
                    }

                    else -> {
                        ImeWebEditorBridgeServer.stop()
                        bridgeRevision++
                    }
                }
            },
        )
    }
}

private fun displayProfile(raw: String): String {
    val profile = UserConfigFiles.normalizeTextKeyboardLayoutProfile(raw)
        ?: UserConfigFiles.DEFAULT_TEXT_KEYBOARD_LAYOUT_PROFILE
    return if (profile == UserConfigFiles.DEFAULT_TEXT_KEYBOARD_LAYOUT_PROFILE) {
        org.fcitx.fcitx5.android.utils.appContext.getString(R.string.default_)
    } else {
        profile
    }
}

private fun numericOverrideSummary(
    context: android.content.Context,
    numericOverride: String,
): String {
    val value = numericOverride.trim()
    if (value.isEmpty()) return context.getString(R.string.numeric_layout_override_builtin)
    return if (value in collectLayoutLayers(context)) value
    else context.getString(R.string.numeric_layout_override_unavailable, value)
}

/** Layout layers of the active keyboard definition, using the same parser the editor uses. */
private fun collectLayoutLayers(context: android.content.Context): List<String> {
    val file = ConfigProviders.provider.textKeyboardLayoutFile() ?: return emptyList()
    val text = runCatching { file.readText() }.getOrNull() ?: return emptyList()
    val parsed = runCatching {
        LayoutDataManager(context).parseJsonText(text, file.name, fallbackToDefault = false)
    }.getOrNull() ?: return emptyList()
    return parsed.keys.sorted()
}
