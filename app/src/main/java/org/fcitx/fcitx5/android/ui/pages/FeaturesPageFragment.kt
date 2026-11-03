/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.pages

import android.content.Intent
import android.view.ViewGroup
import android.widget.LinearLayout
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.clipboardsync.ui.ClipboardSyncSettingsActivity
import org.fcitx.fcitx5.android.data.rime.RimePrefs
import org.fcitx.fcitx5.android.ui.design.KsuTokens
import org.fcitx.fcitx5.android.ui.design.KsuViews
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.ui.main.settings.behavior.ButtonsCustomizerActivity
import splitties.dimensions.dp

/**
 * "功能" page: what the input method can do, plus the entry points that used to be buried in the
 * single settings list.
 */
class FeaturesPageFragment : KsuPageFragment() {

    override fun buildContent(container: LinearLayout) {
        val ctx = requireContext()
        val inset = KsuTokens.pageInset(ctx)

        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_features_input)))
        container.addView(
            KsuViews.cardWithRows(
                ctx, listOf(
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.input_methods),
                        null,
                        icon = R.drawable.ic_baseline_language_24,
                        onClick = { openRoute(SettingsRoute.InputMethodList) }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.addons),
                        null,
                        icon = R.drawable.ic_baseline_extension_24,
                        onClick = { openRoute(SettingsRoute.AddonList) }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.global_options),
                        null,
                        icon = R.drawable.ic_baseline_tune_24,
                        onClick = { openRoute(SettingsRoute.GlobalConfig) }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_rime_title),
                        rimeSchemaSummary(),
                        icon = R.drawable.ic_status_rime,
                        onClick = { openRoute(SettingsRoute.RimeSchemas) }
                    )
                )
            ),
            cardParams(inset)
        )

        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_features_tools)))
        container.addView(
            KsuViews.cardWithRows(
                ctx, listOf(
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.quickphrase_editor),
                        null,
                        icon = R.drawable.ic_baseline_format_quote_24,
                        onClick = { openRoute(SettingsRoute.QuickPhraseList) }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_pinyin_custom_phrase),
                        null,
                        icon = R.drawable.ic_baseline_text_format_24,
                        onClick = { openRoute(SettingsRoute.PinyinCustomPhrase) }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.table_im),
                        null,
                        icon = R.drawable.ic_baseline_library_books_24,
                        onClick = { openRoute(SettingsRoute.TableInputMethods) }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.emoji_and_symbols),
                        null,
                        icon = R.drawable.ic_baseline_emoji_symbols_24,
                        onClick = { openRoute(SettingsRoute.Symbol) }
                    )
                )
            ),
            cardParams(inset)
        )

        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_features_efficiency)))
        container.addView(
            KsuViews.cardWithRows(
                ctx, listOf(
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.clipboard),
                        null,
                        icon = R.drawable.ic_clipboard,
                        onClick = { openRoute(SettingsRoute.Clipboard) }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_clipboard_sync),
                        null,
                        icon = R.drawable.ic_tile_clipboard_sync,
                        onClick = {
                            startActivity(
                                Intent(ctx, ClipboardSyncSettingsActivity::class.java)
                            )
                        }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.plugins),
                        null,
                        icon = R.drawable.ic_baseline_android_24,
                        onClick = { openRoute(SettingsRoute.Plugin) }
                    )
                )
            ),
            cardParams(inset)
        )

        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_features_toolbar)))
        container.addView(
            KsuViews.cardWithRows(
                ctx, listOf(
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_features_toolbar_customize),
                        getString(R.string.ksu_features_toolbar_customize_summary),
                        icon = R.drawable.ic_baseline_tune_24,
                        onClick = {
                            startActivity(
                                Intent(ctx, ButtonsCustomizerActivity::class.java)
                            )
                        }
                    )
                )
            ),
            cardParams(inset)
        )
    }

    /** Shows the active 万象拼音 double-pinyin layout, or a hint to open the page. */
    private fun rimeSchemaSummary(): String {
        val prefs = RimePrefs.ensureInitialized(requireContext())
        val layout = RimePrefs.DoublePinyin.fromId(prefs.doublePinyin.getValue())
        return "${prefs.defaultSchema.getValue()} · ${layout.label}"
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
