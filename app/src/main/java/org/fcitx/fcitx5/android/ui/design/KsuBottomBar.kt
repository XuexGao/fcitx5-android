/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.design

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import splitties.dimensions.dp

/**
 * Four-tab bottom navigation.
 *
 * Visuals follow the KernelSU/Miuix floating bar: a rounded, translucent slab that sits above the
 * page and blurs whatever scrolls underneath. When blur is unavailable or disabled it falls back to
 * a translucent rounded surface, so the shape and colours are unchanged.
 */
class KsuBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    data class Item(@DrawableRes val icon: Int, val label: String)

    private val glass = GlassSurfaceView(context)
    private val row = LinearLayout(context)

    private var items: List<Item> = emptyList()
    private val tabViews = mutableListOf<TabView>()

    var onSelect: ((Int) -> Unit)? = null

    private var selectedIndex = 0

    init {
        addView(glass, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /** `true` when the bar should float above the content with rounded corners. */
    var floating: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            applyChrome()
        }

    /** `true` when the bar may blur the content behind it. */
    var glassEnabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            applyChrome()
        }

    fun setItems(value: List<Item>) {
        items = value
        row.removeAllViews()
        tabViews.clear()
        value.forEachIndexed { index, item ->
            val tab = TabView(context, item).apply {
                setOnClickListener {
                    onSelect?.invoke(index)
                }
            }
            tabViews += tab
            row.addView(
                tab, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            )
        }
        applyChrome()
        select(selectedIndex, animate = false)
    }

    fun select(index: Int, animate: Boolean = true) {
        if (items.isEmpty()) return
        selectedIndex = index.coerceIn(0, items.size - 1)
        tabViews.forEachIndexed { i, tab -> tab.setSelectedState(i == selectedIndex, animate) }
    }

    fun currentIndex(): Int = selectedIndex

    /** Re-samples the blurred backdrop; call on layout and when scrolling settles. */
    fun refreshBackdrop(source: View) {
        if (!floating || !glassEnabled) return
        glass.refreshBackdrop(source)
    }

    fun refreshChrome() = applyChrome()

    private fun applyChrome() {
        val radius = if (floating) context.dp(26f) else 0f
        val margin = if (floating) context.dp(12) else 0
        val barHeight = context.dp(if (floating) 64 else 56)
        val params = layoutParams as? ViewGroup.MarginLayoutParams
        if (params != null) {
            params.height = barHeight
            params.marginStart = margin
            params.marginEnd = margin
            layoutParams = params
        } else {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, barHeight
            ).apply {
                marginStart = margin
                marginEnd = margin
            }
        }
        glass.cornerRadiusPx = radius
        glass.tintColor = if (glassEnabled) {
            KsuTokens.glassTint(context)
        } else {
            KsuTokens.card(context)
        }
        glass.outlineColor = if (floating) KsuTokens.divider(context) else Color.TRANSPARENT
        glass.outlineWidthPx = if (floating) context.dp(1).toFloat() else 0f
        glass.invalidateBackdrop()
        if (!floating && !glassEnabled) {
            setBackgroundColor(KsuTokens.card(context))
        } else {
            setBackgroundColor(Color.TRANSPARENT)
        }
        elevation = if (floating) context.dp(8f) else 0f
        row.setPadding(
            context.dp(if (floating) 6 else 0), 0, context.dp(if (floating) 6 else 0), 0
        )
    }

    private class TabView(context: Context, item: Item) : LinearLayout(context) {

        private val icon = ImageView(context)
        private val label = TextView(context)

        init {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            icon.setImageResource(item.icon)
            // Exposed for accessibility and for instrumented tests selecting a tab by name.
            contentDescription = item.label
            val iconSize = context.dp(23)
            addView(icon, LayoutParams(iconSize, iconSize))
            label.text = item.label
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            label.gravity = Gravity.CENTER
            label.maxLines = 1
            addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(3)
            })
        }

        fun setSelectedState(selected: Boolean, animate: Boolean) {
            val accent = KsuTokens.accent(context)
            val inactive = KsuTokens.onSurfaceVariant(context)
            icon.setColorFilter(if (selected) accent else inactive)
            label.setTextColor(if (selected) accent else inactive)
            label.typeface = Typeface.create(
                Typeface.DEFAULT, if (selected) Typeface.BOLD else Typeface.NORMAL
            )
            if (animate) {
                animate().scaleX(if (selected) 1.06f else 1f)
                    .scaleY(if (selected) 1.06f else 1f)
                    .setDuration(140L)
                    .start()
            } else {
                scaleX = if (selected) 1.06f else 1f
                scaleY = if (selected) 1.06f else 1f
            }
        }
    }
}

/** Reads the two bottom-bar related appearance preferences. */
fun Context.bottomBarFloating(): Boolean = runCatching {
    AppearancePrefs.ensureInitialized(this).floatingBottomBar.getValue()
}.getOrDefault(true)

fun Context.bottomBarGlass(): Boolean = runCatching {
    AppearancePrefs.ensureInitialized(this).liquidGlass.getValue()
}.getOrDefault(true)
