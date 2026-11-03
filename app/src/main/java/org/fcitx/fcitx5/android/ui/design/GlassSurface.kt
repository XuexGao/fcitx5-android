/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.view.View
import org.fcitx.fcitx5.android.utils.BitmapBlurUtil
import timber.log.Timber

/**
 * Captures the pixels behind a glass surface so they can be blurred.
 *
 * The capture is deliberately cheap: it renders only the strip of [source] that sits underneath the
 * surface, and it downsamples before blurring. On devices where this would be too expensive the
 * caller simply never asks for a snapshot and the surface stays translucent (see
 * [GlassSurfaceView.degraded]).
 */
object GlassBackdrop {

    /** Downsample factor used before blurring. */
    private const val DOWNSAMPLE = 4

    /**
     * @param source view to sample, usually the page container
     * @param left left edge of the glass surface, in [source] coordinates
     * @param top top edge of the glass surface, in [source] coordinates
     * @param width surface width
     * @param height surface height
     */
    fun capture(source: View, left: Int, top: Int, width: Int, height: Int): Bitmap? {
        if (width <= 0 || height <= 0) return null
        if (source.width <= 0 || source.height <= 0) return null
        val sw = (width / DOWNSAMPLE).coerceAtLeast(1)
        val sh = (height / DOWNSAMPLE).coerceAtLeast(1)
        return try {
            val bitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(1f / DOWNSAMPLE, 1f / DOWNSAMPLE)
            canvas.translate(-left.toFloat(), -top.toFloat())
            source.draw(canvas)
            bitmap
        } catch (e: Exception) {
            Timber.w(e, "Glass backdrop capture failed")
            null
        }
    }

    fun blur(source: Bitmap, radius: Float): Bitmap = try {
        BitmapBlurUtil.blur(source, radius)
    } catch (e: Exception) {
        Timber.w(e, "Glass backdrop blur failed")
        source
    }
}

/**
 * A rounded, translucent surface that can optionally show a blurred version of whatever is behind
 * it — the "liquid glass" look.
 *
 * Degradation ladder, applied automatically:
 *  1. API 31+ with glass enabled: blurred backdrop + tint + highlight stroke.
 *  2. API 26–30 with glass enabled: same, using the software/RenderScript blur helper.
 *  3. API 23–25, or glass disabled, or the snapshot failed: translucent tint + outline only.
 *
 * The layout, corner radius and colours are identical in every step, so the overall style never
 * changes — only the amount of blur does.
 */
class GlassSurfaceView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val shapePath = Path()
    private val bounds = RectF()

    private var backdrop: Bitmap? = null
    private var backdropDirty = true

    var cornerRadiusPx: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                backdropDirty = true
                invalidate()
            }
        }

    /** Tint drawn over the blurred backdrop. */
    var tintColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            invalidate()
        }

    var outlineColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            invalidate()
        }

    var outlineWidthPx: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    /** `true` when the platform cannot blur, so only the tint is drawn. */
    val degraded: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.O

    private var lastLeft = 0
    private var lastTop = 0
    private val tmpLocation = IntArray(2)

    /**
     * Re-samples the backdrop from [source]. Safe to call often; it is a no-op while the geometry
     * is unchanged.
     */
    fun refreshBackdrop(source: View) {
        if (degraded || width <= 0 || height <= 0) {
            backdrop = null
            invalidate()
            return
        }
        getLocationInWindow(tmpLocation)
        val left = tmpLocation[0]
        val top = tmpLocation[1]
        source.getLocationInWindow(tmpLocation)
        val sourceLeft = tmpLocation[0]
        val sourceTop = tmpLocation[1]
        val relLeft = left - sourceLeft
        val relTop = top - sourceTop
        if (relLeft == lastLeft && relTop == lastTop && !backdropDirty) return
        lastLeft = relLeft
        lastTop = relTop
        backdropDirty = false
        val snapshot = GlassBackdrop.capture(source, relLeft, relTop, width, height) ?: run {
            backdrop = null
            invalidate()
            return
        }
        backdrop = GlassBackdrop.blur(snapshot, blurRadius)
        invalidate()
    }

    /** Marks the cached backdrop stale so the next [refreshBackdrop] re-samples. */
    fun invalidateBackdrop() {
        backdropDirty = true
    }

    /** Blur strength; overridable by the caller for very small surfaces. */
    var blurRadius: Float = 18f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        backdropDirty = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())
        val radius = cornerRadiusPx.coerceAtMost(height / 2f)

        shapePath.reset()
        shapePath.addRoundRect(bounds, radius, radius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(shapePath)

        val snapshot = backdrop
        if (snapshot != null && !snapshot.isRecycled) {
            paint.reset()
            paint.isAntiAlias = true
            paint.isFilterBitmap = true
            val dest = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawBitmap(snapshot, null, dest, paint)
        }

        paint.reset()
        paint.isAntiAlias = true
        paint.color = tintColor
        canvas.drawRect(bounds, paint)
        canvas.restore()

        if (outlineWidthPx > 0f && outlineColor != Color.TRANSPARENT) {
            outlinePaint.color = outlineColor
            outlinePaint.strokeWidth = outlineWidthPx
            canvas.drawPath(shapePath, outlinePaint)
        }
    }
}
