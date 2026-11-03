/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.rime

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.rime.RimeAutoDeploy
import org.fcitx.fcitx5.android.data.rime.RimeManager
import org.fcitx.fcitx5.android.data.rime.RimePrefs
import org.fcitx.fcitx5.android.ui.design.KsuTokens
import org.fcitx.fcitx5.android.ui.design.KsuViews
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.ui.pages.KsuPageFragment
import splitties.dimensions.dp
import java.io.File

/**
 * Graphical configuration for Rime.
 *
 * Kept deliberately small: pick the default schema, enable/disable schemas, choose the
 * double-pinyin layout, tweak a couple of parameters, then deploy/export/import/reset. Everything
 * is written to `*.custom.yaml` in the Rime user directory and takes effect on the next deploy.
 */
class RimeSchemaFragment : KsuPageFragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/yaml")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val ok = withContextIo {
                // SAF gives us a stream, so copy the generated text through a temp file.
                val tmp = File(requireContext().cacheDir, "rime-config-export.yaml")
                RimeManager.exportConfiguration(requireContext(), tmp) &&
                    runCatching {
                        requireContext().contentResolver.openOutputStream(uri)?.use { out ->
                            tmp.inputStream().use { it.copyTo(out) }
                        } ?: return@withContextIo false
                        true
                    }.getOrDefault(false)
            }
            if (ok) toast(getString(R.string.ksu_rime_export_done))
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val ok = withContextIo {
                runCatching {
                    val tmp = File(requireContext().cacheDir, "rime-config-import.yaml")
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        tmp.outputStream().use { input.copyTo(it) }
                    }
                    RimeManager.importConfiguration(requireContext(), tmp)
                }.getOrDefault(false)
            }
            toast(
                getString(
                    if (ok) R.string.ksu_rime_import_done else R.string.ksu_rime_import_failed
                )
            )
            if (ok) rebuildPage()
        }
    }

    private suspend fun <T> withContextIo(block: () -> T): T =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { block() }

    override fun buildContent(container: LinearLayout) {
        val ctx = requireContext()
        val inset = KsuTokens.pageInset(ctx)
        val prefs = RimePrefs.ensureInitialized(ctx)
        val schemas = RimeManager.availableSchemas(ctx)
        val currentDefault = prefs.defaultSchema.getValue()
        val enabled = prefs.enabledSchemas.getValue()

        // ---- Status -------------------------------------------------------------
        val available = RimeManager.isWanxiangAvailable()
        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_rime_status)))
        container.addView(
            KsuViews.cardWithRows(
                ctx, listOf(
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_rime_default_schema),
                        currentDefault,
                        icon = R.drawable.ic_status_rime
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_rime_title),
                        getString(
                            if (available) R.string.ksu_rime_available
                            else R.string.ksu_rime_unavailable
                        ),
                        icon = if (available) R.drawable.ic_baseline_check_circle_24
                        else R.drawable.ic_baseline_info_24,
                        iconTint = if (available) KsuTokens.accent(ctx)
                        else KsuTokens.onSurfaceVariant(ctx)
                    )
                )
            ),
            cardParams(inset)
        )

        // ---- Default schema -----------------------------------------------------
        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_rime_default_schema)))
        container.addView(
            KsuViews.card(ctx) {
                schemas.forEachIndexed { index, schema ->
                    if (index > 0) addView(KsuViews.divider(ctx))
                    addView(
                        KsuViews.listItem(
                            ctx,
                            schema.displayName,
                            schema.id,
                            icon = if (schema.id == currentDefault)
                                R.drawable.ic_baseline_check_circle_24
                            else R.drawable.ic_baseline_language_24,
                            iconTint = if (schema.id == currentDefault) KsuTokens.accent(ctx)
                            else KsuTokens.onSurfaceVariant(ctx),
                            onClick = {
                                prefs.defaultSchema.setValue(schema.id)
                                prefs.enabledSchemas.setValue(enabled + schema.id)
                                applyAndMaybeDeploy()
                                rebuildPage()
                            }
                        )
                    )
                }
            },
            cardParams(inset)
        )

        // ---- Enable / disable ---------------------------------------------------
        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_rime_schema_list)))
        container.addView(
            KsuViews.card(ctx) {
                addView(
                    android.widget.TextView(ctx).apply {
                        text = getString(R.string.ksu_rime_schema_list_summary)
                        setTextColor(KsuTokens.onSurfaceVariant(ctx))
                        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                        setPadding(
                            KsuTokens.pageInset(ctx), ctx.dp(10),
                            KsuTokens.pageInset(ctx), 0
                        )
                    }
                )
                schemas.forEachIndexed { index, schema ->
                    if (index > 0) addView(KsuViews.divider(ctx))
                    addView(
                        KsuViews.switchItem(
                            ctx,
                            schema.displayName,
                            schema.id,
                            checked = enabled.contains(schema.id),
                            // The default schema must stay enabled, otherwise Rime falls back.
                            enabled = schema.id != currentDefault,
                            onCheckedChange = { value ->
                                val next = enabled.toMutableSet()
                                if (value) next += schema.id else next -= schema.id
                                next += currentDefault
                                prefs.enabledSchemas.setValue(next)
                                applyAndMaybeDeploy()
                                rebuildPage()
                            }
                        )
                    )
                }
            },
            cardParams(inset)
        )

        // ---- Double pinyin ------------------------------------------------------
        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_rime_double_pinyin)))
        container.addView(
            KsuViews.card(ctx) {
                addView(
                    android.widget.TextView(ctx).apply {
                        text = getString(R.string.ksu_rime_double_pinyin_summary)
                        setTextColor(KsuTokens.onSurfaceVariant(ctx))
                        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                        setPadding(
                            KsuTokens.pageInset(ctx), ctx.dp(10),
                            KsuTokens.pageInset(ctx), 0
                        )
                    }
                )
                val current = RimePrefs.DoublePinyin.fromId(prefs.doublePinyin.getValue())
                RimePrefs.DoublePinyin.entries.forEachIndexed { index, option ->
                    if (index > 0) addView(KsuViews.divider(ctx))
                    addView(
                        KsuViews.listItem(
                            ctx,
                            option.label,
                            if (option == RimePrefs.DoublePinyin.全拼) "wanxiang_algebra:/base/全拼"
                            else "wanxiang_algebra:/base/${option.id}",
                            icon = if (option == current) R.drawable.ic_baseline_check_circle_24
                            else R.drawable.ic_baseline_keyboard_24,
                            iconTint = if (option == current) KsuTokens.accent(ctx)
                            else KsuTokens.onSurfaceVariant(ctx),
                            onClick = {
                                prefs.doublePinyin.setValue(option.id)
                                applyAndMaybeDeploy()
                                rebuildPage()
                            }
                        )
                    )
                }
            },
            cardParams(inset)
        )

        // ---- Parameters ---------------------------------------------------------
        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_rime_title)))
        container.addView(
            KsuViews.card(ctx) {
                addView(
                    KsuViews.sliderItem(
                        ctx,
                        getString(R.string.ksu_rime_page_size),
                        prefs.pageSize.getValue(),
                        3, 10,
                        valueLabel = { it.toString() },
                        onValueChange = { value -> prefs.pageSize.setValue(value) },
                        onValueSettled = {
                            applyAndMaybeDeploy()
                            rebuildPage()
                        }
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    fuzzyRow(
                        getString(R.string.ksu_rime_fuzzy_z_zh), prefs.fuzzyZhZ
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    fuzzyRow(
                        getString(R.string.ksu_rime_fuzzy_c_ch), prefs.fuzzyChC
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    fuzzyRow(
                        getString(R.string.ksu_rime_fuzzy_s_sh), prefs.fuzzyShS
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    fuzzyRow(
                        getString(R.string.ksu_rime_fuzzy_n_l), prefs.fuzzyNL
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    fuzzyRow(
                        getString(R.string.ksu_rime_fuzzy_in_ing), prefs.fuzzyInIng
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    fuzzyRow(
                        getString(R.string.ksu_rime_fuzzy_an_ang), prefs.fuzzyAnAng
                    )
                )
            },
            cardParams(inset)
        )

        // ---- Actions ------------------------------------------------------------
        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_rime_actions)))
        container.addView(
            KsuViews.cardWithRows(
                ctx, listOf(
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_rime_redeploy),
                        getString(R.string.ksu_rime_redeploy_summary),
                        icon = R.drawable.ic_baseline_sync_24,
                        onClick = {
                            lifecycleScope.launch {
                                RimeAutoDeploy.redeploy(viewModel.fcitx)
                                toast(getString(R.string.ksu_rime_redeploy_done))
                            }
                        }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_rime_export),
                        null,
                        icon = R.drawable.ic_baseline_share_24,
                        onClick = { exportLauncher.launch("rime-config.yaml") }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_rime_import),
                        null,
                        icon = R.drawable.ic_baseline_save_24,
                        onClick = {
                            importLauncher.launch(arrayOf("application/yaml", "text/plain", "*/*"))
                        }
                    ),
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_rime_reset),
                        null,
                        icon = R.drawable.ic_baseline_settings_backup_restore_24,
                        onClick = {
                            RimeManager.resetToDefaults(ctx)
                            toast(getString(R.string.ksu_rime_reset_done))
                            rebuildPage()
                        }
                    )
                )
            ),
            cardParams(inset)
        )

        // ---- License ------------------------------------------------------------
        container.addView(
            android.widget.TextView(ctx).apply {
                text = getString(R.string.ksu_rime_bundled_license)
                setTextColor(KsuTokens.onSurfaceVariant(ctx))
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
                setPadding(
                    KsuTokens.pageInset(ctx) + ctx.dp(4), ctx.dp(16),
                    KsuTokens.pageInset(ctx) + ctx.dp(4), 0
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun fuzzyRow(
        title: String,
        pref: org.fcitx.fcitx5.android.data.prefs.ManagedPreference.PBool
    ): android.view.View = KsuViews.switchItem(
        requireContext(),
        title,
        null,
        checked = pref.getValue(),
        onCheckedChange = { value ->
            pref.setValue(value)
            applyAndMaybeDeploy()
            rebuildPage()
        }
    )

    /** Writes the config; a redeploy is left to the explicit action to avoid surprise rebuilds. */
    private fun applyAndMaybeDeploy() {
        lifecycleScope.launch {
            RimeAutoDeploy.ensureDeployed(requireContext())
        }
    }

    private fun cardParams(inset: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = inset
            marginEnd = inset
            topMargin = requireContext().dp(2)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure the generated config matches the preferences as soon as the page is opened.
        lifecycleScope.launch { RimeAutoDeploy.ensureDeployed(requireContext()) }
    }
}
