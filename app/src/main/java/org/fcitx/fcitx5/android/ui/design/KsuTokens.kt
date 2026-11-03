/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.design

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import androidx.annotation.ColorInt
import org.fcitx.fcitx5.android.data.prefs.AppearancePrefs
import org.fcitx.fcitx5.android.utils.isDarkMode
import splitties.dimensions.dp

/**
 * Design tokens for the KernelSU-flavoured settings UI.
 *
 * The palette is intentionally small: one background, one card surface, one accent and two text
 * tones. Everything the settings pages draw goes through here, so the whole app can be re-skinned
 * from the "界面" page without touching individual screens.
 */
object KsuTokens {

    // ---- Spacing ----------------------------------------------------------------

    const val SPACE_XS = 4
    const val SPACE_S = 8
    const val SPACE_M = 12

    /** Horizontal page inset. */
    const val PAGE_INSET = 16

    /** Vertical gap between two cards. */
    const val CARD_GAP = 12

    // ---- Radii ------------------------------------------------------------------

    const val RADIUS_CARD = 22f
    const val RADIUS_ROW = 14f
    const val RADIUS_PILL = 100f

    // ---- Light palette ----------------------------------------------------------

    private const val LIGHT_BACKGROUND = 0xFFF2F2F7.toInt()
    private const val LIGHT_CARD = 0xFFFFFFFF.toInt()
    private const val LIGHT_ON_SURFACE = 0xFF1C1C1E.toInt()
    private const val LIGHT_ON_SURFACE_VARIANT = 0xFF6C6C70.toInt()
    private const val LIGHT_DIVIDER = 0x14000000
    private const val LIGHT_GLASS_TINT = 0xB3FFFFFF.toInt()

    // ---- Dark palette -----------------------------------------------------------

    private const val DARK_BACKGROUND = 0xFF000000.toInt()
    private const val DARK_CARD = 0xFF1C1C1E.toInt()
    private const val DARK_ON_SURFACE = 0xFFFFFFFF.toInt()
    private const val DARK_ON_SURFACE_VARIANT = 0xFF98989F.toInt()
    private const val DARK_DIVIDER = 0x1FFFFFFF
    private const val DARK_GLASS_TINT = 0xB31C1C1E.toInt()

    fun isDark(context: Context): Boolean =
        context.resources.configuration.isDarkMode()

    @ColorInt
    fun background(context: Context): Int =
        if (isDark(context)) DARK_BACKGROUND else LIGHT_BACKGROUND

    @ColorInt
    fun card(context: Context): Int =
        if (isDark(context)) DARK_CARD else LIGHT_CARD

    @ColorInt
    fun onSurface(context: Context): Int =
        if (isDark(context)) DARK_ON_SURFACE else LIGHT_ON_SURFACE

    @ColorInt
    fun onSurfaceVariant(context: Context): Int =
        if (isDark(context)) DARK_ON_SURFACE_VARIANT else LIGHT_ON_SURFACE_VARIANT

    @ColorInt
    fun divider(context: Context): Int =
        if (isDark(context)) DARK_DIVIDER else LIGHT_DIVIDER

    @ColorInt
    fun glassTint(context: Context): Int =
        if (isDark(context)) DARK_GLASS_TINT else LIGHT_GLASS_TINT

    /** User-selected accent colour, with a safe fallback when prefs are unavailable. */
    @ColorInt
    fun accent(context: Context): Int = runCatching {
        AppearancePrefs.ensureInitialized(context).accentColor.getValue()
    }.getOrDefault(AppearancePrefs.DEFAULT_ACCENT_COLOR)

    /** Card corner radius in px, driven by the user's corner-radius preference. */
    fun cardRadius(context: Context): Float {
        val base = runCatching {
            AppearancePrefs.ensureInitialized(context).cornerRadius.getValue()
        }.getOrDefault(AppearancePrefs.DEFAULT_CORNER_RADIUS).toFloat()
        // Scale: the preference describes the look of a card, rows are a bit tighter.
        return context.dp(base)
    }

    fun rowRadius(context: Context): Float =
        (cardRadius(context) * 0.7f).coerceAtLeast(context.dp(6f))

    /** Page inset in px, driven by the user's spacing preference. */
    fun pageInset(context: Context): Int {
        val base = runCatching {
            AppearancePrefs.ensureInitialized(context).spacing.getValue()
        }.getOrDefault(AppearancePrefs.DEFAULT_SPACING)
        return context.dp(base)
    }

    fun cardGap(context: Context): Int = context.dp(CARD_GAP)
}

/** Rounded rectangle background, optionally stroked. */
fun roundedBackground(
    @ColorInt color: Int,
    radiusPx: Float,
    strokeWidthPx: Int = 0,
    @ColorInt strokeColor: Int = 0
): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    cornerRadius = radiusPx
    setColor(color)
    if (strokeWidthPx > 0) setStroke(strokeWidthPx, strokeColor)
}

/**
 * Ripple wrapper. On API 21+ this is a real [RippleDrawable]; the content drawable is kept in the
 * mask layer so the ripple is clipped to the rounded shape.
 */
fun rippled(drawable: Drawable, @ColorInt rippleColor: Int): Drawable =
    RippleDrawable(ColorStateList.valueOf(rippleColor), drawable, null)

/**
 * Layered background: a solid surface plus a hairline outline on top.
 */
fun outlinedBackground(
    @ColorInt fillColor: Int,
    radiusPx: Float,
    @ColorInt strokeColor: Int,
    strokeWidthPx: Int
): Drawable = LayerDrawable(
    arrayOf(
        roundedBackground(fillColor, radiusPx),
        roundedBackground(0x00000000, radiusPx, strokeWidthPx, strokeColor)
    )
)

/** `true` when the platform can render real blurs cheaply enough to be worth it. */
val supportsRealtimeBlur: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Applies elevation only where it actually draws a shadow worth having. */
fun View.applySoftElevation(elevationPx: Float) {
    elevation = elevationPx
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        clipToOutline = true
    }
}
