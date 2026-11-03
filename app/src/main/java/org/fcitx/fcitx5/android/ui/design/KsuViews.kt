/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.design

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import splitties.dimensions.dp

/**
 * KernelSU-style view builders.
 *
 * These are plain Android views rather than Compose widgets: the rest of the settings UI is built on
 * the Preference framework, and mixing in Compose just for the shell would have meant two rendering
 * stacks and a much larger blast radius. The visual language (grouped cards, generous radii, one
 * accent colour) is ported instead.
 */
object KsuViews {

    @ColorInt
    private fun rippleColor(context: Context): Int = KsuTokens.accent(context).let {
        Color.argb(0x33, Color.red(it), Color.green(it), Color.blue(it))
    }

    /** A grouped card. Contents are stacked vertically. */
    fun card(context: Context, content: LinearLayout.() -> Unit): LinearLayout {
        val style = runCatching {
            AppearancePrefs.ensureInitialized(context).cardStyleValue
        }.getOrDefault(AppearancePrefs.CardStyle.Elevated)
        val radius = KsuTokens.cardRadius(context)
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            background = when (style) {
                AppearancePrefs.CardStyle.Elevated -> roundedBackground(KsuTokens.card(context), radius)
                AppearancePrefs.CardStyle.Flat -> roundedBackground(KsuTokens.card(context), radius)
                AppearancePrefs.CardStyle.Outlined -> outlinedBackground(
                    Color.TRANSPARENT, radius, KsuTokens.divider(context), context.dp(1)
                )
            }
            if (style == AppearancePrefs.CardStyle.Elevated) {
                applySoftElevation(context.dp(2f))
            }
            clipToOutline = true
            content()
        }
    }

    fun sectionHeader(context: Context, text: String): TextView =
        TextView(context).apply {
            this.text = text
            setTextColor(KsuTokens.onSurfaceVariant(context))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.02f
            setPadding(
                KsuTokens.pageInset(context) + context.dp(4),
                context.dp(18),
                KsuTokens.pageInset(context),
                context.dp(6)
            )
        }

    fun divider(context: Context): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, context.dp(1)
        ).apply {
            marginStart = context.dp(52)
        }
        setBackgroundColor(KsuTokens.divider(context))
    }

    /**
     * Standard navigable row: icon, title, optional summary, chevron.
     */
    fun listItem(
        context: Context,
        title: String,
        summary: String? = null,
        @DrawableRes icon: Int? = null,
        @ColorInt iconTint: Int? = null,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null
    ): View = rowContainer(context, enabled).apply {
        if (icon != null) addView(iconView(context, icon, iconTint ?: KsuTokens.accent(context)))
        addView(
            textColumn(context, title, summary), LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = if (icon != null) context.dp(14) else 0 }
        )
        if (onClick != null) addView(chevron(context))
        if (onClick != null) {
            isClickable = true
            isFocusable = true
            background = rippled(
                roundedBackground(Color.TRANSPARENT, KsuTokens.rowRadius(context)),
                rippleColor(context)
            )
            setOnClickListener { onClick() }
        }
    }

    fun switchItem(
        context: Context,
        title: String,
        summary: String? = null,
        @DrawableRes icon: Int? = null,
        checked: Boolean,
        enabled: Boolean = true,
        onCheckedChange: (Boolean) -> Unit
    ): View = rowContainer(context, enabled).apply {
        if (icon != null) addView(iconView(context, icon, KsuTokens.accent(context)))
        addView(
            textColumn(context, title, summary), LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = if (icon != null) context.dp(14) else 0 }
        )
        val switch = SwitchCompat(context).apply {
            isChecked = checked
            isEnabled = enabled
            val accent = KsuTokens.accent(context)
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            trackTintList = android.content.res.ColorStateList.valueOf(
                if (checked) accent else KsuTokens.divider(context).let {
                    Color.argb(0x66, Color.red(it), Color.green(it), Color.blue(it))
                }
            )
            setOnCheckedChangeListener { _, value ->
                trackTintList = android.content.res.ColorStateList.valueOf(
                    if (value) accent else Color.argb(0x66, 0, 0, 0)
                )
                onCheckedChange(value)
            }
        }
        addView(switch, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = context.dp(12) })
    }

    /**
     * Row with a slider. [onValueChange] fires continuously so previews track the finger.
     */
    fun sliderItem(
        context: Context,
        title: String,
        value: Int,
        min: Int,
        max: Int,
        valueLabel: (Int) -> String = { it.toString() },
        onValueChange: (Int) -> Unit,
        onValueSettled: ((Int) -> Unit)? = null
    ): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setPadding(
            KsuTokens.pageInset(context), context.dp(12),
            KsuTokens.pageInset(context), context.dp(8)
        )
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleView = TextView(context).apply {
            text = title
            setTextColor(KsuTokens.onSurface(context))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
        val valueView = TextView(context).apply {
            text = valueLabel(value)
            setTextColor(KsuTokens.accent(context))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.END
        }
        header.addView(
            titleView, LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        )
        header.addView(valueView)
        addView(header)

        val seek = SeekBar(context).apply {
            this.max = (max - min).coerceAtLeast(1)
            progress = (value - min).coerceIn(0, this.max)
            progressTintList = android.content.res.ColorStateList.valueOf(KsuTokens.accent(context))
            thumbTintList = android.content.res.ColorStateList.valueOf(KsuTokens.accent(context))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val real = min + progress
                    valueView.text = valueLabel(real)
                    onValueChange(real)
                }

                override fun onStartTrackingTouch(bar: SeekBar?) = Unit

                override fun onStopTrackingTouch(bar: SeekBar?) {
                    val real = min + (bar?.progress ?: 0)
                    onValueSettled?.invoke(real)
                }
            })
        }
        addView(
            seek, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = context.dp(2) }
        )
    }

    /**
     * Horizontal single-choice row, used for card style / function-bar style.
     */
    fun choiceRow(
        context: Context,
        title: String,
        labels: List<String>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit
    ): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setPadding(
            KsuTokens.pageInset(context), context.dp(12),
            KsuTokens.pageInset(context), context.dp(12)
        )
        addView(TextView(context).apply {
            text = title
            setTextColor(KsuTokens.onSurface(context))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        })
        val group = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = roundedBackground(
                KsuTokens.divider(context).let { Color.argb(0x22, Color.red(it), Color.green(it), Color.blue(it)) },
                KsuTokens.rowRadius(context)
            )
            setPadding(context.dp(3), context.dp(3), context.dp(3), context.dp(3))
        }
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val chip = TextView(context).apply {
                text = label
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(if (selected) Color.WHITE else KsuTokens.onSurface(context))
                background = roundedBackground(
                    if (selected) KsuTokens.accent(context) else Color.TRANSPARENT,
                    KsuTokens.rowRadius(context) - context.dp(2)
                )
                setPadding(context.dp(12), context.dp(8), context.dp(12), context.dp(8))
                isClickable = true
                setOnClickListener { onSelect(index) }
            }
            group.addView(
                chip, LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                ).apply { if (index > 0) marginStart = context.dp(3) }
            )
        }
        addView(
            group, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = context.dp(10) }
        )
    }

    /** Accent-coloured swatches for the theme colour picker. */
    fun colorRow(
        context: Context,
        title: String,
        colors: List<Int>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit
    ): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setPadding(
            KsuTokens.pageInset(context), context.dp(12),
            KsuTokens.pageInset(context), context.dp(12)
        )
        addView(TextView(context).apply {
            text = title
            setTextColor(KsuTokens.onSurface(context))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        })
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        colors.forEachIndexed { index, color ->
            val size = context.dp(34)
            val swatch = View(context).apply {
                background = if (index == selectedIndex) {
                    outlinedBackground(color, size / 2f, KsuTokens.onSurface(context), context.dp(2))
                } else {
                    roundedBackground(color, size / 2f)
                }
                isClickable = true
                setOnClickListener { onSelect(color) }
            }
            row.addView(
                swatch, LinearLayout.LayoutParams(size, size).apply {
                    if (index > 0) marginStart = context.dp(10)
                }
            )
        }
        addView(row, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = context.dp(12) })
    }

    /** Filled accent button, e.g. "恢复默认". */
    fun pillButton(context: Context, text: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            background = rippled(
                roundedBackground(KsuTokens.accent(context), context.dp(KsuTokens.RADIUS_PILL)),
                Color.argb(0x40, 0xFF, 0xFF, 0xFF)
            )
            setPadding(context.dp(20), context.dp(11), context.dp(20), context.dp(11))
            isClickable = true
            setOnClickListener { onClick() }
        }

    /** Outlined button used for secondary actions such as "恢复默认". */
    fun outlineButton(context: Context, text: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextColor(KsuTokens.accent(context))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            background = rippled(
                outlinedBackground(
                    Color.TRANSPARENT, context.dp(KsuTokens.RADIUS_PILL),
                    KsuTokens.accent(context), context.dp(1)
                ),
                rippleColor(context)
            )
            setPadding(context.dp(20), context.dp(10), context.dp(20), context.dp(10))
            isClickable = true
            setOnClickListener { onClick() }
        }

    /**
     * Scrollable page body with the shared background colour and page insets.
     */
    fun page(context: Context, content: LinearLayout.() -> Unit): ScrollView =
        ScrollView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
            setBackgroundColor(KsuTokens.background(context))
            clipToPadding = false
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, context.dp(8), 0, context.dp(96))
                    content()
                },
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

    // ---- internals -------------------------------------------------------------

    private fun rowContainer(context: Context, enabled: Boolean): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            minimumHeight = context.dp(56)
            setPadding(KsuTokens.pageInset(context), context.dp(10), KsuTokens.pageInset(context), context.dp(10))
            alpha = if (enabled) 1f else 0.45f
        }

    private fun iconView(context: Context, @DrawableRes icon: Int, @ColorInt tint: Int): ImageView =
        ImageView(context).apply {
            setImageResource(icon)
            setColorFilter(tint)
            val size = context.dp(24)
            layoutParams = LinearLayout.LayoutParams(size, size)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

    private fun chevron(context: Context): ImageView =
        ImageView(context).apply {
            setImageResource(org.fcitx.fcitx5.android.R.drawable.ic_baseline_keyboard_arrow_right_24)
            setColorFilter(KsuTokens.onSurfaceVariant(context))
            val size = context.dp(20)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = context.dp(8)
            }
        }

    private fun textColumn(context: Context, title: String, summary: String?): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = title
                setTextColor(KsuTokens.onSurface(context))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                ellipsize = TextUtils.TruncateAt.END
                maxLines = 1
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    this, 12, 16, 1, TypedValue.COMPLEX_UNIT_SP
                )
            })
            if (!summary.isNullOrEmpty()) {
                addView(TextView(context).apply {
                    text = summary
                    setTextColor(KsuTokens.onSurfaceVariant(context))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                })
            }
        }

    /** Convenience: a card holding rows, with dividers inserted between them. */
    fun cardWithRows(context: Context, rows: List<View>): LinearLayout = card(context) {
        rows.forEachIndexed { index, row ->
            if (index > 0) addView(divider(context))
            addView(row)
        }
    }

    @Suppress("unused")
    private fun accentDrawable(context: Context): GradientDrawable =
        roundedBackground(KsuTokens.accent(context), context.dp(KsuTokens.RADIUS_PILL))
}
