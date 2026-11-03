/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.pages

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Keep
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.ui.design.KsuTokens
import org.fcitx.fcitx5.android.ui.design.KsuViews
import org.fcitx.fcitx5.android.ui.main.settings.SettingsRoute
import org.fcitx.fcitx5.android.ui.main.settings.icon.IconThemeListActivity
import org.fcitx.fcitx5.android.ui.main.settings.theme.KeyboardPreviewUi
import org.fcitx.fcitx5.android.utils.startActivity
import splitties.dimensions.dp

/**
 * "界面" page: every visual knob, with a keyboard preview that updates as you touch things.
 *
 * Sliders deliberately avoid a full page rebuild while dragging — they repaint the preview and push
 * the change to the live keyboard instead, so the thumb never jumps. Discrete controls (colour,
 * style, switches) rebuild the page so the new radius/colour is applied to the cards themselves.
 */
class AppearancePageFragment : KsuPageFragment() {

    private var previewHost: FrameLayout? = null
    private var previewUi: KeyboardPreviewUi? = null

    @Keep
    private val themeListener = ThemeManager.OnThemeChangeListener { refreshPreview() }

    private val themePrefs get() = ThemeManager.prefs

    override fun buildContent(container: LinearLayout) {
        val ctx = requireContext()
        val inset = KsuTokens.pageInset(ctx)
        previewHost = null

        // ---- Live preview -------------------------------------------------------
        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_appearance_preview)))
        container.addView(
            KsuViews.card {
                addView(previewContainer())
                addView(
                    TextView(ctx).apply {
                        text = getString(R.string.ksu_appearance_preview_hint)
                        setTextColor(KsuTokens.onSurfaceVariant(ctx))
                        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                        setPadding(
                            KsuTokens.pageInset(ctx), ctx.dp(4),
                            KsuTokens.pageInset(ctx), ctx.dp(12)
                        )
                    }
                )
            },
            cardParams(inset)
        )

        // ---- Colours ------------------------------------------------------------
        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_appearance_colors)))
        container.addView(
            KsuViews.card {
                addView(
                    KsuViews.colorRow(
                        ctx,
                        getString(R.string.ksu_appearance_accent),
                        AppearancePrefs.ACCENT_PRESETS,
                        AppearancePrefs.ACCENT_PRESETS.indexOf(appearance.accentColor.getValue()),
                        onSelect = { color -> applyDiscrete { appearance.accentColor.setValue(color) } }
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.ksu_appearance_theme),
                        getString(R.string.ksu_appearance_theme_summary),
                        icon = R.drawable.ic_baseline_palette_24,
                        onClick = { openRoute(SettingsRoute.Theme) }
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    KsuViews.listItem(
                        ctx,
                        getString(R.string.icon_theme),
                        null,
                        icon = R.drawable.ic_icon_theme_24,
                        onClick = { startActivity<IconThemeListActivity>() }
                    )
                )
            },
            cardParams(inset)
        )

        // ---- Layout -------------------------------------------------------------
        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_appearance_layout)))
        container.addView(
            KsuViews.card {
                addView(
                    KsuViews.sliderItem(
                        ctx,
                        getString(R.string.ksu_appearance_corner_radius),
                        appearance.cornerRadius.getValue(),
                        0, 36,
                        valueLabel = { "$it dp" },
                        onValueChange = { value ->
                            appearance.cornerRadius.setValue(value)
                            onLayoutChanged()
                        },
                        onValueSettled = { rebuildPage() }
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    KsuViews.sliderItem(
                        ctx,
                        getString(R.string.ksu_appearance_spacing),
                        appearance.spacing.getValue(),
                        8, 32,
                        valueLabel = { "$it dp" },
                        onValueChange = { value ->
                            appearance.spacing.setValue(value)
                            onLayoutChanged()
                        },
                        onValueSettled = { rebuildPage() }
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    KsuViews.choiceRow(
                        ctx,
                        getString(R.string.ksu_appearance_card_style),
                        listOf(
                            getString(R.string.ksu_appearance_card_elevated),
                            getString(R.string.ksu_appearance_card_flat),
                            getString(R.string.ksu_appearance_card_outlined)
                        ),
                        appearance.cardStyleValue.ordinal,
                        onSelect = { index ->
                            applyDiscrete {
                                appearance.cardStyle.setValue(
                                    AppearancePrefs.CardStyle.entries[index].name
                                )
                            }
                        }
                    )
                )
            },
            cardParams(inset)
        )

        // ---- Keyboard keys ------------------------------------------------------
        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_appearance_keys)))
        container.addView(
            KsuViews.card {
                addView(
                    KsuViews.sliderItem(
                        ctx,
                        getString(R.string.ksu_appearance_key_radius),
                        themePrefs.keyRadius.getValue(),
                        0, 48,
                        valueLabel = { "$it dp" },
                        onValueChange = { value ->
                            themePrefs.keyRadius.setValue(value)
                            onKeyAppearanceChanged()
                        }
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    KsuViews.sliderItem(
                        ctx,
                        getString(R.string.ksu_appearance_key_h_margin),
                        themePrefs.keyHorizontalMargin.getValue(),
                        0, 24,
                        valueLabel = { "$it dp" },
                        onValueChange = { value ->
                            themePrefs.keyHorizontalMargin.setValue(value)
                            onKeyAppearanceChanged()
                        }
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    KsuViews.sliderItem(
                        ctx,
                        getString(R.string.ksu_appearance_key_v_margin),
                        themePrefs.keyVerticalMargin.getValue(),
                        0, 24,
                        valueLabel = { "$it dp" },
                        onValueChange = { value ->
                            themePrefs.keyVerticalMargin.setValue(value)
                            onKeyAppearanceChanged()
                        }
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    KsuViews.switchItem(
                        ctx,
                        getString(R.string.ksu_appearance_key_border),
                        null,
                        checked = themePrefs.keyBorder.getValue(),
                        onCheckedChange = { value ->
                            themePrefs.keyBorder.setValue(value)
                            applyDiscrete { }
                        }
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    KsuViews.switchItem(
                        ctx,
                        getString(R.string.ksu_appearance_key_ripple),
                        null,
                        checked = themePrefs.keyRippleEffect.getValue(),
                        onCheckedChange = { value ->
                            themePrefs.keyRippleEffect.setValue(value)
                            applyDiscrete { }
                        }
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    KsuViews.switchItem(
                        ctx,
                        getString(R.string.ksu_appearance_key_oval),
                        null,
                        checked = themePrefs.specialKeyOvalShape.getValue(),
                        onCheckedChange = { value ->
                            themePrefs.specialKeyOvalShape.setValue(value)
                            applyDiscrete { }
                        }
                    )
                )
            },
            cardParams(inset)
        )

        // ---- Bottom bar ---------------------------------------------------------
        container.addView(KsuViews.sectionHeader(ctx, getString(R.string.ksu_appearance_bottom_bar)))
        container.addView(
            KsuViews.card {
                addView(
                    KsuViews.choiceRow(
                        ctx,
                        getString(R.string.ksu_appearance_function_bar_style),
                        listOf(
                            getString(R.string.ksu_appearance_style_icon),
                            getString(R.string.ksu_appearance_style_label),
                            getString(R.string.ksu_appearance_style_icon_label)
                        ),
                        appearance.functionBarStyleValue.ordinal,
                        onSelect = { index ->
                            applyDiscrete {
                                appearance.functionBarStyle.setValue(
                                    AppearancePrefs.FunctionBarStyle.entries[index].name
                                )
                            }
                        }
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    KsuViews.switchItem(
                        ctx,
                        getString(R.string.ksu_appearance_floating_bar),
                        null,
                        checked = appearance.floatingBottomBar.getValue(),
                        onCheckedChange = { value -> applyDiscrete { appearance.floatingBottomBar.setValue(value) } }
                    )
                )
                addView(KsuViews.divider(ctx))
                addView(
                    KsuViews.switchItem(
                        ctx,
                        getString(R.string.ksu_appearance_liquid_glass),
                        getString(R.string.ksu_appearance_liquid_glass_summary),
                        checked = appearance.liquidGlass.getValue(),
                        onCheckedChange = { value -> applyDiscrete { appearance.liquidGlass.setValue(value) } }
                    )
                )
            },
            cardParams(inset)
        )

        // ---- Reset --------------------------------------------------------------
        container.addView(
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                addView(
                    KsuViews.outlineButton(
                        ctx, getString(R.string.ksu_appearance_reset)
                    ) {
                        appearance.resetToDefaults()
                        toast(getString(R.string.ksu_appearance_reset_done))
                        rebuildPage()
                    },
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = inset
                marginEnd = inset
                topMargin = ctx.dp(20)
            }
        )
    }

    private fun previewContainer(): FrameLayout {
        val ctx = requireContext()
        val host = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(190)
            )
            setBackgroundColor(Color.TRANSPARENT)
        }
        previewHost = host
        host.post { refreshPreview() }
        return host
    }

    /** Rebuilds the keyboard preview so it picks up new radii, margins and colours. */
    private fun refreshPreview() {
        val host = previewHost ?: return
        val ctx = requireContext()
        val scale = 0.62f
        host.removeAllViews()
        val ui = KeyboardPreviewUi(ctx, ThemeManager.activeTheme)
        previewUi = ui
        ui.onSizeMeasured = { _, height -> resizePreviewHost(host, height, scale) }
        ui.root.apply {
            scaleX = scale
            scaleY = scale
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }
        host.addView(ui.root)
        ui.setTheme(ThemeManager.activeTheme)
        // The first measurement may happen before the keyboard exists, so retry once the theme has
        // been applied and a layout pass has definitely run.
        host.post { resizePreviewHost(host, ui.root.height, scale) }
    }

    private fun resizePreviewHost(host: FrameLayout, height: Int, scale: Float) {
        if (height <= 0) return
        val target = (height * scale).toInt().coerceAtLeast(host.context.dp(120))
        if (host.layoutParams.height != target) {
            host.layoutParams = host.layoutParams.apply { this.height = target }
            host.requestLayout()
        }
    }

    /** Applies a discrete change and repaints the whole page with the new tokens. */
    private inline fun applyDiscrete(block: () -> Unit) {
        block()
        rebuildPage()
        onKeyAppearanceChanged()
    }

    private fun onLayoutChanged() {
        // Card radii and insets are baked into the views, so a full rebuild is required, but the
        // live preview keeps the slider response immediate.
        ThemeManager.notifyAppearanceChanged()
    }

    private fun onKeyAppearanceChanged() {
        ThemeManager.notifyAppearanceChanged()
    }

    override fun onAppearanceChanged(key: String) {
        // Slider-driven keys are handled by their own callbacks to avoid destroying the slider
        // mid-drag; everything else rebuilds.
        if (key == AppearancePrefs.KEY_CORNER_RADIUS || key == AppearancePrefs.KEY_SPACING) {
            ThemeManager.notifyAppearanceChanged()
            return
        }
        rebuildPage()
    }

    override fun onStart() {
        super.onStart()
        ThemeManager.addOnChangedListener(themeListener)
    }

    override fun onStop() {
        ThemeManager.removeOnChangedListener(themeListener)
        super.onStop()
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
