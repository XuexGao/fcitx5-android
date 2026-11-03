/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.pages

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.LinearLayout
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.clipboardsync.ui.ClipboardSyncSettingsActivity
import org.fcitx.fcitx5.android.ui.design.KsuTokens
import org.fcitx.fcitx5.android.ui.design.KsuViews
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.utils.Const
import splitties.dimensions.dp

/**
 * "设置" page: client behaviour, diagnostics and the about section.
 */
class SettingsPageFragment : KsuPageFragment() {

    override fun buildContent(container: LinearLayout) {
        val ctx = requireContext()
        val inset = KsuTokens.pageInset(ctx)

        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_tab_settings)))
        container.addView(
            KsuViews.cardWithRows(
                ctx, listOf(
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.virtual_keyboard),
                        getString(R.string.keyboard_category_behavior),
                        icon = R.drawable.ic_baseline_keyboard_24,
                        onClick = { openRoute(SettingsRoute.VirtualKeyboard) }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.candidates_window),
                        null,
                        icon = R.drawable.ic_baseline_list_alt_24,
                        onClick = { openRoute(SettingsRoute.CandidatesWindow) }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.clipboard),
                        null,
                        icon = R.drawable.ic_clipboard,
                        onClick = { openRoute(SettingsRoute.Clipboard) }
                    )
                )
            ),
            cardParams(inset)
        )

        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_settings_app)))
        container.addView(
            KsuViews.cardWithRows(
                ctx, listOf(
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.clipboard_sync_settings),
                        getString(R.string.clipboard_sync_settings_summary),
                        icon = R.drawable.ic_tile_clipboard_sync,
                        onClick = {
                            startActivity(Intent(ctx, ClipboardSyncSettingsActivity::class.java))
                        }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.advanced),
                        null,
                        icon = R.drawable.ic_baseline_more_horiz_24,
                        onClick = { openRoute(SettingsRoute.Advanced) }
                    )
                )
            ),
            cardParams(inset)
        )

        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_settings_about)))
        container.addView(
            KsuViews.cardWithRows(
                ctx, listOf(
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_settings_faq),
                        null,
                        icon = R.drawable.ic_baseline_info_24,
                        onClick = {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Const.faqUrl)))
                        }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.developer),
                        null,
                        icon = R.drawable.ic_baseline_developer_mode_24,
                        onClick = { openRoute(SettingsRoute.Developer) }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.about),
                        null,
                        icon = R.drawable.ic_baseline_info_24,
                        onClick = { openRoute(SettingsRoute.About) }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.license),
                        null,
                        icon = R.drawable.ic_baseline_library_books_24,
                        onClick = { openRoute(SettingsRoute.License) }
                    )
                )
            ),
            cardParams(inset)
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
