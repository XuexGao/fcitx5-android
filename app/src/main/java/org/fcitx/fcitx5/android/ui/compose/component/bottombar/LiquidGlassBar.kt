/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.compose.component.bottombar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.fcitx.fcitx5.android.ui.compose.theme.isInDarkTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * The "liquid glass" bottom bar.
 *
 * The reference implementations (FlClash, and the miuix example's
 * `LiquidGlassNavigationBar`) blur the content *behind* the bar. miuix's own blur needs
 * RenderEffect, which only exists from API 31, so this variant blurs a **software snapshot**
 * instead: the shell hands us the page content as an already-blurred, downscaled [backdrop] bitmap
 * (produced with [org.fcitx.fcitx5.android.utils.BitmapBlurUtil], which uses RenderScript on
 * API 30 and below), and we draw the slice that sits under the bar, clipped to the pill shape,
 * with a tint, a hairline border and a top highlight on top.
 *
 * That keeps the effect identical from API 24 up, which is the whole point of doing it this way.
 */
@Composable
fun LiquidGlassBar(
    backdrop: ImageBitmap?,
    /** How much [backdrop] was downscaled relative to the screen, as `screenPx / bitmapPx`. */
    backdropScale: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    val dark = isInDarkTheme()
    val tint = if (dark) Color.Black.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.30f)
    val border = colorScheme.outline.copy(alpha = 0.35f)
    val highlight = Color.White.copy(alpha = if (dark) 0.10f else 0.30f)

    var windowBounds by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(shape)
            .onGloballyPositioned { windowBounds = it.boundsInWindow() },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawGlassSlice(backdrop, backdropScale, windowBounds, tint)
            // Hairline border and a soft highlight along the top edge: the "glass" read.
            drawRoundRect(
                color = border,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
            drawRoundRect(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    0f to highlight,
                    0.35f to Color.Transparent,
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx()),
            )
        }
        content()
    }
}

/**
 * Draws the part of [backdrop] that lies behind the bar into the bar's own coordinate space.
 *
 * [backdrop] covers the whole window at `1 / scale` resolution, so the source rectangle is the bar's
 * window bounds divided by the scale. Falls back to a flat translucent fill when there is no
 * snapshot yet (first frame, or the shell could not capture one).
 */
private fun DrawScope.drawGlassSlice(
    backdrop: ImageBitmap?,
    scale: Float,
    windowBounds: Rect,
    fallback: Color,
) {
    if (backdrop == null || scale <= 0f || windowBounds.isEmpty) {
        drawRect(color = fallback)
        return
    }
    val srcLeft = (windowBounds.left / scale).toInt().coerceIn(0, backdrop.width)
    val srcTop = (windowBounds.top / scale).toInt().coerceIn(0, backdrop.height)
    val srcWidth = (windowBounds.width / scale).toInt().coerceIn(1, backdrop.width - srcLeft)
    val srcHeight = (windowBounds.height / scale).toInt().coerceIn(1, backdrop.height - srcTop)
    drawImage(
        image = backdrop,
        srcOffset = IntOffset(srcLeft, srcTop),
        srcSize = IntSize(srcWidth, srcHeight),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
    )
    // A tint keeps text legible no matter what is behind the glass.
    drawRect(color = fallback)
}

/** Convenience for the shell: the blurred snapshot plus the scale it was taken at. */
data class GlassBackdrop(
    val bitmap: ImageBitmap,
    val scale: Float,
)
