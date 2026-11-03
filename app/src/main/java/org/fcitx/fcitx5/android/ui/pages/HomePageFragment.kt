/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.pages

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.ui.design.KsuTokens
import org.fcitx.fcitx5.android.ui.design.KsuViews
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.ui.setup.SetupActivity
import org.fcitx.fcitx5.android.utils.InputMethodUtil
import org.fcitx.fcitx5.android.utils.startActivity
import splitties.dimensions.dp

/**
 * Home page: current state plus the handful of actions people actually need day to day.
 */
class HomePageFragment : KsuPageFragment() {

    override fun buildContent(container: LinearLayout) {
        val ctx = requireContext()
        val inset = KsuTokens.pageInset(ctx)

        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_home_status)))
        container.addView(
            KsuViews.cardWithRows(
                ctx, listOf(
                    statusRow(),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_home_ime_default),
                        imeSelectionSummary(),
                        icon = R.drawable.ic_baseline_keyboard_24
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_home_enable_ime),
                        getString(R.string.ksu_home_enable_ime_summary),
                        icon = R.drawable.ic_baseline_settings_24,
                        onClick = { InputMethodUtil.startSettingsActivity(ctx) }
                    )
                )
            ),
            cardParams(inset)
        )

        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_home_quick_actions)))
        container.addView(
            KsuViews.cardWithRows(
                ctx, listOf(
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_home_pick_ime),
                        getString(R.string.ksu_home_pick_ime_summary),
                        icon = R.drawable.ic_baseline_language_24,
                        onClick = { InputMethodUtil.showPicker() }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.input_methods),
                        null,
                        icon = R.drawable.ic_baseline_language_24,
                        onClick = { openRoute(SettingsRoute.InputMethodList) }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.virtual_keyboard),
                        null,
                        icon = R.drawable.ic_baseline_keyboard_24,
                        onClick = { openRoute(SettingsRoute.VirtualKeyboard) }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_home_setup_wizard),
                        getString(R.string.ksu_home_setup_wizard_summary),
                        icon = R.drawable.ic_baseline_auto_awesome_24,
                        onClick = { startActivity<SetupActivity>() }
                    )
                )
            ),
            cardParams(inset)
        )

        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_home_about)))
        container.addView(
            KsuViews.cardWithRows(
                ctx, listOf(
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_home_version),
                        BuildConfig.VERSION_NAME,
                        icon = R.drawable.ic_baseline_info_24
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_home_package),
                        ctx.packageName,
                        icon = R.drawable.ic_baseline_android_24
                    )
                )
            ),
            cardParams(inset)
        )
    }

    private fun statusRow(): View {
        val ctx = requireContext()
        val enabled = runCatching { InputMethodUtil.isEnabled() }.getOrDefault(false)
        return KsuViews.listItem(
            ctx,
            getString(R.string.ksu_home_ime_status),
            getString(if (enabled) R.string.ksu_home_ime_enabled else R.string.ksu_home_ime_disabled),
            icon = if (enabled) R.drawable.ic_baseline_check_circle_24
            else R.drawable.ic_baseline_info_24,
            iconTint = if (enabled) KsuTokens.accent(ctx) else KsuTokens.onSurfaceVariant(ctx)
        )
    }

    private fun imeSelectionSummary(): String {
        val selected = runCatching { InputMethodUtil.isSelected() }.getOrDefault(false)
        return getString(
            if (selected) R.string.ksu_home_ime_default else R.string.ksu_home_ime_not_default
        )
    }

    private fun cardParams(inset: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = inset
            marginEnd = inset
            topMargin = requireContext().dp(2)
        }
}
