/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.pages

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Paste
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.ui.compose.component.EnumChoiceRow
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.compose.component.SliderRow
import org.fcitx.fcitx5.android.ui.compose.component.SwitchRow
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute

/**
 * 功能: everything the input method *does* — engines, schemes, text tools, typing behaviour,
 * feedback, clipboard and plugins.
 */
@Composable
fun FeaturesPage(
    bottomInnerPadding: Dp,
    onOpenRoute: (SettingsRoute) -> Unit,
    onOpenActivity: (Class<*>) -> Unit,
) {
    PageScaffold(
        title = stringResource(R.string.ksu_tab_features),
        bottomInnerPadding = bottomInnerPadding,
    ) {
        inputSection(onOpenRoute)
        textToolsSection(onOpenRoute)
        typingBehaviourSection()
        feedbackSection()
        clipboardSection(onOpenRoute)
        pluginsSection(onOpenRoute)
    }
}

private fun LazyListScope.inputSection(onOpenRoute: (SettingsRoute) -> Unit) {
    item {
        SettingsGroup(
            title = stringResource(R.string.ksu_features_input),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            NavRow(
                title = stringResource(R.string.input_methods),
                icon = Icons.Rounded.Language,
                onClick = { onOpenRoute(SettingsRoute.InputMethodList) },
            )
            NavRow(
                title = stringResource(R.string.ksu_rime_title),
                summary = stringResource(R.string.ksu_rime_default_schema_summary),
                icon = Icons.Rounded.Spellcheck,
                onClick = { onOpenRoute(SettingsRoute.RimeSchemas) },
            )
            NavRow(
                title = stringResource(R.string.addons),
                icon = Icons.Rounded.Extension,
                onClick = { onOpenRoute(SettingsRoute.AddonList) },
            )
            NavRow(
                title = stringResource(R.string.global_options),
                icon = Icons.Rounded.Tune,
                onClick = { onOpenRoute(SettingsRoute.GlobalConfig) },
            )
        }
    }
}

private fun LazyListScope.textToolsSection(onOpenRoute: (SettingsRoute) -> Unit) {
    item {
        SettingsGroup(
            title = stringResource(R.string.ksu_features_tools),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            NavRow(
                title = stringResource(R.string.quickphrase_editor),
                icon = Icons.Rounded.FormatQuote,
                onClick = { onOpenRoute(SettingsRoute.QuickPhraseList) },
            )
            NavRow(
                title = stringResource(R.string.ksu_pinyin_custom_phrase),
                icon = Icons.Rounded.TextFields,
                onClick = { onOpenRoute(SettingsRoute.PinyinCustomPhrase) },
            )
            NavRow(
                title = stringResource(R.string.table_im),
                icon = Icons.Rounded.LibraryBooks,
                onClick = { onOpenRoute(SettingsRoute.TableInputMethods) },
            )
            NavRow(
                title = stringResource(R.string.emoji_and_symbols),
                icon = Icons.Rounded.EmojiEmotions,
                onClick = { onOpenRoute(SettingsRoute.Symbol) },
            )
        }
    }
}

private fun LazyListScope.typingBehaviourSection() {
    item {
        val keyboard = AppPrefs.getInstance().keyboard
        SettingsGroup(
            title = stringResource(R.string.ksu_features_behaviour),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            SwitchRow(
                pref = keyboard.spaceSwipeMoveCursor,
                title = stringResource(R.string.space_swipe_move_cursor),
                icon = Icons.Rounded.SwapHoriz,
            )
            EnumChoiceRow(
                pref = keyboard.spaceKeyLongPressBehavior,
                title = stringResource(R.string.space_long_press_behavior),
            )
            EnumChoiceRow(
                pref = keyboard.spaceKeyLabelMode,
                title = stringResource(R.string.space_key_label_mode),
            )
            SwitchRow(
                pref = keyboard.showLangSwitchKey,
                title = stringResource(R.string.show_lang_switch_key),
                icon = Icons.Rounded.Language,
            )
            EnumChoiceRow(
                pref = keyboard.langSwitchKeyBehavior,
                title = stringResource(R.string.lang_switch_key_behavior),
                enabled = keyboard.showLangSwitchKey.getValue(),
            )
            EnumChoiceRow(
                pref = keyboard.swipeSymbolDirection,
                title = stringResource(R.string.swipe_symbol_behavior),
            )
            SliderRow(
                pref = keyboard.longPressDelay,
                title = stringResource(R.string.keyboard_long_press_delay),
                range = 100f..1000f,
                steps = 17,
                valueText = { "$it ms" },
                icon = Icons.Rounded.Timer,
            )
            SwitchRow(
                pref = keyboard.keepLettersUppercase,
                title = stringResource(R.string.keep_keyboard_letters_uppercase),
            )
            SwitchRow(
                pref = keyboard.popupOnKeyPress,
                title = stringResource(R.string.popup_on_key_press),
            )
            SwitchRow(
                pref = keyboard.expandKeypressArea,
                title = stringResource(R.string.expand_keypress_area),
                icon = Icons.Rounded.TouchApp,
            )
            SwitchRow(
                pref = keyboard.focusChangeResetKeyboard,
                title = stringResource(R.string.reset_keyboard_on_focus_change),
            )
            SwitchRow(
                pref = keyboard.inlineSuggestions,
                title = stringResource(R.string.inline_suggestions),
            )
            SwitchRow(
                pref = keyboard.toolbarNumRowOnPassword,
                title = stringResource(R.string.toolbar_num_row_on_password),
            )
            SwitchRow(
                pref = keyboard.showVoiceInputButton,
                title = stringResource(R.string.show_voice_input_button),
            )
        }
    }
}

private fun LazyListScope.feedbackSection() {
    item {
        val keyboard = AppPrefs.getInstance().keyboard
        SettingsGroup(
            title = stringResource(R.string.ksu_features_feedback),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            EnumChoiceRow(
                pref = keyboard.hapticOnKeyPress,
                title = stringResource(R.string.button_haptic_feedback),
                icon = Icons.Rounded.Vibration,
            )
            SwitchRow(
                pref = keyboard.hapticOnKeyUp,
                title = stringResource(R.string.button_up_haptic_feedback),
                enabled = keyboard.hapticOnKeyPress.getValue() !=
                    org.fcitx.fcitx5.android.data.InputFeedbacks.InputFeedbackMode.Disabled,
            )
            SwitchRow(
                pref = keyboard.hapticOnRepeat,
                title = stringResource(R.string.haptic_on_repeat),
                enabled = keyboard.hapticOnKeyPress.getValue() !=
                    org.fcitx.fcitx5.android.data.InputFeedbacks.InputFeedbackMode.Disabled,
            )
            SliderRow(
                pref = keyboard.buttonPressVibrationMilliseconds,
                title = stringResource(R.string.button_press),
                range = 0f..100f,
                valueText = { "$it ms" },
            )
            SliderRow(
                pref = keyboard.buttonLongPressVibrationMilliseconds,
                title = stringResource(R.string.button_long_press),
                range = 0f..100f,
                valueText = { "$it ms" },
            )
            SwitchRow(
                pref = keyboard.soundOnKeyPress,
                title = stringResource(R.string.button_sound),
                icon = Icons.Rounded.Speaker,
            )
            SliderRow(
                pref = keyboard.soundOnKeyPressVolume,
                title = stringResource(R.string.button_sound_volume),
                range = 0f..100f,
                valueText = { "$it%" },
                enabled = keyboard.soundOnKeyPress.getValue(),
            )
        }
    }
}

private fun LazyListScope.clipboardSection(onOpenRoute: (SettingsRoute) -> Unit) {
    item {
        val clipboard = AppPrefs.getInstance().clipboard
        SettingsGroup(
            title = stringResource(R.string.clipboard),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            SwitchRow(
                pref = clipboard.clipboardListening,
                title = stringResource(R.string.clipboard_listening),
                icon = Icons.Rounded.Paste,
            )
            SwitchRow(
                pref = clipboard.clipboardSuggestion,
                title = stringResource(R.string.clipboard_suggestion),
            )
            SwitchRow(
                pref = clipboard.clipboardReturnAfterPaste,
                title = stringResource(R.string.clipboard_return_after_paste),
            )
            SwitchRow(
                pref = clipboard.clipboardMaskSensitive,
                title = stringResource(R.string.clipboard_mask_sensitive),
            )
            SliderRow(
                pref = clipboard.clipboardHistoryLimitLocal,
                title = stringResource(R.string.clipboard_limit_local),
                range = 0f..500f,
                steps = 49,
                valueText = { it.toString() },
            )
            SliderRow(
                pref = clipboard.clipboardHistoryLimitRemote,
                title = stringResource(R.string.clipboard_limit_remote),
                range = 0f..500f,
                steps = 49,
                valueText = { it.toString() },
            )
            SliderRow(
                pref = clipboard.clipboardHistoryLimitMedia,
                title = stringResource(R.string.clipboard_limit_media),
                range = 0f..200f,
                steps = 19,
                valueText = { it.toString() },
            )
            SliderRow(
                pref = clipboard.clipboardItemTimeout,
                title = stringResource(R.string.clipboard_suggestion_timeout),
                range = 0f..720f,
                steps = 71,
                valueText = { "$it h" },
            )
            NavRow(
                title = stringResource(R.string.ksu_clipboard_sync),
                summary = stringResource(R.string.clipboard_sync_settings_summary),
                icon = Icons.Rounded.Memory,
                onClick = {
                    onOpenRoute(SettingsRoute.Clipboard)
                },
            )
        }
    }
}

private fun LazyListScope.pluginsSection(onOpenRoute: (SettingsRoute) -> Unit) {
    item {
        SettingsGroup(
            title = stringResource(R.string.plugins),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            NavRow(
                title = stringResource(R.string.plugins),
                icon = Icons.Rounded.Extension,
                onClick = { onOpenRoute(SettingsRoute.Plugin) },
            )
            NavRow(
                title = stringResource(R.string.advanced),
                icon = Icons.Rounded.MoreHoriz,
                onClick = { onOpenRoute(SettingsRoute.Advanced) },
            )
            NavRow(
                title = stringResource(R.string.about),
                icon = Icons.Rounded.Article,
                onClick = { onOpenRoute(SettingsRoute.About) },
            )
            NavRow(
                title = stringResource(R.string.virtual_keyboard),
                icon = Icons.Rounded.Keyboard,
                onClick = { onOpenRoute(SettingsRoute.VirtualKeyboard) },
            )
        }
    }
}
