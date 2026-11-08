/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.pages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceProvider
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.ui.compose.component.EnumChoiceRow
import org.fcitx.fcitx5.android.ui.compose.component.NavRow
import org.fcitx.fcitx5.android.ui.compose.component.PageScaffold
import org.fcitx.fcitx5.android.ui.compose.component.SettingsGroup
import org.fcitx.fcitx5.android.ui.compose.component.SliderRow
import org.fcitx.fcitx5.android.ui.compose.component.SwitchRow
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.ui.main.settings.theme.KeyboardPreviewUi

/**
 * 界面: everything about how the input method *looks* — keyboard skin, key geometry, keyboard
 * layout metrics, candidate window styling and the function bar.
 *
 * The preview at the top is a real keyboard rendered by [KeyboardPreviewUi]; it is rebuilt whenever
 * any keyboard appearance preference changes, so what you see matches what the keyboard will do.
 */
@Composable
fun AppearancePage(
    bottomInnerPadding: Dp,
    onOpenRoute: (SettingsRoute) -> Unit,
    onOpenActivity: (Class<*>) -> Unit,
) {
    PageScaffold(
        title = stringResource(R.string.ksu_tab_appearance),
        bottomInnerPadding = bottomInnerPadding,
    ) {
        previewSection()
        themeSection(onOpenRoute, onOpenActivity)
        keyAppearanceSection()
        keyboardLayoutSection(onOpenRoute)
        candidateSection(onOpenRoute)
        toolbarSection(onOpenActivity)
    }
}

/** Live keyboard preview; rebuilds on every keyboard-appearance preference change. */
private fun LazyListScope.previewSection() {
    item {
        val context = LocalContext.current
        val appearance = AppearancePrefs.ensureInitialized(context)
        val showPreview = appearance.showKeyboardPreview.getValue()
        // Bumping this counter tears the AndroidView down and rebuilds the keyboard with the new
        // radii / margins / colours. It is driven by both preference providers, so sliders update
        // the preview as you drag.
        var revision by remember { mutableIntStateOf(0) }

        val themePrefs = ThemeManager.prefs
        val keyboardPrefs = AppPrefs.getInstance().keyboard
        DisposableEffect(themePrefs, keyboardPrefs) {
            val listener = ManagedPreferenceProvider.OnChangeListener { revision++ }
            themePrefs.registerOnChangeListener(listener)
            keyboardPrefs.registerOnChangeListener(listener)
            val themeListener = ThemeManager.OnThemeChangeListener { revision++ }
            ThemeManager.addOnChangedListener(themeListener)
            onDispose {
                themePrefs.unregisterOnChangeListener(listener)
                keyboardPrefs.unregisterOnChangeListener(listener)
                ThemeManager.removeOnChangedListener(themeListener)
            }
        }

        if (showPreview) {
            SettingsGroup(
                title = stringResource(R.string.ksu_ime_appearance_preview),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                KeyboardPreview(
                    revision = revision,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .height(200.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
            }
        }
    }
}

/**
 * Hosts the View-based keyboard preview inside Compose.
 *
 * The keyboard is laid out at its natural width and centred, then scaled down to fit, so it looks
 * like a keyboard rather than a stretched strip. It is rebuilt only when [revision] changes (the
 * preference listeners bump it), not on every recomposition.
 */
@Composable
private fun KeyboardPreview(revision: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Only this subtree is keyed: the rest of the page keeps its state across revisions.
        key(revision) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    val ui = KeyboardPreviewUi(context, ThemeManager.activeTheme)
                    ui.root.apply {
                        // Natural width, scaled about its own centre: the keyboard keeps its
                        // proportions and stays centred instead of being stretched to the card.
                        scaleX = PREVIEW_SCALE
                        scaleY = PREVIEW_SCALE
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                update = { view -> view.invalidate() },
            )
        }
    }
}

/** How much the real keyboard is shrunk inside the preview card. */
private const val PREVIEW_SCALE = 0.72f

private fun LazyListScope.themeSection(
    onOpenRoute: (SettingsRoute) -> Unit,
    onOpenActivity: (Class<*>) -> Unit,
) {
    item {
        SettingsGroup(
            title = stringResource(R.string.ksu_ime_section_theme),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            NavRow(
                title = stringResource(R.string.theme),
                summary = ThemeManager.activeTheme.name,
                icon = Icons.Rounded.Palette,
                onClick = { onOpenRoute(SettingsRoute.Theme) },
            )
            NavRow(
                title = stringResource(R.string.ksu_ime_icon_theme),
                icon = Icons.Rounded.Wallpaper,
                onClick = { onOpenRoute(SettingsRoute.IconTheme) },
            )
            SwitchRow(
                pref = ThemeManager.prefs.followSystemDayNightTheme,
                title = stringResource(R.string.ksu_ime_follow_system_theme),

            )
        }
    }
}

private fun LazyListScope.keyAppearanceSection() {
    item {
        val prefs = ThemeManager.prefs
        SettingsGroup(
            title = stringResource(R.string.ksu_ime_section_key),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            SliderRow(
                pref = prefs.keyRadius,
                title = stringResource(R.string.ksu_ime_key_radius),
                range = 0f..48f,
                valueText = { "$it dp" },

            )
            SliderRow(
                pref = prefs.keyHorizontalMargin,
                title = stringResource(R.string.ksu_ime_key_h_margin),
                range = 0f..24f,
                valueText = { "$it dp" },

            )
            SliderRow(
                pref = prefs.keyHorizontalMarginLandscape,
                title = stringResource(R.string.ksu_ime_key_h_margin_land),
                range = 0f..24f,
                valueText = { "$it dp" },
            )
            SliderRow(
                pref = prefs.keyVerticalMargin,
                title = stringResource(R.string.ksu_ime_key_v_margin),
                range = 0f..24f,
                valueText = { "$it dp" },
            )
            SliderRow(
                pref = prefs.keyVerticalMarginLandscape,
                title = stringResource(R.string.ksu_ime_key_v_margin_land),
                range = 0f..24f,
                valueText = { "$it dp" },
            )
            SwitchRow(
                pref = prefs.keyBorder,
                title = stringResource(R.string.ksu_ime_key_border),

            )
            SwitchRow(
                pref = prefs.keyBorderStroke,
                title = stringResource(R.string.ksu_ime_key_border_stroke),
                enabled = prefs.keyBorder.getValue(),
            )
            SwitchRow(
                pref = prefs.keyRippleEffect,
                title = stringResource(R.string.ksu_ime_key_ripple),

            )
            SwitchRow(
                pref = prefs.specialKeyOvalShape,
                title = stringResource(R.string.ksu_ime_key_oval),

            )
            SliderRow(
                pref = prefs.textEditingButtonRadius,
                title = stringResource(R.string.ksu_ime_text_editing_radius),
                range = 0f..48f,
                valueText = { "$it dp" },
            )
            SliderRow(
                pref = prefs.clipboardEntryRadius,
                title = stringResource(R.string.ksu_ime_clipboard_radius),
                range = 0f..48f,
                valueText = { "$it dp" },
            )
            EnumChoiceRow(
                pref = prefs.navbarBackground,
                title = stringResource(R.string.ksu_ime_navbar_background),
                entries = org.fcitx.fcitx5.android.data.theme.ThemePrefs.NavbarBackground.entries,
                icon = Icons.Rounded.AspectRatio,
            )
        }
    }
}

private fun LazyListScope.keyboardLayoutSection(onOpenRoute: (SettingsRoute) -> Unit) {
    item {
        val prefs = AppPrefs.getInstance().keyboard
        SettingsGroup(
            title = stringResource(R.string.ksu_ime_section_layout),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            SliderRow(
                pref = prefs.keyboardHeightPercent,
                title = stringResource(R.string.ksu_ime_keyboard_height),
                range = 10f..90f,
                valueText = { "$it%" },

            )
            SliderRow(
                pref = prefs.keyboardHeightPercentLandscape,
                title = "${stringResource(R.string.ksu_ime_keyboard_height)}（横屏）",
                range = 10f..90f,
                valueText = { "$it%" },
            )
            SliderRow(
                pref = prefs.keyboardSidePadding,
                title = stringResource(R.string.ksu_ime_keyboard_side_padding),
                range = 0f..32f,
                valueText = { "$it dp" },
            )
            SliderRow(
                pref = prefs.keyboardBottomPadding,
                title = stringResource(R.string.ksu_ime_keyboard_bottom_padding),
                range = 0f..100f,
                valueText = { "$it dp" },
            )
            NavRow(
                title = stringResource(R.string.ksu_ime_layout_profile),
                summary = prefs.textKeyboardLayoutProfile.getValue(),
                icon = Icons.Rounded.Tune,
                onClick = { onOpenRoute(SettingsRoute.KeyboardGroup(0)) },
            )
            NavRow(
                title = stringResource(R.string.ksu_ime_numeric_layout),
                summary = prefs.numericLayoutOverride.getValue().ifEmpty {
                    stringResource(R.string.numeric_layout_override_builtin)
                },
                icon = Icons.Rounded.Tune,
                onClick = { onOpenRoute(SettingsRoute.KeyboardGroup(0)) },
            )
            SwitchRow(
                pref = prefs.splitKeyboardEnabled,
                title = stringResource(R.string.ksu_ime_split_keyboard),
            )
            SliderRow(
                pref = prefs.splitKeyboardThreshold,
                title = stringResource(R.string.ksu_ime_split_keyboard_threshold),
                range = 320f..900f,
                steps = 57,
                valueText = { "$it dp" },
                enabled = prefs.splitKeyboardEnabled.getValue(),
            )
            SliderRow(
                pref = prefs.splitKeyboardGapPercent,
                title = stringResource(R.string.ksu_ime_split_keyboard_gap),
                range = 0f..60f,
                valueText = { "$it%" },
                enabled = prefs.splitKeyboardEnabled.getValue(),
            )
            SwitchRow(
                pref = prefs.splitKeyboardUseLandscapeLayout,
                title = stringResource(R.string.ksu_ime_split_keyboard_landscape),
                enabled = prefs.splitKeyboardEnabled.getValue(),
            )
        }
    }
}

private fun LazyListScope.candidateSection(onOpenRoute: (SettingsRoute) -> Unit) {
    item {
        val prefs = AppPrefs.getInstance().candidates
        val keyboard = AppPrefs.getInstance().keyboard
        SettingsGroup(
            title = stringResource(R.string.ksu_ime_section_candidates),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            SliderRow(
                pref = prefs.fontSize,
                title = stringResource(R.string.ksu_ime_candidate_font_size),
                range = 12f..32f,
                valueText = { "$it sp" },

            )
            SliderRow(
                pref = prefs.windowRadius,
                title = stringResource(R.string.ksu_ime_candidate_radius),
                range = 0f..48f,
                valueText = { "$it dp" },
            )
            SliderRow(
                pref = prefs.candidateHighlightRadius,
                title = stringResource(R.string.ksu_ime_candidate_highlight_radius),
                range = 0f..48f,
                valueText = { "$it dp" },
            )
            SliderRow(
                pref = prefs.windowMinWidth,
                title = stringResource(R.string.ksu_ime_candidate_min_width),
                range = 0f..600f,
                steps = 59,
                valueText = { "$it dp" },
            )
            SliderRow(
                pref = prefs.windowPadding,
                title = stringResource(R.string.ksu_ime_candidate_padding),
                range = 0f..32f,
                valueText = { "$it dp" },
            )
            EnumChoiceRow(
                pref = keyboard.horizontalCandidateStyle,
                title = stringResource(R.string.ksu_ime_horizontal_candidate_style),
                entries = org.fcitx.fcitx5.android.input.candidates.horizontal.HorizontalCandidateMode.entries,
            )
            EnumChoiceRow(
                pref = keyboard.expandedCandidateStyle,
                title = stringResource(R.string.ksu_ime_expanded_candidate_style),
                entries = org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateStyle.entries,
            )
        }
    }
}

private fun LazyListScope.toolbarSection(onOpenActivity: (Class<*>) -> Unit) {
    item {
        val prefs = AppPrefs.getInstance().keyboard
        SettingsGroup(
            title = stringResource(R.string.ksu_ime_section_toolbar),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            SwitchRow(
                pref = prefs.expandToolbarByDefault,
                title = stringResource(R.string.ksu_ime_expand_toolbar),
            )
            NavRow(
                title = stringResource(R.string.ksu_features_toolbar_customize),
                summary = stringResource(R.string.ksu_features_toolbar_customize_summary),
                icon = Icons.Rounded.Tune,
                onClick = {
                    onOpenActivity(
                        org.fcitx.fcitx5.android.ui.main.settings.behavior.ButtonsCustomizerActivity::class.java
                    )
                },
            )
        }
    }
}
